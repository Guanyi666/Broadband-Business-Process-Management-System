package com.bbpms.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request body for {@code POST /api/work-orders}.
 *
 * <p>Issued by the dispatch team (manual) or by the
 * {@code OrderEventListener} (auto). If {@code installerId} or
 * {@code dispatcherId} is null the service falls back to mock scoring to
 * pick them.</p>
 */
@Data
@Schema(description = "Create work order request")
public class WorkOrderCreateReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "orderId 不能为空")
    @Schema(description = "Source order id", example = "1001")
    private Long orderId;

    @Schema(description = "Preselected installer id (optional — service falls back to mock scoring)")
    private Long installerId;

    @Schema(description = "Dispatcher user id (optional — defaults to current user)")
    private Long dispatcherId;

    @Schema(description = "Free remark for the audit timeline")
    private String remark;
}
