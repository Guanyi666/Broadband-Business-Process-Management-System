package com.bbpms.workorder.vo;

import com.bbpms.common.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Row shape for a timeline entry returned by both detail and the dedicated
 * timeline endpoint.
 */
@Data
@Schema(description = "Work order timeline entry")
public class WorkOrderTimelineVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long workOrderId;
    private WorkOrderStatus fromStatus;
    private String fromStatusDesc;
    private WorkOrderStatus toStatus;
    private String toStatusDesc;
    private Long operatorId;
    private String operatorName;
    private String operatorRole;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
