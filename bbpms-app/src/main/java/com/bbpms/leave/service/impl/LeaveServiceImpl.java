package com.bbpms.leave.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bbpms.common.annotation.DistributedLock;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.entity.BaseDO;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.leave.config.LeaveProperties;
import com.bbpms.leave.dto.LeaveApplyReq;
import com.bbpms.leave.dto.LeaveApprovalReq;
import com.bbpms.leave.entity.LeaveApprovalRecord;
import com.bbpms.leave.entity.LeaveRequest;
import com.bbpms.leave.mapper.LeaveApprovalRecordMapper;
import com.bbpms.leave.mapper.LeaveRequestMapper;
import com.bbpms.leave.service.LeaveService;
import com.bbpms.leave.util.LeaveEscalationRule;
import com.bbpms.leave.vo.LeaveVO;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveServiceImpl implements LeaveService {

    private final LeaveRequestMapper requestMapper;
    private final LeaveApprovalRecordMapper approvalMapper;
    private final SysUserMapper userMapper;
    private final LeaveProperties props;
    private final ApplicationEventPublisher publisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "申请请假", module = "请假")
    public LeaveVO apply(LeaveApplyReq req, Long applicantId) {
        if (req == null || applicantId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求不能为空");
        }
        if (req.getStartAt() == null || req.getEndAt() == null || req.getEndAt().isBefore(req.getStartAt())) {
            throw new BizException(ResultCode.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (req.getLeaveType() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请假类型不能为空");
        }
        if (Boolean.TRUE.equals(props.getSickNeedsAttachment())
                && "SICK".equalsIgnoreCase(req.getLeaveType())
                && (req.getAttachmentUrl() == null || req.getAttachmentUrl().isBlank())) {
            throw new BizException(ResultCode.BAD_REQUEST, "病假必须上传附件（医疗证明）");
        }
        if (props.getMinNoticeHours() != null && props.getMinNoticeHours() > 0) {
            long notice = Duration.between(LocalDateTime.now(), req.getStartAt()).toHours();
            if (notice < props.getMinNoticeHours()) {
                throw new BizException(ResultCode.BAD_REQUEST,
                        "请假至少需要提前 " + props.getMinNoticeHours() + " 小时申请");
            }
        }
        double totalHours = Duration.between(req.getStartAt(), req.getEndAt()).toMinutes() / 60.0;
        int requiredLevel = LeaveEscalationRule.requiredLevel(req.getLeaveType(), totalHours, props);

        LeaveRequest lr = new LeaveRequest();
        lr.setApplicantId(applicantId);
        lr.setLeaveType(req.getLeaveType().toUpperCase());
        lr.setStartAt(req.getStartAt());
        lr.setEndAt(req.getEndAt());
        lr.setTotalHours(BigDecimal.valueOf(totalHours).setScale(2, RoundingMode.HALF_UP));
        lr.setReason(req.getReason());
        lr.setAttachmentUrl(req.getAttachmentUrl());
        lr.setStatus("PENDING");
        lr.setCurrentLevel(0);
        lr.setRequiredLevel(requiredLevel);
        lr.setAppliedAt(LocalDateTime.now());
        applyAudit(lr, applicantId);
        requestMapper.insert(lr);

        publisher.publishEvent(new BbpmsEvents.LeaveAppliedEvent(
                lr.getId(), applicantId, lr.getLeaveType(), lr.getStartAt(), lr.getEndAt(), requiredLevel));
        return enrich(lr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'leave:approve:' + #leaveId", waitTime = 5, leaseTime = 30)
    @OperationLog(value = "审批请假", module = "请假")
    public LeaveVO approve(Long leaveId, Long approverId, LeaveApprovalReq req) {
        if (leaveId == null || req == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求不能为空");
        }
        LeaveRequest lr = mustLoad(leaveId);
        if (!"PENDING".equals(lr.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "该请假已结束（" + lr.getStatus() + "）");
        }
        Integer level = req.getApprovalLevel();
        if (level == null || (level != 1 && level != 2)) {
            throw new BizException(ResultCode.BAD_REQUEST, "审批级别必须为 1 或 2");
        }
        if (level > lr.getRequiredLevel()) {
            throw new BizException(ResultCode.BAD_REQUEST, "无需 L2 审批（requiredLevel=" + lr.getRequiredLevel() + "）");
        }
        // SA-P2-002: approvals must progress strictly level by level. Without
        // this, a two-level request (currentLevel=0) could be acted on directly
        // at level 2, bypassing the L1 approver entirely. currentLevel is
        // already verified below (>= level → "已审批过"); the strict-equality
        // check adds the "cannot skip ahead" rule.
        int current = lr.getCurrentLevel() == null ? 0 : lr.getCurrentLevel();
        if (current >= level) {
            throw new BizException(ResultCode.BAD_REQUEST, "L" + level + " 已审批过");
        }
        if (level != current + 1) {
            throw new BizException(ResultCode.BAD_REQUEST,
                    "审批必须按级别顺序进行（当前 L" + current + "，本次 L" + level + "）");
        }

        String action = req.getAction() == null ? "" : req.getAction().toUpperCase();
        if (!"APPROVE".equals(action) && !"REJECT".equals(action)) {
            throw new BizException(ResultCode.BAD_REQUEST, "审批动作必须为 APPROVE 或 REJECT");
        }

        // Record the step
        LeaveApprovalRecord rec = new LeaveApprovalRecord();
        rec.setLeaveId(leaveId);
        rec.setApproverId(approverId);
        rec.setApprovalLevel(level);
        rec.setAction(action.equals("APPROVE") ? "APPROVED" : "REJECTED");
        rec.setComment(req.getComment());
        applyAudit(rec, approverId);
        approvalMapper.insert(rec);

        if ("REJECT".equals(action)) {
            lr.setStatus("REJECTED");
            if (level == 1) {
                lr.setLevel1ApproverId(approverId);
                lr.setLevel1DecidedAt(LocalDateTime.now());
                lr.setLevel1Remark(req.getComment());
            } else {
                lr.setLevel2ApproverId(approverId);
                lr.setLevel2DecidedAt(LocalDateTime.now());
                lr.setLevel2Remark(req.getComment());
            }
            applyAudit(lr, approverId);
            requestMapper.updateById(lr);
            publisher.publishEvent(new BbpmsEvents.LeaveRejectedEvent(leaveId, lr.getApplicantId(),
                    req.getComment() == null ? "rejected" : req.getComment()));
            return enrich(lr);
        }

        // APPROVE
        if (level == 1) {
            lr.setLevel1ApproverId(approverId);
            lr.setLevel1DecidedAt(LocalDateTime.now());
            lr.setLevel1Remark(req.getComment());
            lr.setCurrentLevel(1);
        } else {
            lr.setLevel2ApproverId(approverId);
            lr.setLevel2DecidedAt(LocalDateTime.now());
            lr.setLevel2Remark(req.getComment());
            lr.setCurrentLevel(2);
        }

        // Terminal?
        if (lr.getCurrentLevel() >= lr.getRequiredLevel()) {
            lr.setStatus("APPROVED");
        }
        applyAudit(lr, approverId);
        requestMapper.updateById(lr);

        if ("APPROVED".equals(lr.getStatus())) {
            publisher.publishEvent(new BbpmsEvents.LeaveApprovedEvent(
                    leaveId, lr.getApplicantId(), lr.getLeaveType(), lr.getStartAt(), lr.getEndAt()));
        } else if (lr.getRequiredLevel() == 2 && lr.getCurrentLevel() == 1) {
            // Escalate to L2 — emit a record so audit shows the pass-through
            LeaveApprovalRecord esc = new LeaveApprovalRecord();
            esc.setLeaveId(leaveId);
            esc.setApproverId(approverId);
            esc.setApprovalLevel(1);
            esc.setAction("ESCALATED");
            esc.setComment("L1 approved; awaiting L2");
            applyAudit(esc, approverId);
            approvalMapper.insert(esc);
        }
        return enrich(lr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(key = "'leave:cancel:' + #leaveId", waitTime = 5, leaseTime = 30)
    public LeaveVO cancel(Long leaveId, Long applicantId) {
        LeaveRequest lr = mustLoad(leaveId);
        if (!lr.getApplicantId().equals(applicantId)) {
            throw new BizException(ResultCode.FORBIDDEN, "只能撤销自己的申请");
        }
        if (!"PENDING".equals(lr.getStatus())) {
            throw new BizException(ResultCode.BAD_REQUEST, "仅 PENDING 状态可撤销");
        }
        lr.setStatus("CANCELLED");
        applyAudit(lr, applicantId);
        requestMapper.updateById(lr);
        publisher.publishEvent(new BbpmsEvents.LeaveCancelledEvent(leaveId, applicantId));
        return enrich(lr);
    }

    @Override
    public List<LeaveVO> myApplications(Long applicantId, String status) {
        LambdaQueryWrapper<LeaveRequest> w = new LambdaQueryWrapper<>();
        w.eq(LeaveRequest::getApplicantId, applicantId);
        if (status != null && !status.isBlank()) w.eq(LeaveRequest::getStatus, status);
        w.orderByDesc(LeaveRequest::getAppliedAt);
        return requestMapper.selectList(w).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<LeaveVO> pendingApprovals(Long approverId, Integer approverLevel) {
        // Find leaves at exactly this level, PENDING, not yet decided.
        LambdaQueryWrapper<LeaveRequest> w = new LambdaQueryWrapper<>();
        w.eq(LeaveRequest::getStatus, "PENDING");
        w.eq(LeaveRequest::getRequiredLevel, approverLevel == null ? 1 : approverLevel);
        w.and(qw -> qw.isNull(LeaveRequest::getCurrentLevel).or().lt(LeaveRequest::getCurrentLevel, approverLevel == null ? 1 : approverLevel));
        w.orderByAsc(LeaveRequest::getAppliedAt);
        return requestMapper.selectList(w).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public List<LeaveVO> teamCalendar(LocalDateTime from, LocalDateTime to) {
        LambdaQueryWrapper<LeaveRequest> w = new LambdaQueryWrapper<>();
        w.eq(LeaveRequest::getStatus, "APPROVED");
        w.between(LeaveRequest::getStartAt, from, to);
        return requestMapper.selectList(w).stream().map(this::enrich).collect(Collectors.toList());
    }

    @Override
    public boolean hasActiveLeave(Long installerId, LocalDateTime at) {
        if (at == null) at = LocalDateTime.now();
        return requestMapper.findActiveLeave(installerId, at) != null;
    }

    @Override
    public List<LeaveRequest> findAllActiveLeaves(LocalDateTime at) {
        if (at == null) at = LocalDateTime.now();
        return requestMapper.findAllActive(at);
    }

    @Override
    public LeaveVO get(Long leaveId) {
        if (leaveId == null) return null;
        return enrich(requestMapper.selectById(leaveId));
    }

    /* ---------- internals ---------- */

    private LeaveRequest mustLoad(Long id) {
        LeaveRequest lr = requestMapper.selectById(id);
        if (lr == null) {
            throw new BizException(ResultCode.NOT_FOUND, "请假申请不存在");
        }
        return lr;
    }

    private LeaveVO enrich(LeaveRequest lr) {
        if (lr == null) return null;
        LeaveVO vo = LeaveVO.from(lr);
        if (vo == null) return null;
        try {
            SysUser u = userMapper.selectById(lr.getApplicantId());
            if (u != null) vo.setApplicantName(u.getRealName() == null ? u.getUsername() : u.getRealName());
            if (lr.getLevel1ApproverId() != null) {
                SysUser a1 = userMapper.selectById(lr.getLevel1ApproverId());
                if (a1 != null) vo.setLevel1ApproverName(a1.getRealName() == null ? a1.getUsername() : a1.getRealName());
            }
            if (lr.getLevel2ApproverId() != null) {
                SysUser a2 = userMapper.selectById(lr.getLevel2ApproverId());
                if (a2 != null) vo.setLevel2ApproverName(a2.getRealName() == null ? a2.getUsername() : a2.getRealName());
            }
        } catch (Exception ignored) { /* best-effort */ }
        return vo;
    }

    private void applyAudit(BaseDO d, Long userId) {
        Long uid = userId != null ? userId : SecurityUtils.getCurrentUserId();
        if (d.getCreateBy() == null) d.setCreateBy(uid);
        d.setUpdateBy(uid);
    }
}
