package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_service_ticket")
public class CustomerServiceTicket extends BaseDO {
    private String ticketNo;
    private Long customerId;
    private Long orderId;
    private Long workOrderId;
    private String type;
    private String category;
    private Integer priority;
    private String description;
    private String attachments;
    private String status;
    private Long handlerId;
    private String handleResult;
    private LocalDateTime acceptedTime;
    private LocalDateTime resolvedTime;
    private LocalDateTime closedTime;
}
