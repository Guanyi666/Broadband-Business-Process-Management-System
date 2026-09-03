package com.bbpms.dashboard.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GET /api/dashboard/dispatch-duration 响应体
 */
@Data
@NoArgsConstructor
public class DashboardDispatchVO {

    /** 统计窗口天数 */
    private Integer days;

    /** 派单时效分桶（固定 5 桶，含 count=0；口径 = 接单时间 - 派单时间） */
    private List<DurationBucketVO> durationBuckets;

    public DashboardDispatchVO(Integer days, List<DurationBucketVO> durationBuckets) {
        this.days = days;
        this.durationBuckets = durationBuckets;
    }
}
