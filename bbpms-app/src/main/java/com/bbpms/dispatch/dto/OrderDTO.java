package com.bbpms.dispatch.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Internal projection of an order used during dispatch scoring.
 * Not exposed as a request body — built from the order aggregate.
 */
public record OrderDTO(
        Long id,
        String orderNo,
        BigDecimal lat,
        BigDecimal lng,
        String gridCode,
        String packageCode,
        List<String> requiredSkills
) {}