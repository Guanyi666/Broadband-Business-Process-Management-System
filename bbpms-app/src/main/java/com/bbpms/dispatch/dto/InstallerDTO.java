package com.bbpms.dispatch.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight installer projection used during dispatch scoring.
 * Sourced from installer_profile + an active-location snapshot.
 */
public record InstallerDTO(
        Long id,
        String name,
        BigDecimal lat,
        BigDecimal lng,
        Integer workload,
        Integer maxWorkload,
        Double rating,
        Integer level,
        List<String> skills,
        List<String> serviceAreas,
        Integer onDuty,
        String username,
        String phone
) {}
