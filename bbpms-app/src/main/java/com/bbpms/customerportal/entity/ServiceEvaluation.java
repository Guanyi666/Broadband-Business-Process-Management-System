package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_evaluation")
public class ServiceEvaluation extends BaseDO {
    private Long orderId;
    private Long workOrderId;
    private Long customerId;
    private Long installerId;
    private Integer overallScore;
    private Integer serviceScore;
    private Integer qualityScore;
    private Integer punctualityScore;
    private String tags;
    private String content;
    private Integer status;
}
