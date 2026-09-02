package com.bbpms.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import com.bbpms.common.enums.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Work-order header. One row per service call-out to an installer.
 *
 * <p>{@code workNo} is the human-readable business number ({@code WO + snowflake})
 * and is the natural unique key. The {@code version} column inherited from
 * {@link BaseDO} drives MyBatis-Plus optimistic locking on every update.</p>
 *
 * <p>{@code status} is persisted as VARCHAR (the {@code code} field of
 * {@link WorkOrderStatus}) — kept as String for backward compatibility and
 * safe cross-module hand-off via {@link com.bbpms.common.dto.OrderSummaryDTO}.</p>
 *
 * <p>SLA-related fields ({@code priority}, {@code expectedFinishTime},
 * {@code lastActiveAt}, {@code stallReason}, {@code cancelType}) are added
 * in Phase 5 for the SLA engine. They are nullable in old rows so no
 * backfill is required.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
@Schema(description = "Work order")
public class WorkOrder extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Human-readable work order number, UK, format "WO{snowflake}". */
    private String workNo;

    /** Source broadband_order.id. */
    private Long orderId;

    /** Assigned installer (sys_user.id). Null until dispatch resolves. */
    private Long installerId;

    /** Dispatcher (sys_user.id) who picked / assigned this order. */
    private Long dispatcherId;

    /** Current state, stored as the {@link WorkOrderStatus} code. */
    private String status;

    private LocalDateTime dispatchTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    /** Install address snapshot (denormalised from the order). */
    private String installAddress;

    /** Customer phone snapshot (denormalised from the order). */
    private String customerPhone;

    /** Package name snapshot (denormalised from the order). */
    private String packageName;

    /* ---------- Phase 5 SLA / lifecycle additions ---------- */

    /** 1=URGENT, 2=HIGH, 3=NORMAL. */
    private Integer priority;

    /** Expected finish time computed at dispatch (start + sla window). */
    private LocalDateTime expectedFinishTime;

    /** Last installer heartbeat; used by the SLA scheduler to detect drops. */
    private LocalDateTime lastActiveAt;

    /** Reason populated when status flips to STALLED. */
    private String stallReason;

    /** CUSTOMER | CS | DISPATCHER | SYSTEM | INSTALLER_SICK | ADDRESS_INVALID | EQUIPMENT | TIMEOUT. */
    private String cancelType;

    /** Convenience: parse {@link #status} as the enum. */
    public WorkOrderStatus getStatusEnum() {
        return WorkOrderStatus.fromCode(this.status);
    }

    /** Convenience: write the {@link WorkOrderStatus} code. */
    public void setStatusEnum(WorkOrderStatus s) {
        this.status = s == null ? null : s.getCode();
    }
}
