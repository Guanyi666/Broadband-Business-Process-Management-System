package com.bbpms.attendance.dto;

import lombok.Data;

/**
 * Row returned by {@code AttendanceRecordMapper.aggregateMonth} — the
 * per-month rollup used to (re)build {@code att_attendance_summary} (SA-P2-001).
 */
@Data
public class AttendanceMonthlyStat {

    private Integer totalWorkMinutes;
    private Integer workDays;
    private Integer lateCount;
    private Integer earlyLeaveCount;
}
