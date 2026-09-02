package com.bbpms.dispatch.vo;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DispatchStatVO {
    private LocalDate date;
    private Long total;
    private Long autoCount;
    private Long manualCount;
    private Long reassignCount;
    private Double avgScore;
}