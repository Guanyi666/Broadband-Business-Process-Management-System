package com.bbpms.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Append-only log of every state transition for an order. A row is inserted
 * for every transition under the same DB transaction so the audit is
 * guaranteed to reflect exactly what was applied.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_audit_log")
public class OrderAuditLog extends BaseDO {

    private Long orderId;

    /** Auditor / operator user id. */
    private Long auditorId;

    /** Status before transition (null for CREATED). */
    private String fromStatus;

    /** Status after transition. */
    private String toStatus;

    private String remark;
}
