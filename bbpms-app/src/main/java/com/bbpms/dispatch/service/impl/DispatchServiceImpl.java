package com.bbpms.dispatch.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.lock.RedissonDistributedLock;
import com.bbpms.common.result.PageResp;
import com.bbpms.common.util.JsonUtils;
import com.bbpms.common.util.RedisUtils;
import com.bbpms.common.util.SecurityUtils;
import com.bbpms.dispatch.algorithm.DispatchScoringService;
import com.bbpms.dispatch.config.DispatchProperties;
import com.bbpms.dispatch.dto.CandidateDTO;
import com.bbpms.dispatch.dto.DispatchQueryReq;
import com.bbpms.dispatch.dto.DispatchResultDTO;
import com.bbpms.dispatch.dto.InstallerDTO;
import com.bbpms.dispatch.dto.ManualDispatchReq;
import com.bbpms.dispatch.dto.OrderDTO;
import com.bbpms.dispatch.dto.ReassignReq;
import com.bbpms.dispatch.entity.DispatchRecord;
import com.bbpms.dispatch.entity.DispatchRule;
import com.bbpms.dispatch.mapper.DispatchRecordMapper;
import com.bbpms.dispatch.service.DispatchRuleService;
import com.bbpms.dispatch.service.DispatchService;
import com.bbpms.dispatch.vo.DispatchRecordVO;
import com.bbpms.dispatch.vo.DispatchStatVO;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.service.OrderService;
import com.bbpms.leave.entity.LeaveRequest;
import com.bbpms.leave.service.LeaveService;
import com.bbpms.user.service.InstallerProfileService;
import com.bbpms.user.vo.InstallerVO;
import com.bbpms.workorder.dto.WorkOrderCreateReq;
import com.bbpms.workorder.service.WorkOrderService;
import com.bbpms.workorder.vo.WorkOrderDetailVO;
import com.bbpms.workorder.vo.WorkOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Core dispatch orchestrator.
 *
 * <p>Write paths (auto / manual / reassign) are transactional and use
 * Redisson locks to keep concurrent dispatch safe per installer. Cross-module
 * side effects (work order creation, order status flip, notifications) are
 * performed via the Spring application event bus rather than MQ.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DispatchServiceImpl implements DispatchService {

    public static final String REDIS_REASSIGN_COOLDOWN_PREFIX = "dispatch:cooldown:";

    private final OrderService orderService;
    private final WorkOrderService workOrderService;
    private final InstallerProfileService installerProfileService;
    private final DispatchScoringService scoringService;
    private final DispatchRuleService ruleService;
    private final DispatchRecordMapper recordMapper;
    private final RedisUtils redisUtils;
    private final RedissonDistributedLock lock;
    private final DispatchProperties props;
    private final com.bbpms.leave.service.LeaveService leaveService;

    /* ========================= AUTO ========================= */

    @Override
    @OperationLog(module = "dispatch", value = "自动派单")
    @Transactional(rollbackFor = Exception.class)
    public DispatchResultDTO autoDispatch(Long orderId) {
        if (orderId == null) throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");

        // SA-P2-004: serialize dispatch per order. The lock MUST be taken before
        // the first SELECT so the transaction's REPEATABLE_READ snapshot is
        // created inside the lock — otherwise the short-circuit query below reuses
        // a stale snapshot that cannot see the previous winner's committed work
        // order, and a duplicate WO + record is created anyway. Released in
        // afterCompletion (after commit), not at method exit.
        String orderLockKey = "lock:dispatch-order:" + orderId;
        if (!lock.tryLock(orderLockKey,
                props.getLockWaitSeconds(), props.getLockLeaseSeconds(), TimeUnit.SECONDS)) {
            throw new BizException(ResultCode.LOCK_CONTENDED, "该订单正在派单中");
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.unlock(orderLockKey);
                    }
                });

        BroadbandOrder order = orderService.findByOrderId(orderId);
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);

        // Idempotent short-circuit: a non-terminal work order already exists
        // for this order (dispatched by an earlier auto/manual call).
        WorkOrderVO existingWo = workOrderService.findByOrderId(orderId);
        if (existingWo != null
                && !WorkOrderStatus.isTerminal(existingWo.getStatus())
                && existingWo.getStatus() != WorkOrderStatus.PENDING) {
            log.info("autoDispatch orderId={} idempotent hit existing workNo={}",
                    orderId, existingWo.getWorkNo());
            return new DispatchResultDTO(existingWo.getId(), existingWo.getWorkNo(),
                    existingWo.getInstallerId(), existingWo.getInstallerName(), 0D, null);
        }

        DispatchRule rule = ruleService.getActive();
        OrderDTO orderDTO = toOrderDTO(order);
        List<InstallerDTO> candidates = loadCandidates();
        if (candidates.isEmpty()) {
            throw new BizException(ResultCode.NO_DISPATCH_CANDIDATE);
        }

        List<CandidateDTO> ranked = scoringService.score(orderDTO, candidates, rule);
        if (ranked.isEmpty()) {
            throw new BizException(ResultCode.NO_DISPATCH_CANDIDATE, "无符合服务半径的装维人员");
        }

        // Pick the first rank-ordered installer whose lock we can acquire and
        // HOLD the lock through the entire write (WO create + order flip +
        // dispatch-record insert). The previous implementation unlocked
        // immediately, so two concurrent dispatches could assign the same
        // installer twice.
        InstallerDTO winner = null;
        String winnerLockKey = null;
        for (CandidateDTO c : ranked) {
            InstallerDTO inst = candidates.stream()
                    .filter(i -> i.id() != null && i.id().equals(c.installerId()))
                    .findFirst().orElse(null);
            if (inst == null) continue;
            String lockKey = "lock:dispatch:" + inst.id();
            if (lock.tryLock(lockKey,
                    props.getLockWaitSeconds(), props.getLockLeaseSeconds(), TimeUnit.SECONDS)) {
                winner = inst;
                winnerLockKey = lockKey;
                break;
            }
        }
        if (winner == null) {
            throw new BizException(ResultCode.LOCK_CONTENDED, "无空闲装维人员");
        }

        final String heldWinnerLockKey = winnerLockKey;
        final InstallerDTO selectedWinner = winner;
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.unlock(heldWinnerLockKey);
                    }
                });

        String candidatesJson = JsonUtils.toJson(ranked);
        CandidateDTO winnerCandidate = ranked.stream()
                .filter(candidate -> selectedWinner.id().equals(candidate.installerId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ResultCode.NO_DISPATCH_CANDIDATE));

        WorkOrderVO wo;
        if (existingWo != null && existingWo.getStatus() == WorkOrderStatus.PENDING) {
            wo = workOrderService.assignPending(existingWo.getId(), winner.id(), null, "AUTO_DISPATCH");
        } else {
            WorkOrderCreateReq req = new WorkOrderCreateReq();
            req.setOrderId(orderId);
            req.setInstallerId(winner.id());
            req.setRemark("AUTO_DISPATCH");
            wo = workOrderService.create(req);
        }

        DispatchRecord rec = new DispatchRecord();
        rec.setWorkOrderId(wo.getId());
        rec.setInstallerId(winner.id());
        rec.setStrategy("AUTO");
        rec.setScore(BigDecimal.valueOf(winnerCandidate.totalScore()).setScale(2, RoundingMode.HALF_UP));
        rec.setCandidatesJson(candidatesJson);
        rec.setReason("AUTO");
        recordMapper.insert(rec);

        log.info("autoDispatch orderId={} -> workOrderId={} installerId={} score={}",
                orderId, wo.getId(), winner.id(), winnerCandidate.totalScore());

        return new DispatchResultDTO(
                wo.getId(),
                wo.getWorkNo(),
                winner.id(),
                winner.name(),
                winnerCandidate.totalScore(),
                candidatesJson);
    }

    /* ========================= MANUAL ========================= */

    @Override
    @OperationLog(module = "dispatch", value = "手动派单")
    @Transactional(rollbackFor = Exception.class)
    public DispatchResultDTO manualDispatch(ManualDispatchReq req) {
        if (req == null || req.getOrderId() == null || req.getInstallerId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "orderId/installerId 不能为空");
        }

        // SA-P2-004: same order-scoped serialization as autoDispatch (lock
        // before the first SELECT so the snapshot is created inside the lock).
        String orderLockKey = "lock:dispatch-order:" + req.getOrderId();
        if (!lock.tryLock(orderLockKey,
                props.getLockWaitSeconds(), props.getLockLeaseSeconds(), TimeUnit.SECONDS)) {
            throw new BizException(ResultCode.LOCK_CONTENDED, "该订单正在派单中");
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.unlock(orderLockKey);
                    }
                });

        BroadbandOrder order = orderService.findByOrderId(req.getOrderId());
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        WorkOrderVO existingWo = workOrderService.findByOrderId(req.getOrderId());
        if (existingWo != null
                && !WorkOrderStatus.isTerminal(existingWo.getStatus())
                && existingWo.getStatus() != WorkOrderStatus.PENDING) {
            log.info("manualDispatch orderId={} idempotent hit existing workNo={}",
                    req.getOrderId(), existingWo.getWorkNo());
            return new DispatchResultDTO(existingWo.getId(), existingWo.getWorkNo(),
                    existingWo.getInstallerId(), existingWo.getInstallerName(), 0D, null);
        }

        String lockKey = "lock:dispatch:" + req.getInstallerId();
        boolean locked = lock.tryLock(lockKey,
                props.getLockWaitSeconds(), props.getLockLeaseSeconds(), TimeUnit.SECONDS);
        if (!locked) {
            throw new BizException(ResultCode.LOCK_CONTENDED, "该装维人员正在派单中");
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.unlock(lockKey);
                    }
                });

        Long dispatcherId = req.getDispatcherId() == null
                ? SecurityUtils.getCurrentUserId() : req.getDispatcherId();
        WorkOrderVO wo;
        if (existingWo != null && existingWo.getStatus() == WorkOrderStatus.PENDING) {
            wo = workOrderService.assignPending(existingWo.getId(), req.getInstallerId(),
                    dispatcherId, req.getReason());
        } else {
            WorkOrderCreateReq woReq = new WorkOrderCreateReq();
            woReq.setOrderId(req.getOrderId());
            woReq.setInstallerId(req.getInstallerId());
            woReq.setDispatcherId(dispatcherId);
            woReq.setRemark(req.getReason());
            wo = workOrderService.create(woReq);
        }

        DispatchRecord rec = new DispatchRecord();
        rec.setWorkOrderId(wo.getId());
        rec.setInstallerId(req.getInstallerId());
        rec.setStrategy("MANUAL");
        rec.setScore(BigDecimal.ZERO);
        rec.setCandidatesJson("[]");
        rec.setReason(req.getReason());
        recordMapper.insert(rec);

        return new DispatchResultDTO(wo.getId(), wo.getWorkNo(),
                req.getInstallerId(), wo.getInstallerName(), null, "[]");
    }

    /* ========================= REASSIGN ========================= */

    @Override
    @OperationLog(module = "dispatch", value = "改派")
    @Transactional(rollbackFor = Exception.class)
    public DispatchResultDTO reassign(ReassignReq req) {
        if (req == null || req.getWorkOrderId() == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "workOrderId 不能为空");
        }

        // Cooldown guard
        String cooldownKey = REDIS_REASSIGN_COOLDOWN_PREFIX + req.getWorkOrderId();
        if (redisUtils.get(cooldownKey) != null) {
            throw new BizException(ResultCode.LOCK_CONTENDED, "请勿频繁改派");
        }
        redisUtils.set(cooldownKey, "1",
                props.getReassignCooldownMinutes(), TimeUnit.MINUTES);

        // Cancel old work order then auto-dispatch the underlying order again.
        try {
            workOrderService.cancel(req.getWorkOrderId(), null, req.getReason());
        } catch (Exception e) {
            log.warn("cancel existing work order failed (may already be terminal): {}", e.getMessage());
        }

        Long orderId = resolveOrderId(req.getWorkOrderId());
        if (orderId == null) {
            throw new BizException(ResultCode.NOT_FOUND, "无法定位源订单");
        }

        DispatchResultDTO fresh = autoDispatch(orderId);

        List<DispatchRecord> records = recordMapper.selectByWorkOrderId(fresh.workOrderId());
        if (records != null && !records.isEmpty()) {
            DispatchRecord latest = records.get(0);
            latest.setStrategy("REASSIGN");
            latest.setReason(req.getReason());
            recordMapper.updateById(latest);
        }
        return fresh;
    }

    /* ========================= READ PATHS ========================= */

    @Override
    public List<CandidateDTO> getCandidates(Long orderId, Long excludeInstallerId, int limit) {
        if (orderId == null) throw new BizException(ResultCode.BAD_REQUEST, "orderId 不能为空");
        BroadbandOrder order = orderService.findByOrderId(orderId);
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        DispatchRule rule = ruleService.getActive();
        OrderDTO orderDTO = toOrderDTO(order);
        List<InstallerDTO> candidates = loadCandidates();
        if (excludeInstallerId != null) {
            candidates = candidates.stream()
                    .filter(i -> !excludeInstallerId.equals(i.id()))
                    .toList();
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return scoringService.score(orderDTO, candidates, rule).stream()
                .limit(safeLimit)
                .toList();
    }

    @Override
    public PageResp<DispatchRecordVO> pageRecords(DispatchQueryReq req) {
        IPage<DispatchRecord> page = new Page<>(
                req.getPageNum() == null ? 1 : req.getPageNum(),
                req.getPageSize() == null ? 20 : req.getPageSize());
        IPage<DispatchRecord> result = recordMapper.selectPageWithScope(page, req);
        List<DispatchRecordVO> vos = new ArrayList<>();
        for (DispatchRecord r : result.getRecords()) {
            DispatchRecordVO vo = new DispatchRecordVO();
            BeanUtils.copyProperties(r, vo);
            vos.add(vo);
        }
        IPage<DispatchRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(vos);
        return PageResp.of(voPage);
    }

    @Override
    public DispatchStatVO stats(Integer days) {
        int lookback = days == null || days <= 0 ? 7 : days;
        List<DispatchRecord> rows = recordMapper.selectRecent(lookback);
        DispatchStatVO stat = new DispatchStatVO();
        stat.setTotal((long) rows.size());
        long auto = 0, manual = 0, reassign = 0;
        double scoreSum = 0.0;
        int scoreCount = 0;
        for (DispatchRecord r : rows) {
            String s = r.getStrategy() == null ? "" : r.getStrategy();
            switch (s) {
                case "AUTO" -> auto++;
                case "MANUAL" -> manual++;
                case "REASSIGN" -> reassign++;
                default -> {}
            }
            if (r.getScore() != null) {
                scoreSum += r.getScore().doubleValue();
                scoreCount++;
            }
        }
        stat.setAutoCount(auto);
        stat.setManualCount(manual);
        stat.setReassignCount(reassign);
        stat.setAvgScore(scoreCount == 0 ? 0.0 : Math.round((scoreSum / scoreCount) * 100.0) / 100.0);
        stat.setDate(LocalDate.now());
        return stat;
    }

    /* ========================= internals ========================= */

    /**
     * Load assignable installers from durable profile/account state. Redis is a
     * presence accelerator and may be empty after restart; using it as the only
     * source made both manual dispatch and reassignment show an empty selector
     * even while on-duty installers existed in MySQL.
     */
    private List<InstallerDTO> loadCandidates() {
        List<InstallerVO> available = installerProfileService.listAvailable(props.getMaxLoad());

        // Phase 7: pre-fetch installers who are currently on approved leave, so we
        // can exclude them from dispatch candidates.
        Set<Long> onLeave;
        try {
            List<LeaveRequest> leaves = leaveService.findAllActiveLeaves(java.time.LocalDateTime.now());
            onLeave = leaves.stream()
                    .map(LeaveRequest::getApplicantId)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            log.warn("loadCandidates: failed to query active leaves, proceeding without leave filter", ex);
            onLeave = Collections.emptySet();
        }

        List<InstallerDTO> result = new ArrayList<>();
        for (InstallerVO v : available) {
            if (v.getUserId() == null) continue;
            if (onLeave.contains(v.getUserId())) continue;
            result.add(toInstallerDTO(v));
        }
        return result;
    }

    private InstallerDTO toInstallerDTO(InstallerVO v) {
        String tags = null;
        if (v != null && v.getUserId() != null) {
            try {
                com.bbpms.user.entity.InstallerProfile profile =
                        installerProfileService.getByUserId(v.getUserId());
                if (profile != null) tags = profile.getSkillTags();
            } catch (Exception ignored) {
                // tolerate service being unavailable
            }
        }
        List<String> skills = StringUtils.hasText(tags)
                ? List.of(tags.split(","))
                : List.of();
        return new InstallerDTO(
                v.getUserId(),
                v.getRealName() != null ? v.getRealName() : v.getUsername(),
                v.getLat(),
                v.getLng(),
                v.getWorkload() == null ? 0 : v.getWorkload(),
                props.getMaxLoad(),
                v.getRating() == null ? 0.0 : v.getRating().doubleValue(),
                1,
                skills,
                List.of(),
                v.getOnDuty(),
                v.getUsername(),
                v.getPhone()
        );
    }

    private OrderDTO toOrderDTO(BroadbandOrder order) {
        return new OrderDTO(
                order.getId(),
                order.getOrderNo(),
                readBigDecimal(order, "lat"),
                readBigDecimal(order, "lng"),
                readString(order, "gridCode"),
                order.getPackageCode(),
                readStringList(order, "requiredSkills")
        );
    }

    /** Resolve the source orderId by reading the work-order detail. */
    private Long resolveOrderId(Long workOrderId) {
        try {
            WorkOrderDetailVO d = workOrderService.getDetail(workOrderId);
            return d == null ? null : d.getOrderId();
        } catch (Exception e) {
            return null;
        }
    }

    /* ---------- reflective field probes (tolerant of schema gaps) ---------- */

    private static String readString(Object target, String field) {
        if (target == null) return null;
        try {
            Method g = target.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
            Object v = g.invoke(target);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal readBigDecimal(Object target, String field) {
        String s = readString(target, field);
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (Exception e) { return null; }
    }

    private static List<String> readStringList(Object target, String field) {
        Object v;
        try {
            Method g = target.getClass().getMethod("get" + Character.toUpperCase(field.charAt(0)) + field.substring(1));
            v = g.invoke(target);
        } catch (Exception e) { return List.of(); }
        if (v instanceof List<?> l) {
            List<String> out = new ArrayList<>();
            for (Object o : l) if (o != null) out.add(o.toString());
            return out;
        }
        if (v instanceof String s && StringUtils.hasText(s)) {
            return List.of(s.split(","));
        }
        return List.of();
    }

}
