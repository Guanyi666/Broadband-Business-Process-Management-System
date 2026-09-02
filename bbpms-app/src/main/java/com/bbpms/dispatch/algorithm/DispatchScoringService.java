package com.bbpms.dispatch.algorithm;

import com.bbpms.dispatch.dto.CandidateDTO;
import com.bbpms.dispatch.dto.InstallerDTO;
import com.bbpms.dispatch.dto.OrderDTO;
import com.bbpms.dispatch.entity.DispatchRule;
import com.bbpms.dispatch.config.DispatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Core dispatch scoring engine.
 *
 * <p>Composite score (0-100) is a weighted sum of 4 normalised factors:</p>
 * <ul>
 *     <li>distance (default 40) — Haversine great-circle distance vs. configured radius.</li>
 *     <li>load     (default 25) — 1 - workload / maxWorkload, clamped.</li>
 *     <li>skill    (default 20) — 1.0 if all required skills present, else 0.5.</li>
 *     <li>rating   (default 15) — installer rating / 5.0, clamped.</li>
 * </ul>
 *
 * <p>Ordering is deterministic: totalScore desc, then workload asc, rating desc, id asc.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchScoringService {

    /** Mean Earth radius in km (IUGG). */
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final DispatchProperties props;

    /**
     * Score and rank the candidate set for the given order. Candidates
     * whose distance exceeds the rule's radius are filtered out. The returned
     * list is sorted highest-score first.
     */
    public List<CandidateDTO> score(OrderDTO order,
                                    List<InstallerDTO> candidates,
                                    DispatchRule rule) {
        if (order == null || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // Resolve effective weights from rule, falling back to properties defaults.
        int wDist  = rule != null && rule.getWeightDistance() != null ? rule.getWeightDistance() : props.getWeightsDistance();
        int wLoad  = rule != null && rule.getWeightLoad()     != null ? rule.getWeightLoad()     : props.getWeightsLoad();
        int wSkill = rule != null && rule.getWeightSkill()    != null ? rule.getWeightSkill()    : props.getWeightsSkill();
        int wRate  = rule != null && rule.getWeightRating()   != null ? rule.getWeightRating()   : props.getWeightsRating();
        int radius = rule != null && rule.getRadiusKm()       != null ? rule.getRadiusKm()       : props.getRadiusKm();
        int maxLoad = props.getMaxLoad();

        List<String> requiredSkills = order.requiredSkills() == null ? List.of() : order.requiredSkills();

        List<CandidateDTO> ranked = new ArrayList<>();
        for (InstallerDTO inst : candidates) {
            double distanceKm = haversine(order.lat(), order.lng(), inst.lat(), inst.lng());

            // Out-of-range: skip. Null installer location is treated as same-point.
            if (inst.lat() != null && inst.lng() != null && distanceKm > radius) {
                continue;
            }

            double sd = distanceScore(distanceKm, radius);
            double sl = loadScore(inst, maxLoad);
            double ss = skillScore(inst, requiredSkills);
            double sr = ratingScore(inst);

            double total = wDist * sd + wLoad * sl + wSkill * ss + wRate * sr;

            Map<String, Double> breakdown = new LinkedHashMap<>();
            breakdown.put("distance", round2(distanceKm));
            breakdown.put("sd", round4(sd));
            breakdown.put("sl", round4(sl));
            breakdown.put("ss", round4(ss));
            breakdown.put("sr", round4(sr));

            ranked.add(new CandidateDTO(
                    inst.id(),
                    inst.name(),
                    distanceKm,
                    inst.workload(),
                    ss,
                    inst.rating(),
                    round2(total),
                    breakdown
            ));
        }

        ranked.sort(Comparator
                .comparingDouble(CandidateDTO::totalScore).reversed()
                .thenComparing(CandidateDTO::workload, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CandidateDTO::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CandidateDTO::installerId, Comparator.nullsLast(Comparator.naturalOrder())));

        return ranked;
    }

    /** Returns the highest-scoring candidate or null when the list is empty. */
    public InstallerDTO chooseBest(OrderDTO order,
                                   List<InstallerDTO> candidates,
                                   DispatchRule rule) {
        List<CandidateDTO> ranked = score(order, candidates, rule);
        if (ranked.isEmpty()) return null;
        Long winnerId = ranked.get(0).installerId();
        return candidates.stream()
                .filter(i -> i.id() != null && i.id().equals(winnerId))
                .findFirst()
                .orElse(null);
    }

    /* -------------------- factor normalisations -------------------- */

    private static double distanceScore(double distanceKm, int radiusKm) {
        if (radiusKm <= 0) return 0.0;
        double s = 1.0 - distanceKm / radiusKm;
        return clamp01(s);
    }

    private static double loadScore(InstallerDTO inst, int maxLoad) {
        if (inst == null || inst.workload() == null) return 0.0;
        int cap = inst.maxWorkload() != null && inst.maxWorkload() > 0 ? inst.maxWorkload() : maxLoad;
        if (cap <= 0) return 0.0;
        double s = 1.0 - (double) inst.workload() / cap;
        return clamp01(s);
    }

    private static double skillScore(InstallerDTO inst, List<String> required) {
        if (required == null || required.isEmpty()) return 1.0;
        if (inst == null || inst.skills() == null || inst.skills().isEmpty()) return 0.0;
        boolean all = required.stream().allMatch(s -> inst.skills().contains(s));
        return all ? 1.0 : 0.5;
    }

    private static double ratingScore(InstallerDTO inst) {
        if (inst == null || inst.rating() == null) return 0.0;
        double s = inst.rating() / 5.0;
        return clamp01(s);
    }

    /* -------------------- haversine -------------------- */

    /**
     * Great-circle distance in kilometres between two (lat, lng) pairs using
     * the haversine formula. Null inputs are treated as zero distance
     * (same-point heuristic for missing location data).
     */
    public static double haversine(BigDecimal lat1, BigDecimal lng1,
                                   BigDecimal lat2, BigDecimal lng2) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return 0.0;
        double phi1 = Math.toRadians(lat1.doubleValue());
        double phi2 = Math.toRadians(lat2.doubleValue());
        double dPhi = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLam = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLam / 2) * Math.sin(dLam / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /* -------------------- utils -------------------- */

    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
}