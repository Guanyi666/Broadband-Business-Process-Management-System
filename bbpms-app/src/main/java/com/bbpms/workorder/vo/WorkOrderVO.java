package com.bbpms.workorder.vo;

import com.bbpms.common.enums.WorkOrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lightweight projection of {@link com.bbpms.workorder.entity.WorkOrder}
 * used by list / page endpoints.
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Work order view")
public class WorkOrderVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String workNo;
    private Long orderId;
    private Long installerId;
    private String installerName;
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
    private String packageName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
