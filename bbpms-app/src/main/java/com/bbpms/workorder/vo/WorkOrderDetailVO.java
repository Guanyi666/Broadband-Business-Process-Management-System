package com.bbpms.workorder.vo;

import com.bbpms.common.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detail payload returned by {@code GET /api/work-orders/{id}}. Includes
 * the joined order snapshot (pulled by direct call into the order
 * service) and the full timeline of state transitions.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Work order detail (with timeline + order snapshot)")
public class WorkOrderDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String workNo;
    private Long orderId;
    private String orderNo;
    private Long installerId;
    private String installerName;
    private String installerPhone;
    private Long dispatcherId;
    private String dispatcherName;
    private WorkOrderStatus status;
    private String statusDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dispatchTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime acceptTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;

    private String installAddress;
    private String customerPhone;
    private String customerName;
    private String packageName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /** Joined order snapshot (best-effort; null if cross-module read is suppressed). */
    private Object orderInfo;

    /** Append-only transition log, oldest first. */
    private List<WorkOrderTimelineVO> timeline;
}
