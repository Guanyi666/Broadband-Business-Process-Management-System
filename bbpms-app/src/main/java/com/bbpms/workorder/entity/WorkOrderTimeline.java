package com.bbpms.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import com.bbpms.common.enums.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Append-only audit row for every {@link WorkOrder} state transition.
 * Drives the timeline UI on the detail screen.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_timeline")
@Schema(description = "Work order timeline (state transition audit log)")
public class WorkOrderTimeline extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long workOrderId;

    /** Status before the transition (null for the initial DISPATCHED row). */
    private String fromStatus;

    /** Status after the transition. */
    private String toStatus;

    private Long operatorId;
    private String operatorRole;
    private String remark;

    /** Convenience: parse {@link #fromStatus} as the enum. */
    public WorkOrderStatus getFromStatusEnum() {
        return WorkOrderStatus.fromCode(this.fromStatus);
    }

    /** Convenience: parse {@link #toStatus} as the enum. */
    public WorkOrderStatus getToStatusEnum() {
        return WorkOrderStatus.fromCode(this.toStatus);
    }

    /** Convenience: write {@link #fromStatus} as the enum code. */
    public void setFromStatusEnum(WorkOrderStatus s) {
        this.fromStatus = s == null ? null : s.getCode();
    }

    /** Convenience: write {@link #toStatus} as the enum code. */
    public void setToStatusEnum(WorkOrderStatus s) {
        this.toStatus = s == null ? null : s.getCode();
    }
}