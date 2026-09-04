package com.bbpms.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.dto.OrderSummaryDTO;
import com.bbpms.common.enums.OrderEvent;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.common.statemachine.WorkOrderStateMachine;
import com.bbpms.common.util.SnowflakeIdGenerator;
import com.bbpms.order.service.OrderService;
import com.bbpms.workorder.config.WorkOrderProperties;
import com.bbpms.workorder.dto.WorkOrderCreateReq;
import com.bbpms.workorder.dto.WorkOrderQueryReq;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import com.bbpms.workorder.mapper.WorkOrderTimelineMapper;
import com.bbpms.workorder.service.WorkOrderService;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderDetailVO;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import com.bbpms.workorder.vo.WorkOrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default {@link WorkOrderService} implementation (monolith edition).
 *
 * <p>State transitions are validated via
 * {@link WorkOrderStateMachine}. Multi-table writes (work_order + timeline
 * + parent-order status update) are wrapped in
 * {@link Transactional @Transactional}. Cross-module effects use
 * {@link ApplicationEventPublisher} (replaces RabbitMQ) and direct
 * {@code @Autowired} of the Order module's {@link OrderService} (replaces
 * Feign).</p>
 */
@Slf4j
@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    /** Prefix for all business work-order numbers. */
    public static final String WORK_NO_PREFIX = "WO";

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderTimelineMapper timelineMapper;
    private final WorkOrderTimelineService timelineService;
    private final OrderService orderService;
    private final WorkOrderStateMachine workOrderStateMachine;
    private final ApplicationEventPublisher publisher;
    private final WorkOrderProperties workOrderProperties;
    private final SnowflakeIdGenerator snowflakeIdGenerator;

    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper,
                                WorkOrderTimelineMapper timelineMapper,
                                WorkOrderTimelineService timelineService,
                                OrderService orderService,
                                WorkOrderStateMachine workOrderStateMachine,
                                ApplicationEventPublisher publisher,
                                WorkOrderProperties workOrderProperties,
                                @Qualifier("workorderSnowflakeIdGenerator") SnowflakeIdGenerator snowflakeIdGenerator) {
        this.workOrderMapper = workOrderMapper;
        this.timelineMapper = timelineMapper;
        this.timelineService = timelineService;
        this.orderService = orderService;
        this.workOrderStateMachine = workOrderStateMachine;
        this.publisher = publisher;
        this.workOrderProperties = workOrderProperties;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
    }

    /* ============================================================ */
    /*  create                                                       */
    /* ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "创建工单", module = "workorder")
    public WorkOrderVO create(WorkOrderCreateReq req) {
        if (req == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "请求不能为空");
        }
        if (req.getOrderId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        }

        // 1. Idempotency on orderId — only for ACTIVE work orders. A cancelled /
        //    auto-cancelled / completed work order must NOT short-circuit the
        //    re-dispatch flow, otherwise reassign / SLA auto-redispatch would
        //    keep returning the dead work order and never create a new one.
        WorkOrder existing = workOrderMapper.selectByOrderId(req.getOrderId());
        if (existing != null) {
            if (WorkOrderStatus.isTerminal(existing.getStatusEnum())) {
                log.info("Existing WO {} is terminal ({}); creating a new work order for orderId={}",
                        existing.getWorkNo(), existing.getStatus(), req.getOrderId());
            } else {
                log.info("Idempotent create — returning existing workNo={}", existing.getWorkNo());
                return toVO(existing);
            }
        }

        // 2. Determine dispatcher / installer.
        Long dispatcherId = req.getDispatcherId();
        if (dispatcherId == null) {
            dispatcherId = SecurityUtils.getCurrentUserId();
        }
        Long installerId = req.getInstallerId();

        // 3. Generate workNo = "WO" + snowflake.
        String workNo = WORK_NO_PREFIX + snowflakeIdGenerator.nextId();

        WorkOrder wo = new WorkOrder();
        wo.setWorkNo(workNo);
        wo.setOrderId(req.getOrderId());
        wo.setInstallerId(installerId);
        wo.setDispatcherId(dispatcherId);
        wo.setStatus(WorkOrderStatus.DISPATCHED.getCode());
        wo.setBusinessType("BROADBAND_INSTALL");
        wo.setDispatchTime(LocalDateTime.now());
        wo.setCreateBy(dispatcherId);
        wo.setUpdateBy(dispatcherId);

        // Pull install address / customer phone / package name from the order
        // service so the work order keeps its own snapshot.
        try {
            OrderSummaryDTO summary = orderService.snapshot(req.getOrderId());
            if (summary != null) {
                wo.setInstallAddress(summary.getInstallAddress());
                wo.setCustomerPhone(summary.getContactPhone());
                wo.setPackageName(summary.getPackageName());
            }
        } catch (Exception ex) {
            log.debug("enrichFromOrder failed (non-fatal): {}", ex.getMessage());
        }

        workOrderMapper.insert(wo);

        // Timeline row: null -> DISPATCHED.
        appendTimeline(wo.getId(), null, WorkOrderStatus.DISPATCHED,
                dispatcherId, "DISPATCHER", req.getRemark());

        // Flip parent order -> DISPATCHED via direct OrderService injection.
        try {
            orderService.updateStatus(req.getOrderId(), OrderStatus.DISPATCHED, dispatcherId);
        } catch (Exception ex) {
            log.warn("Parent order status flip failed orderId={}: {}", req.getOrderId(), ex.getMessage());
        }

        // Outbound internal event replaces RabbitMQ.
        publisher.publishEvent(new BbpmsEvents.WorkOrderDispatchedEvent(
                wo.getId(), wo.getWorkNo(), wo.getOrderId(),
                wo.getInstallerId(), null));

        log.info("Created work order {} for order {}", workNo, req.getOrderId());
        return toVO(wo);
    }

    /* ============================================================ */
    /*  accept / start / complete                                    */
    /* ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "接单", module = "workorder")
    public WorkOrderVO accept(Long workOrderId, Long installerId) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        assertTransition(current.getStatusEnum(), WorkOrderStatus.ACCEPTED);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(WorkOrderStatus.ACCEPTED.getCode());
        update.setAcceptTime(LocalDateTime.now());
        update.setUpdateBy(installerId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }

        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.ACCEPTED,
                installerId, "INSTALLER", "installer accepted the work order");

        WorkOrder reloaded = workOrderMapper.selectById(workOrderId);
        publisher.publishEvent(new BbpmsEvents.WorkOrderAcceptedEvent(reloaded.getId(), installerId));
        return toVO(reloaded);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "开始施工", module = "workorder")
    public WorkOrderVO start(Long workOrderId, Long installerId) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        assertTransition(current.getStatusEnum(), WorkOrderStatus.IN_PROGRESS);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        update.setStartTime(LocalDateTime.now());
        update.setUpdateBy(installerId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.IN_PROGRESS,
                installerId, "INSTALLER", "installer started the install");

        return toVO(workOrderMapper.selectById(workOrderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "完成工单", module = "workorder")
    public void complete(Long workOrderId, Long installerId, Long installRecordId) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        assertTransition(current.getStatusEnum(), WorkOrderStatus.COMPLETED);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(WorkOrderStatus.COMPLETED.getCode());
        update.setFinishTime(LocalDateTime.now());
        update.setUpdateBy(installerId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        String remark = installRecordId == null
                ? "completed"
                : "completed, installRecordId=" + installRecordId;
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.COMPLETED,
                installerId, "INSTALLER", remark);

        // Mirror to parent order.
        if (current.getOrderId() != null) {
            try {
                orderService.updateStatus(current.getOrderId(), OrderStatus.FINISHED, installerId);
            } catch (Exception ex) {
                log.warn("Parent order flip to FINISHED failed orderId={}: {}",
                        current.getOrderId(), ex.getMessage());
            }
        }

        WorkOrder reloaded = workOrderMapper.selectById(workOrderId);
        publisher.publishEvent(new BbpmsEvents.WorkOrderCompletedEvent(
                reloaded.getId(), installerId));
    }

    /* ============================================================ */
    /*  transfer / cancel / admin                                    */
    /* ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "转单", module = "workorder")
    public void transfer(Long workOrderId, Long installerId, String reason) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException(ResultCode.BAD_REQUEST, "转单原因不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        // Transfer is permitted from DISPATCHED or ACCEPTED.
        if (current.getStatusEnum() != WorkOrderStatus.DISPATCHED
                && current.getStatusEnum() != WorkOrderStatus.ACCEPTED) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "当前状态 " + current.getStatusEnum() + " 不允许转单");
        }
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setInstallerId(null);          // returned to pool
        update.setStatus(WorkOrderStatus.DISPATCHED.getCode());
        update.setDispatchTime(LocalDateTime.now());
        update.setUpdateBy(installerId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.DISPATCHED,
                installerId, "INSTALLER", "transfer: " + reason);

        // Notify listeners — the workorder pool may re-pick this back to a fresh installer.
        publisher.publishEvent(new BbpmsEvents.WorkOrderTransferEvent(
                workOrderId,
                current.getInstallerId(),   // who gave it up
                reason));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "取消工单", module = "workorder")
    public void cancel(Long workOrderId, Long operatorId, String reason) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (current.getStatusEnum() == WorkOrderStatus.COMPLETED
                || current.getStatusEnum() == WorkOrderStatus.CANCELLED) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "工单已结束，无法取消");
        }
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(WorkOrderStatus.CANCELLED.getCode());
        update.setFinishTime(LocalDateTime.now());
        update.setUpdateBy(operatorId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.CANCELLED,
                operatorId, "OPERATOR", reason == null ? "cancelled" : reason);

        if (current.getOrderId() != null) {
            try {
                orderService.updateStatus(current.getOrderId(), OrderStatus.CANCELLED, operatorId);
            } catch (Exception ex) {
                log.warn("Parent order flip to CANCELLED failed: {}", ex.getMessage());
            }
        }

        // Notify listeners — DispatchFailedEvent is the canonical signal for
        // "we had a work order assigned and it failed to complete". Hooks can
        // reschedule the parent order, re-dispatch, or alert the dispatcher.
        publisher.publishEvent(new BbpmsEvents.WorkOrderDispatchFailedEvent(
                workOrderId,
                current.getInstallerId(),
                reason == null ? "cancelled" : reason));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "修改工单状态", module = "workorder")
    public void updateStatus(Long workOrderId, WorkOrderStatus targetStatus, String remark, Long operatorId) {
        if (workOrderId == null || targetStatus == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 和 status 必填");
        }
        WorkOrder current = mustLoad(workOrderId);
        workOrderStateMachine.assertTransition(current.getStatusEnum(), targetStatus, null);
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(targetStatus.getCode());
        update.setUpdateBy(operatorId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), targetStatus,
                operatorId, "ADMIN", remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revertToInstalling(Long workOrderId) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (current.getStatusEnum() != WorkOrderStatus.COMPLETED) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "仅 COMPLETED 工单可回滚到 INSTALLING (workorder perspective)");
        }
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatus(WorkOrderStatus.IN_PROGRESS.getCode());
        update.setUpdateBy(SecurityUtils.getCurrentUserId());
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.IN_PROGRESS,
                SecurityUtils.getCurrentUserId(), "ADMIN", "BSS compensation rollback");
    }

    /* ============================================================ */
    /*  reads                                                        */
    /* ============================================================ */

    @Override
    public WorkOrderDetailVO getDetail(Long workOrderId) {
        WorkOrder wo = mustLoad(workOrderId);
        WorkOrderDetailVO vo = new WorkOrderDetailVO();
        BeanUtils.copyProperties(wo, vo, "status");
        WorkOrderStatus s = wo.getStatusEnum();
        vo.setStatus(s);
        vo.setStatusDesc(s == null ? null : s.getDesc());
        vo.setTimeline(timelineService.getTimeline(workOrderId));
        enrichDetail(vo, wo);
        return vo;
    }

    @Override
    public WorkOrderVO findByOrderId(Long orderId) {
        WorkOrder wo = workOrderMapper.selectByOrderId(orderId);
        return wo == null ? null : toVO(wo);
    }

    @Override
    public WorkOrder getById(Long id) {
        if (id == null) return null;
        return workOrderMapper.selectById(id);
    }

    @Override
    public PageResp<WorkOrderVO> page(WorkOrderQueryReq req) {
        long pageNum  = req.getPageNum()  == null ? 1L  : req.getPageNum();
        long pageSize = req.getPageSize() == null ? 20L : req.getPageSize();
        Page<WorkOrder> page = new Page<>(pageNum, pageSize);
        IPage<WorkOrder> result = workOrderMapper.selectPageWithScope(page, req);
        // Convert the IPage in place using the mapper result, then map records.
        Page<WorkOrder> populated = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        populated.setRecords(result.getRecords());
        IPage<WorkOrderVO> voPage = populated.convert(this::toVO);
        return PageResp.of(voPage);
    }

    /* ============================================================ */
    /*  helpers                                                      */
    /* ============================================================ */

    private WorkOrder mustLoad(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) {
            throw new BizException(ResultCode.WORKORDER_NOT_FOUND);
        }
        return wo;
    }

    private void assertTransition(WorkOrderStatus from, WorkOrderStatus to) {
        try {
            workOrderStateMachine.assertTransition(from, to, null);
        } catch (BizException ex) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "工单状态 " + from + " -> " + to + " 不允许");
        }
    }

    private void appendTimeline(Long workOrderId, WorkOrderStatus from, WorkOrderStatus to,
                                Long operatorId, String operatorRole, String remark) {
        WorkOrderTimeline t = new WorkOrderTimeline();
        t.setWorkOrderId(workOrderId);
        t.setFromStatus(from == null ? null : from.getCode());
        t.setToStatus(to == null ? null : to.getCode());
        t.setOperatorId(operatorId);
        t.setOperatorRole(operatorRole);
        t.setRemark(remark);
        t.setCreateBy(operatorId);
        t.setUpdateBy(operatorId);
        timelineMapper.insert(t);
    }

    private void enrichDetail(WorkOrderDetailVO vo, WorkOrder wo) {
        try {
            if (wo.getOrderId() != null) {
                OrderSummaryDTO summary = orderService.snapshot(wo.getOrderId());
                if (summary != null) {
                    vo.setOrderNo(summary.getOrderNo());
                    vo.setPackageName(summary.getPackageName());
                }
            }
        } catch (Exception ignore) {
            // enrich is non-essential
        }
    }

    private WorkOrderVO toVO(WorkOrder wo) {
        if (wo == null) return null;
        WorkOrderVO vo = new WorkOrderVO();
        BeanUtils.copyProperties(wo, vo, "status");
        // entity.status is String; VO.status is WorkOrderStatus enum — copy manually.
        WorkOrderStatus s = wo.getStatusEnum();
        vo.setStatus(s);
        vo.setStatusDesc(s == null ? null : s.getDesc());
        return vo;
    }

    /* ============================================================ */
    /*  Phase 5: SLA / lifecycle hardening                          */
    /* ============================================================ */

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "工单停滞", module = "workorder")
    public void markStalled(Long workOrderId, String reason, String triggerSource) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (WorkOrderStatus.isTerminal(current.getStatusEnum())) {
            log.debug("markStalled: work order {} already terminal ({})", workOrderId, current.getStatus());
            return;
        }
        workOrderStateMachine.assertSystemTransition(current.getStatusEnum(), WorkOrderStatus.STALLED);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatusEnum(WorkOrderStatus.STALLED);
        update.setStallReason(reason == null ? "STALLED" : reason);
        update.setUpdateBy(SecurityUtils.getCurrentUserId());
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        String operatorRole = "SYSTEM".equalsIgnoreCase(triggerSource) ? "SYSTEM" :
                ("DISPATCHER".equalsIgnoreCase(triggerSource) ? "DISPATCHER" : "INSTALLER");
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.STALLED,
                SecurityUtils.getCurrentUserId(), operatorRole,
                "[" + (triggerSource == null ? "MANUAL" : triggerSource) + "] " + (reason == null ? "" : reason));
        log.info("markStalled: work order {} → STALLED (reason={}, source={})",
                workOrderId, reason, triggerSource);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "恢复施工", module = "workorder")
    public WorkOrderVO resume(Long workOrderId, Long operatorId) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (current.getStatusEnum() != WorkOrderStatus.STALLED) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "仅 STALLED 工单可恢复，当前状态 " + current.getStatus());
        }
        workOrderStateMachine.assertTransition(current.getStatusEnum(),
                WorkOrderStatus.IN_PROGRESS, null);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatusEnum(WorkOrderStatus.IN_PROGRESS);
        update.setStallReason(null);
        update.setLastActiveAt(LocalDateTime.now());
        update.setUpdateBy(operatorId);
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.IN_PROGRESS,
                operatorId, "OPERATOR", "resumed from STALLED");
        publisher.publishEvent(new BbpmsEvents.WorkOrderResumedEvent(workOrderId, current.getInstallerId()));
        return toVO(workOrderMapper.selectById(workOrderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "改派工单", module = "workorder")
    public WorkOrderVO reassign(Long workOrderId, Long newInstallerId, Long operatorId, String reason) {
        if (workOrderId == null || newInstallerId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId / newInstallerId 必填");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (WorkOrderStatus.isTerminal(current.getStatusEnum())) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID,
                    "工单已结束，无法改派");
        }
        Long fromInstaller = current.getInstallerId();
        // Mark this row DISPATCHED, clear installer, dispatch to new installer
        workOrderStateMachine.assertSystemTransition(current.getStatusEnum(), WorkOrderStatus.REASSIGNING);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatusEnum(WorkOrderStatus.REASSIGNING);
        update.setInstallerId(null);
        update.setUpdateBy(operatorId);
        if (workOrderMapper.updateById(update) == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.REASSIGNING,
                operatorId, "DISPATCHER", "reassign: " + (reason == null ? "" : reason));

        // Now set the new installer and back to DISPATCHED
        WorkOrder reassign = new WorkOrder();
        reassign.setId(workOrderId);
        reassign.setVersion(current.getVersion() + 1);
        reassign.setStatusEnum(WorkOrderStatus.DISPATCHED);
        reassign.setInstallerId(newInstallerId);
        reassign.setDispatcherId(operatorId);
        reassign.setDispatchTime(LocalDateTime.now());
        reassign.setStallReason(null);
        reassign.setUpdateBy(operatorId);
        if (workOrderMapper.updateById(reassign) == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, WorkOrderStatus.REASSIGNING, WorkOrderStatus.DISPATCHED,
                operatorId, "DISPATCHER", "reassigned to installer #" + newInstallerId);

        publisher.publishEvent(new BbpmsEvents.WorkOrderReassignedEvent(
                workOrderId, fromInstaller, newInstallerId, reason));
        return toVO(workOrderMapper.selectById(workOrderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "工单自动取消", module = "workorder")
    public void autoCancel(Long workOrderId, String reason, String cancelType) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (WorkOrderStatus.isTerminal(current.getStatusEnum())) {
            log.debug("autoCancel: work order {} already terminal", workOrderId);
            return;
        }
        workOrderStateMachine.assertSystemTransition(current.getStatusEnum(), WorkOrderStatus.AUTO_CANCELLED);

        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatusEnum(WorkOrderStatus.AUTO_CANCELLED);
        update.setCancelType(cancelType == null ? "SYSTEM" : cancelType);
        update.setFinishTime(LocalDateTime.now());
        update.setUpdateBy(SecurityUtils.getCurrentUserId());
        int rows = workOrderMapper.updateById(update);
        if (rows == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.AUTO_CANCELLED,
                SecurityUtils.getCurrentUserId(), "SYSTEM",
                (reason == null ? "" : reason) + " [auto]");
        log.info("autoCancel: work order {} → AUTO_CANCELLED (reason={}, type={})",
                workOrderId, reason, cancelType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @OperationLog(value = "管理员强制关闭工单", module = "workorder")
    public void forceClose(Long workOrderId, Long operatorId, String reason, String cancelType) {
        if (workOrderId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }
        WorkOrder current = mustLoad(workOrderId);
        if (WorkOrderStatus.isTerminal(current.getStatusEnum())) {
            throw new BizException(ResultCode.WORKORDER_STATUS_INVALID, "工单已结束");
        }
        // Force close only requires super_admin (role 1); we skip the user-role check
        // and rely on the controller's @PreAuthorize for the gate.
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setStatusEnum(WorkOrderStatus.CANCELLED);
        update.setCancelType(cancelType == null ? "ADMIN" : cancelType);
        update.setFinishTime(LocalDateTime.now());
        update.setUpdateBy(operatorId);
        if (workOrderMapper.updateById(update) == 0) {
            throw new BizException(ResultCode.VERSION_CONFLICT, "工单版本冲突，请刷新后重试");
        }
        appendTimeline(workOrderId, current.getStatusEnum(), WorkOrderStatus.CANCELLED,
                operatorId, "ADMIN", "[FORCE] " + (reason == null ? "" : reason));
        publisher.publishEvent(new BbpmsEvents.WorkOrderForceClosedEvent(
                workOrderId, operatorId, reason, cancelType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markHeartbeat(Long workOrderId) {
        if (workOrderId == null) return;
        WorkOrder current = mustLoad(workOrderId);
        if (current.getStatusEnum() != WorkOrderStatus.IN_PROGRESS
                && current.getStatusEnum() != WorkOrderStatus.ACCEPTED
                && current.getStatusEnum() != WorkOrderStatus.DISPATCHED) {
            return; // heartbeat is irrelevant in terminal states
        }
        WorkOrder update = new WorkOrder();
        update.setId(workOrderId);
        update.setVersion(current.getVersion());
        update.setLastActiveAt(LocalDateTime.now());
        update.setUpdateBy(current.getInstallerId());
        workOrderMapper.updateById(update);
    }

    @Override
    public List<WorkOrderVO> listExpiring(int withinMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(withinMinutes);
        LambdaQueryWrapper<WorkOrder> w = new LambdaQueryWrapper<>();
        w.in(WorkOrder::getStatus, "DISPATCHED", "IN_PROGRESS")
         .isNotNull(WorkOrder::getExpectedFinishTime)
         .le(WorkOrder::getExpectedFinishTime, cutoff)
         .orderByAsc(WorkOrder::getExpectedFinishTime)
         .last("LIMIT 100");
        return workOrderMapper.selectList(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * Helper for the SLA scheduler: raw select with wrapper.
     */
    public List<WorkOrder> selectList(LambdaQueryWrapper<WorkOrder> w) {
        return workOrderMapper.selectList(w);
    }
}
