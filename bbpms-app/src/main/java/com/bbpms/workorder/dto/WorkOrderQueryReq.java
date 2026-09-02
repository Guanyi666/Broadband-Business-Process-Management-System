package com.bbpms.workorder.dto;

import com.bbpms.common.entity.BaseDTO;
import com.bbpms.common.enums.WorkOrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * Pagination + filter for {@code GET /api/work-orders/page}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Work order pagination query")
public class WorkOrderQueryReq extends BaseDTO {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Filter by source order id")
    private Long orderId;

    @Schema(description = "Filter by installer id")
    private Long installerId;

    @Schema(description = "Filter by status")
    private WorkOrderStatus status;

    @Schema(description = "Exact match on work_no")
    private String workNo;

    @Schema(description = "Create-time lower bound")
    private LocalDateTime startTime;

    @Schema(description = "Create-time upper bound")
    private LocalDateTime endTime;
}
