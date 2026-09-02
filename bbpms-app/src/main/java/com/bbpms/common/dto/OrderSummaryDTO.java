package com.bbpms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Lightweight cross-module snapshot of an order.
 *
 * <p>The {@code workorder} module reads this snapshot at creation time to
 * denormalise address, contact phone and package onto the work-order row
 * — keeping the work-order aggregate usable even if the parent order
 * later mutates.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long customerId;
    private String installAddress;
    private String gridCode;
    private String packageCode;
    private String packageName;
    private String status;
    private BigDecimal lat;
    private BigDecimal lng;

    /** Contact phone (denormalised from latest appointment). */
    private String contactPhone;

    /** Skill tags resolved by the dispatch service from the package code. */
    private List<String> requiredSkills;
}