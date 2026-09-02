package com.bbpms.leave.service;

import com.bbpms.leave.dto.LeaveApplyReq;
import com.bbpms.leave.dto.LeaveApprovalReq;
import com.bbpms.leave.entity.LeaveRequest;
import com.bbpms.leave.vo.LeaveVO;

import java.time.LocalDateTime;
import java.util.List;

public interface LeaveService {

    /** Submit a new leave request. Sets {@code requiredLevel} per the escalation rule. */
    LeaveVO apply(LeaveApplyReq req, Long applicantId);

    /** Approve or reject at the given level. Idempotent per level. */
    LeaveVO approve(Long leaveId, Long approverId, LeaveApprovalReq req);

    /** Cancel a PENDING leave. */
    LeaveVO cancel(Long leaveId, Long applicantId);

    /** Applicant's own list. */
    List<LeaveVO> myApplications(Long applicantId, String status);

    /** Pending applications for the current approver at their level. */
    List<LeaveVO> pendingApprovals(Long approverId, Integer approverLevel);

    /** Team calendar of approved leaves in a date range. */
    List<LeaveVO> teamCalendar(LocalDateTime from, LocalDateTime to);

    /** Whether the installer currently has an APPROVED leave covering {@code now}. */
    boolean hasActiveLeave(Long installerId, LocalDateTime at);

    /** All currently active approved leaves. Used by the dispatch module to filter candidates. */
    List<LeaveRequest> findAllActiveLeaves(LocalDateTime at);

    /** Get by id. */
    LeaveVO get(Long leaveId);
}
