package com.bbpms.workorder.service;

import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.result.PageResp;
import com.bbpms.workorder.dto.WorkOrderCreateReq;
import com.bbpms.workorder.dto.WorkOrderQueryReq;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.vo.WorkOrderDetailVO;
import com.bbpms.workorder.vo.WorkOrderVO;

import java.util.List;

/**
 * Work-order lifecycle contract. Every state-mutating method is wrapped in
 * Spring {@code @Transactional} so the multi-table writes
 * (work_order / timeline) commit atomically. Cross-module effects go
 * through Spring {@code ApplicationEventPublisher}; cross-module reads go
 * through direct {@code @Autowired} of same-process services.
 */
public interface WorkOrderService {

    /**
     * Create a new work order. Idempotent on {@code orderId}: if a work
     * order already exists, the existing {@link WorkOrderVO} is returned.
     */
    WorkOrderVO create(WorkOrderCreateReq req);

    /** Assign an existing PENDING pool row instead of creating a duplicate. */
    WorkOrderVO assignPending(Long workOrderId, Long installerId, Long dispatcherId, String remark);

    /** DISPATCHED -> ACCEPTED. */
    WorkOrderVO accept(Long workOrderId, Long installerId);

    /** ACCEPTED -> IN_PROGRESS. */
    WorkOrderVO start(Long workOrderId, Long installerId);

    /** IN_PROGRESS -> COMPLETED + link to install record. */
    void complete(Long workOrderId, Long installerId, Long installRecordId);

    /** DISPATCHED / ACCEPTED -> DISPATCHED with installerId cleared (returned to pool). */
    void transfer(Long workOrderId, Long installerId, String reason);

    /** Any non-terminal -> CANCELLED. */
    void cancel(Long workOrderId, Long operatorId, String reason);

    /** Admin-override PUT to a target status. Validated via state machine. */
    void updateStatus(Long workOrderId, WorkOrderStatus targetStatus, String remark, Long operatorId);

    /** Used by BSS compensation to roll back COMPLETED -> IN_PROGRESS. */
    void revertToInstalling(Long workOrderId);

    /* ---------- Phase 5 additions ---------- */

    /** Any non-terminal -> STALLED with a reason. */
    void markStalled(Long workOrderId, String reason, String triggerSource);

    /** STALLED -> IN_PROGRESS (resume work). */
    WorkOrderVO resume(Long workOrderId, Long operatorId);

    /** DISPATCHED/STALLED/IN_PROGRESS -> reassigned to a new installer. */
    WorkOrderVO reassign(Long workOrderId, Long newInstallerId, Long operatorId, String reason);

    /** System-driven auto-cancel (SLA breach, dispatch-failed, etc). */
    void autoCancel(Long workOrderId, String reason, String cancelType);

    /** Admin force-close: skip state machine role check, force CANCELLED. */
    void forceClose(Long workOrderId, Long operatorId, String reason, String cancelType);

    /** Refresh {@code lastActiveAt} and {@code expectedFinishTime} for the SLA engine. */
    void markHeartbeat(Long workOrderId);

    /** List of work orders expiring within the given minutes (for SLA dashboard). */
    List<WorkOrderVO> listExpiring(int withinMinutes);

    /**
     * Raw select for internal callers (SLA scheduler). Returns matching
     * {@link WorkOrder} rows using the given wrapper.
     */
    java.util.List<WorkOrder> selectList(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WorkOrder> w);

    /* ---------- reads ---------- */

    WorkOrderDetailVO getDetail(Long workOrderId);

    WorkOrderVO findByOrderId(Long orderId);

    PageResp<WorkOrderVO> page(WorkOrderQueryReq req);

    /** Single-row read by primary key — used by the Order module. */
    WorkOrder getById(Long id);
}
