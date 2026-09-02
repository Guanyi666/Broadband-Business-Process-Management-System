package com.bbpms.dispatch.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dispatch_record")
public class DispatchRecord extends BaseDO {
    private static final long serialVersionUID = 1L;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("installer_id")
    private Long installerId;

    /** Strategy used: AUTO / MANUAL / REASSIGN. */
    @TableField("strategy")
    private String strategy;

    /** Total composite score (out of 100). */
    @TableField("score")
    private BigDecimal score;

    /** Full scored candidate list as JSON for replay / audit. */
    @TableField("candidates_json")
    private String candidatesJson;

    /** Human-readable reason or annotation. */
    @TableField("reason")
    private String reason;
}