package com.bbpms.dispatch.vo;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class DispatchRuleVO {
    private Long id;
    private String name;
    /** Convenience map so the front-end can render sliders generically. */
    private Map<String, Integer> weights = new LinkedHashMap<>();
    private Integer radiusKm;
    private Integer enabled;
    private Integer weightsSum;
}