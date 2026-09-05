package com.bbpms.dispatch.dto;

import java.util.Map;

/**
 * One ranked candidate with the per-factor contribution breakdown.
 * totalScore is on a 0-100 scale (sum of weighted factor scores).
 */
public record CandidateDTO(
        Long installerId,
        String name,
        Double distance,
        Integer workload,
        Double skillMatchScore,
        Double rating,
        Double totalScore,
        Map<String, Double> factorBreakdown,
        String username,
        String phone,
        String status
) {}
