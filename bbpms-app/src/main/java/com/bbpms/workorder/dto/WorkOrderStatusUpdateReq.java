package com.bbpms.workorder.dto;

import com.bbpms.common.enums.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request body for {@code PUT /api/work-orders/{id}/status}. Allows the
 * dispatcher admin to jump a work order to a target state (used by the
 * admin UI rather than the normal flow endpoints).
 */
@Data
@Schema(description = "Generic work order status update (admin override)")
public class WorkOrderStatusUpdateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;

    @NotNull(message = "目标状态不能为空")
    private WorkOrderStatus status;

    @jakarta.validation.constraints.Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
