package com.bbpms.attendance.service;

import com.bbpms.attendance.dto.BreakReq;
import com.bbpms.attendance.dto.ClockInReq;
import com.bbpms.attendance.dto.ClockOutReq;
import com.bbpms.attendance.vo.AttendanceSummaryVO;
import com.bbpms.attendance.vo.AttendanceVO;

import java.time.LocalDate;
import java.util.List;

/**
 * Attendance module service.
 */
public interface AttendanceService {

    /**
     * Sign in. Idempotent: if a record for today already exists with
     * {@code clockInAt != null}, return it instead of creating a new one.
     */
    AttendanceVO clockIn(Long installerId, ClockInReq req);

    /** Sign out. Computes {@code workMinutes} and flips status. */
    AttendanceVO clockOut(Long installerId, ClockOutReq req);

    /** Start a break (status -> ON_BREAK). */
    AttendanceVO breakStart(Long installerId, BreakReq req);

    /** End a break (status -> ON_DUTY). */
    AttendanceVO breakEnd(Long installerId, BreakReq req);

    /** Today's record for the given installer (may be null if never signed in). */
    AttendanceVO today(Long installerId);

    /** Daily history. Most recent N days. */
    List<AttendanceVO> daily(Long installerId, int limit);

    /** Monthly rollup for the given installer. */
    AttendanceSummaryVO monthly(Long installerId, String yearMonth);

    /** Admin: list installers on duty right now (status=ON_DUTY or ON_BREAK). */
    List<AttendanceVO> onDutyNow();

    /** Whether the installer is currently signed in (ON_DUTY or ON_BREAK). */
    boolean isOnDuty(Long installerId, LocalDate on);
}
