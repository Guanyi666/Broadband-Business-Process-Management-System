package com.bbpms.attendance.controller;

import com.bbpms.attendance.dto.BreakReq;
import com.bbpms.attendance.dto.ClockInReq;
import com.bbpms.attendance.dto.ClockOutReq;
import com.bbpms.attendance.service.AttendanceService;
import com.bbpms.attendance.vo.AttendanceSummaryVO;
import com.bbpms.attendance.vo.AttendanceVO;
import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "attendance", description = "考勤管理")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "签到")
    @PostMapping("/clock-in")
    @PreAuthorize("hasAuthority('attendance:clock')")
    public R<AttendanceVO> clockIn(@Valid @RequestBody ClockInReq req) {
        return R.ok(attendanceService.clockIn(SecurityUtils.getCurrentUserId(), req));
    }

    @Operation(summary = "签出")
    @PostMapping("/clock-out")
    @PreAuthorize("hasAuthority('attendance:clock')")
    public R<AttendanceVO> clockOut(@Valid @RequestBody ClockOutReq req) {
        return R.ok(attendanceService.clockOut(SecurityUtils.getCurrentUserId(), req));
    }

    @Operation(summary = "开始休息")
    @PostMapping("/break/start")
    @PreAuthorize("hasAuthority('attendance:clock')")
    public R<AttendanceVO> breakStart(@RequestBody(required = false) BreakReq req) {
        return R.ok(attendanceService.breakStart(SecurityUtils.getCurrentUserId(), req));
    }

    @Operation(summary = "结束休息")
    @PostMapping("/break/end")
    @PreAuthorize("hasAuthority('attendance:clock')")
    public R<AttendanceVO> breakEnd(@RequestBody(required = false) BreakReq req) {
        return R.ok(attendanceService.breakEnd(SecurityUtils.getCurrentUserId(), req));
    }

    @Operation(summary = "我今日的考勤")
    @GetMapping("/today")
    @PreAuthorize("isAuthenticated()")
    public R<AttendanceVO> today() {
        return R.ok(attendanceService.today(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "我的历史（最近 N 天）")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('attendance:view-self')")
    public R<List<AttendanceVO>> my(@RequestParam(value = "limit", defaultValue = "30") int limit) {
        return R.ok(attendanceService.daily(SecurityUtils.getCurrentUserId(), limit));
    }

    @Operation(summary = "指定人员的最近 N 天考勤")
    @GetMapping("/daily/{installerId}")
    @PreAuthorize("hasAuthority('attendance:view-all')")
    public R<List<AttendanceVO>> dailyOf(@PathVariable Long installerId,
                                         @RequestParam(value = "limit", defaultValue = "30") int limit) {
        return R.ok(attendanceService.daily(installerId, limit));
    }

    @Operation(summary = "我的月度汇总")
    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('attendance:view-self')")
    public R<AttendanceSummaryVO> monthly(@RequestParam(value = "month", required = false) String month) {
        return R.ok(attendanceService.monthly(SecurityUtils.getCurrentUserId(), month));
    }

    @Operation(summary = "指定人员的月度汇总")
    @GetMapping("/monthly/{installerId}")
    @PreAuthorize("hasAuthority('attendance:view-all')")
    public R<AttendanceSummaryVO> monthlyOf(@PathVariable Long installerId,
                                            @RequestParam(value = "month", required = false) String month) {
        return R.ok(attendanceService.monthly(installerId, month));
    }

    @Operation(summary = "当前在岗列表")
    @GetMapping("/on-duty")
    @PreAuthorize("hasAuthority('attendance:view-all')")
    public R<List<AttendanceVO>> onDutyNow() {
        return R.ok(attendanceService.onDutyNow());
    }
}
