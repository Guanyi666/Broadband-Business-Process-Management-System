package com.bbpms.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 派单时效分桶项（派单 → 接单耗时，固定 5 桶，含 count=0）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DurationBucketVO {

    /** 桶编码：LT_30M / M30_60 / H1_2 / H2_4 / GT_4H */
    private String bucket;

    /** 桶中文标签（前端图例/坐标轴直接使用） */
    private String label;

    /** 命中该桶的工单数 */
    private Long count;
}
