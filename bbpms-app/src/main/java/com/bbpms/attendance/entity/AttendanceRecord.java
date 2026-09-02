package com.bbpms.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Daily attendance record (one row per installer per day).
 * Maps to table {@code att_attendance_record}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_attendance_record")
public class AttendanceRecord extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long installerId;
    private LocalDate workDate;
    private LocalDateTime clockInAt;
    private LocalDateTime clockOutAt;
    private Integer workMinutes;
    private Integer breakMinutes;
    /** Timestamp the current break started (SA-P2-003); null when not on break. */
    private LocalDateTime breakStartAt;
    /** OFF_DUTY | ON_DUTY | ON_BREAK | AUTO_OFF */
    private String status;
    /** MANUAL | GPS | SCHEDULED | HEARTBEAT_TIMEOUT */
    private String source;
    private BigDecimal locationLat;
    private BigDecimal locationLng;
    private String remark;
}
