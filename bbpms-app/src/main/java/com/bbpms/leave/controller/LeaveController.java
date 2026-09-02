package com.bbpms.leave.controller;

import com.bbpms.common.result.R;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.leave.dto.LeaveApplyReq;
import com.bbpms.leave.dto.LeaveApprovalReq;
import com.bbpms.leave.service.LeaveService;
import com.bbpms.leave.vo.LeaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "leave", description = "请假管理")
@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @Operation(summary = "申请请假")
    @PostMapping("/apply")
    @PreAuthorize("hasAuthority('leave:apply')")
    public R<LeaveVO> apply(@Valid @RequestBody LeaveApplyReq req) {
        return R.ok(leaveService.apply(req, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "撤销我的请假")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('leave:cancel')")
    public R<LeaveVO> cancel(@PathVariable Long id) {
        return R.ok(leaveService.cancel(id, SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "审批请假")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('leave:approve-l1','leave:approve-l2','leave:approve')")
    public R<LeaveVO> approve(@PathVariable Long id, @Valid @RequestBody LeaveApprovalReq req) {
        return R.ok(leaveService.approve(id, SecurityUtils.getCurrentUserId(), req));
    }

    @Operation(summary = "我的请假记录")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('leave:view-self')")
    public R<List<LeaveVO>> my(@RequestParam(value = "status", required = false) String status) {
        return R.ok(leaveService.myApplications(SecurityUtils.getCurrentUserId(), status));
    }

    @Operation(summary = "待我审批的请假")
    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAnyAuthority('leave:approve-l1','leave:approve-l2','leave:approve')")
    public R<List<LeaveVO>> pendingApprovals(
            @RequestParam(value = "level", required = false) Integer level) {
        return R.ok(leaveService.pendingApprovals(SecurityUtils.getCurrentUserId(), level));
    }

    @Operation(summary = "团队请假日历")
    @GetMapping("/team-calendar")
    @PreAuthorize("hasAuthority('leave:view-all')")
    public R<List<LeaveVO>> teamCalendar(
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return R.ok(leaveService.teamCalendar(from, to));
    }

    @Operation(summary = "请假详情")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<LeaveVO> get(@PathVariable Long id) {
        return R.ok(leaveService.get(id));
    }
}
