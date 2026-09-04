package com.bbpms.customerportal.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("appointment_change_log")
public class AppointmentChangeLog extends BaseDO {
    private Long appointmentId;
    private Long orderId;
    private LocalDateTime oldAppointmentTime;
    private LocalDateTime newAppointmentTime;
    private Long operatorId;
    private String operatorRole;
    private String reason;
}
