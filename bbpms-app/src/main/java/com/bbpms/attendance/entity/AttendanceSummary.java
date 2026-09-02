package com.bbpms.attendance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bbpms.common.entity.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Monthly rollup of attendance per installer. Maps to {@code att_attendance_summary}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("att_attendance_summary")
public class AttendanceSummary extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long installerId;
    /**
     * "YYYY-MM". NB: {@code YEAR_MONTH} is a reserved word in MySQL 8 —
     * every reference (MyBatis-Plus generated SQL and hand-written mapper
     * SQL) must backtick the column, otherwise MySQL throws 1064 (SA-P2-001).
     */
    @TableField("`year_month`")
    private String yearMonth;
    private Integer totalWorkMinutes;
    private Integer workDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer absentCount;
    private LocalDateTime lastCalculatedAt;
}
