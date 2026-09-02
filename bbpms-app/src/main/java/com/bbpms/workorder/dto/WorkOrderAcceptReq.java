package com.bbpms.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request body for {@code POST /api/work-orders/{id}/accept}.
 */
@Data
@Schema(description = "Accept work order request")
public class WorkOrderAcceptReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "workOrderId 不能为空")
    private Long workOrderId;
}
