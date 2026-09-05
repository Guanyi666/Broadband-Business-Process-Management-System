package com.bbpms.track.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.exception.BizException;
import com.bbpms.dispatch.entity.DispatchRecord;
import com.bbpms.dispatch.mapper.DispatchRecordMapper;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.entity.Customer;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.BroadbandOrderMapper;
import com.bbpms.order.mapper.CustomerMapper;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.track.service.TrackService;
import com.bbpms.track.vo.StageVO;
import com.bbpms.track.vo.TrackEventVO;
import com.bbpms.track.vo.TrackResultVO;
import com.bbpms.track.vo.TrackSummaryVO;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.workorder.entity.WorkOrder;
import com.bbpms.workorder.entity.WorkOrderTimeline;
import com.bbpms.workorder.mapper.WorkOrderMapper;
import com.bbpms.workorder.mapper.WorkOrderTimelineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单 / 工单双轨时间线轨迹合成实现。
 *
 * <p>仅新增文件，不修改任何现有表结构、现有接口或现有 Service。
 * 所有主干时间严格来自订单表 / 工单表真实字段。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackServiceImpl implements TrackService {

    private static final String DONE = "DONE";
    private static final String CURRENT = "CURRENT";
    private static final String PENDING = "PENDING";
    private static final String EXCEPTION = "EXCEPTION";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BroadbandOrderMapper orderMapper;
    private final WorkOrderMapper workOrderMapper;
    private final OrderAuditLogMapper orderAuditLogMapper;
    private final WorkOrderTimelineMapper workOrderTimelineMapper;
    private final DispatchRecordMapper dispatchRecordMapper;
    private final SysUserMapper userMapper;
    private final CustomerMapper customerMapper;

    /* ============================ 订单轨迹 ============================ */

    @Override
    public TrackResultVO getOrderTrack(Long orderId) {
        BroadbandOrder order = orderMapper.selectById(orderId);
        if (order == null) throw new BizException(ResultCode.ORDER_NOT_FOUND, "订单不存在");

        List<WorkOrder> wos = workOrderMapper.selectList(
                new QueryWrapper<WorkOrder>().eq("order_id", orderId));
        WorkOrder mainWO = pickMainWorkOrder(wos);
        DispatchRecord mainDispatch = mainWO != null ? firstDispatch(mainWO.getId()) : null;

        // 人员解析：主表字段优先
        Set<Long> userIds = new HashSet<>();
        addIfNotNull(userIds, order.getAuditorId(), order.getCsId());
        if (mainWO != null) {
            addIfNotNull(userIds, mainWO.getInstallerId(), mainWO.getDispatcherId());
        }
        Map<Long, String> userName = resolveUserNames(userIds);
        Map<Long, String> custName = resolveCustomerNames(List.of(order.getCustomerId()));

        // 8 节点真实时间字段
        LocalDateTime[] times = new LocalDateTime[8];
        times[0] = order.getCreateTime();
        times[1] = order.getAuditTime();
        times[2] = mainWO == null ? null : mainWO.getCreateTime();
        times[3] = order.getDispatchTime() != null ? order.getDispatchTime()
                : (mainWO == null ? null : mainWO.getDispatchTime());
        times[4] = mainWO == null ? null : mainWO.getAcceptTime();
        times[5] = mainWO == null ? null : mainWO.getStartTime();
        times[6] = mainWO == null ? null : mainWO.getFinishTime();
        if (times[6] == null) times[6] = order.getCompletedTime(); // 工单完成时间兜底
        times[7] = OrderStatus.CLOSED.name().equals(order.getStatus()) ? order.getUpdateTime() : null;

        String[] codes = {"CREATED", "AUDITED", "WAIT_DISPATCH", "DISPATCHED",
                "INSTALLING", "INSTALLING", "FINISHED", "CLOSED"};
        String[] names = {"订单创建", "订单审核", "生成工单", "已派单",
                "装维接单", "上门安装", "安装完成", "订单归档"};
        String[] roles = {"客户", "审核人员", "系统", "调度人员",
                "装维人员", "装维人员", "装维人员", "客服人员"};
        String[] ops = new String[8];
        ops[0] = custName.get(order.getCustomerId());
        ops[1] = userName.get(order.getAuditorId());
        ops[2] = null; // SYSTEM
        ops[3] = userName.get(mainWO == null ? null : mainWO.getDispatcherId());
        ops[4] = userName.get(mainWO == null ? null : mainWO.getInstallerId());
        ops[5] = ops[4];
        ops[6] = ops[4];
        ops[7] = userName.get(order.getCsId());
        Boolean[] autos = {false, false, true, isAuto(mainDispatch), false, false, false, false};

        List<StageVO> stages = buildStages(codes, names, roles, ops, autos, times);

        OrderStatus status = OrderStatus.fromCode(order.getStatus());
        boolean terminal = status == OrderStatus.CLOSED || status == OrderStatus.CANCELLED;
        boolean exception = status == OrderStatus.REJECTED || status == OrderStatus.CANCELLED;
        int exceptionIndex = Math.min(frontierIndex(times) + 1, times.length);
        int currentIndex = exception ? exceptionIndex : (status == OrderStatus.CLOSED ? 8 : orderCurrentIndex(status, mainWO));
        assignStates(stages, times, currentIndex, terminal, exception, exceptionIndex);

        List<TrackEventVO> events = buildOrderEvents(order, wos, userName);
        TrackSummaryVO summary = buildOrderSummary(order, status, currentIndex, exception, terminal, times);

        TrackResultVO result = new TrackResultVO();
        result.setStages(stages);
        result.setEvents(events);
        result.setSummary(summary);
        return result;
    }

    private int orderCurrentIndex(OrderStatus status, WorkOrder mainWO) {
        if (status == null) return 1;
        switch (status) {
            case CREATED:      return 2;
            case AUDITED:      return 3;
            case WAIT_DISPATCH: return 4;
            case DISPATCHED:    return 4;
            case INSTALLING:   return (mainWO != null && mainWO.getStartTime() != null) ? 6 : 5;
            case FINISHED:     return 7;
            case CLOSED:       return 8;
            default:           return 1;
        }
    }

    private TrackSummaryVO buildOrderSummary(BroadbandOrder order, OrderStatus status,
                                             int currentIndex, boolean exception, boolean terminal,
                                             LocalDateTime[] times) {
        TrackSummaryVO s = new TrackSummaryVO();
        s.setCurrentStatus(order.getStatus());
        s.setCurrentStatusDesc(status == null ? order.getStatus() : status.getDesc());
        int stageIndex = exception ? frontierIndex(times) : currentIndex;
        s.setCurrentStageIndex(stageIndex);
        s.setProgress(stageIndex + "/8");
        s.setIsTerminal(terminal);

        LocalDateTime end = LocalDateTime.now();
        if (order.getCancelledTime() != null) end = order.getCancelledTime();
        else if (status == OrderStatus.CLOSED && order.getUpdateTime() != null) end = order.getUpdateTime();
        else if (status == OrderStatus.FINISHED && order.getCompletedTime() != null) end = order.getCompletedTime();
        s.setElapsed(order.getCreateTime() == null ? null
                : formatDuration(Duration.between(order.getCreateTime(), end).abs()));

        LocalDateTime last = lastNonNull(times);
        s.setWaiting(terminal || last == null ? null
                : formatDuration(Duration.between(last, LocalDateTime.now()).abs()));
        return s;
    }

    private List<TrackEventVO> buildOrderEvents(BroadbandOrder order, List<WorkOrder> wos,
                                                Map<Long, String> userName) {
        List<TrackEventVO> evs = new ArrayList<>();

        for (OrderAuditLog l : safe(orderAuditLogMapper.selectByOrderId(order.getId()))) {
            TrackEventVO e = new TrackEventVO();
            e.setTime(fmt(l.getCreateTime()));
            e.setTitle(orderEventTitle(l.getToStatus()));
            e.setDesc(transitionDesc(orderStatusDesc(l.getFromStatus()), orderStatusDesc(l.getToStatus())));
            e.setOperatorName(userName.get(l.getAuditorId()));
            e.setOperatorRole(l.getAuditorId() == null ? "系统" : "审核人员");
            e.setSource("ORDER_AUDIT");
            e.setAuto(false);
            e.setRemark(l.getRemark());
            evs.add(e);
        }

        for (WorkOrderTimeline l : safe(workOrderTimelineMapper.selectByOrderId(order.getId()))) {
            TrackEventVO e = new TrackEventVO();
            e.setTime(fmt(l.getCreateTime()));
            e.setTitle(woStatusDesc(l.getToStatus()));
            e.setDesc(transitionDesc(woStatusDesc(l.getFromStatus()), woStatusDesc(l.getToStatus())));
            e.setOperatorName(userName.get(l.getOperatorId()));
            e.setOperatorRole(l.getOperatorRole());
            e.setSource("WORKORDER");
            e.setAuto("SYSTEM".equals(l.getOperatorRole()));
            e.setRemark(l.getRemark());
            evs.add(e);
        }

        for (WorkOrder wo : safe(wos)) {
            for (DispatchRecord r : safe(dispatchRecordMapper.selectByWorkOrderId(wo.getId()))) {
                TrackEventVO e = new TrackEventVO();
                LocalDateTime t = r.getCreateTime() != null ? r.getCreateTime() : wo.getDispatchTime();
                e.setTime(fmt(t));
                boolean auto = "AUTO".equals(r.getStrategy());
                e.setTitle(auto ? "自动派单" : "人工派单");
                e.setDesc(r.getReason() != null ? r.getReason() : (auto ? "系统自动派单" : "调度人员手动派单"));
                e.setOperatorName(userName.get(r.getInstallerId()));
                e.setOperatorRole(auto ? "系统" : "调度人员");
                e.setSource("DISPATCH");
                e.setAuto(auto);
                e.setRemark(r.getReason());
                evs.add(e);
            }
        }

        // 取消时间取自订单表真实字段。订单日志表可能没有对应记录，
        // 但订单确实在那一刻被取消，属于真实事件，需要体现在操作轨上。
        if (order.getCancelledTime() != null) {
            TrackEventVO e = new TrackEventVO();
            e.setTime(fmt(order.getCancelledTime()));
            e.setTitle("订单取消");
            e.setDesc(order.getCancelReason() != null ? order.getCancelReason() : "订单已取消");
            e.setOperatorName(userName.get(order.getCsId()));
            e.setOperatorRole("客服人员");
            e.setSource("ORDER_AUDIT");
            e.setAuto(false);
            e.setRemark(order.getCancelReason());
            evs.add(e);
        }

        evs.removeIf(e -> e.getTime() == null);
        evs.sort(Comparator.comparing(TrackEventVO::getTime));
        return evs;
    }

    /* ============================ 工单轨迹 ============================ */

    @Override
    public TrackResultVO getWorkOrderTrack(Long workOrderId) {
        WorkOrder wo = workOrderMapper.selectById(workOrderId);
        if (wo == null) throw new BizException(ResultCode.WORKORDER_NOT_FOUND, "工单不存在");

        DispatchRecord dr = firstDispatch(wo.getId());

        Set<Long> userIds = new HashSet<>();
        addIfNotNull(userIds, wo.getInstallerId(), wo.getDispatcherId());
        Map<Long, String> userName = resolveUserNames(userIds);

        LocalDateTime[] times = new LocalDateTime[5];
        times[0] = wo.getCreateTime();
        times[1] = wo.getDispatchTime();
        times[2] = wo.getAcceptTime();
        times[3] = wo.getStartTime();
        times[4] = wo.getFinishTime();

        String[] codes = {"PENDING", "DISPATCHED", "ACCEPTED", "IN_PROGRESS", "COMPLETED"};
        String[] names = {"工单创建", "已派单", "已接单", "施工中", "已完成"};
        String[] roles = {"系统", "调度人员", "装维人员", "装维人员", "装维人员"};
        String[] ops = new String[5];
        ops[0] = null; // SYSTEM
        ops[1] = userName.get(wo.getDispatcherId());
        ops[2] = userName.get(wo.getInstallerId());
        ops[3] = ops[2];
        ops[4] = ops[2];
        Boolean[] autos = {true, isAuto(dr), false, false, false};

        List<StageVO> stages = buildStages(codes, names, roles, ops, autos, times);

        WorkOrderStatus status = wo.getStatusEnum();
        boolean terminal = WorkOrderStatus.isTerminal(status);
        boolean exception = status == WorkOrderStatus.STALLED
                || status == WorkOrderStatus.REASSIGNING
                || status == WorkOrderStatus.FAILED
                || status == WorkOrderStatus.CANCELLED
                || status == WorkOrderStatus.AUTO_CANCELLED;
        int exceptionIndex = Math.min(frontierIndex(times) + 1, times.length);
        int currentIndex = exception ? exceptionIndex : workOrderCurrentIndex(status);
        assignStates(stages, times, currentIndex, terminal, exception, exceptionIndex);

        List<TrackEventVO> events = buildWorkOrderEvents(wo, userName);
        TrackSummaryVO summary = buildWorkOrderSummary(wo, status, currentIndex, exception, terminal, times);

        TrackResultVO result = new TrackResultVO();
        result.setStages(stages);
        result.setEvents(events);
        result.setSummary(summary);
        return result;
    }

    private int workOrderCurrentIndex(WorkOrderStatus status) {
        if (status == null) return 1;
        switch (status) {
            case PENDING:     return 1;
            case DISPATCHED:  return 2;
            case ACCEPTED:    return 3;
            case IN_PROGRESS: return 4;
            case COMPLETED:   return 5;
            default:          return frontierIndex(new LocalDateTime[0]);
        }
    }

    private TrackSummaryVO buildWorkOrderSummary(WorkOrder wo, WorkOrderStatus status,
                                                 int currentIndex, boolean exception, boolean terminal,
                                                 LocalDateTime[] times) {
        TrackSummaryVO s = new TrackSummaryVO();
        s.setCurrentStatus(wo.getStatus());
        s.setCurrentStatusDesc(status == null ? wo.getStatus() : status.getDesc());
        int stageIndex = exception ? frontierIndex(times) : currentIndex;
        s.setCurrentStageIndex(stageIndex);
        s.setProgress(stageIndex + "/5");
        s.setIsTerminal(terminal);

        LocalDateTime end = LocalDateTime.now();
        if (wo.getFinishTime() != null) end = wo.getFinishTime();
        else if (terminal && wo.getUpdateTime() != null) end = wo.getUpdateTime();
        s.setElapsed(wo.getCreateTime() == null ? null
                : formatDuration(Duration.between(wo.getCreateTime(), end).abs()));

        LocalDateTime last = lastNonNull(times);
        s.setWaiting(terminal || last == null ? null
                : formatDuration(Duration.between(last, LocalDateTime.now()).abs()));
        return s;
    }

    private List<TrackEventVO> buildWorkOrderEvents(WorkOrder wo, Map<Long, String> userName) {
        List<TrackEventVO> evs = new ArrayList<>();
        for (WorkOrderTimeline l : safe(workOrderTimelineMapper.selectByWorkOrderId(wo.getId()))) {
            TrackEventVO e = new TrackEventVO();
            e.setTime(fmt(l.getCreateTime()));
            e.setTitle(woStatusDesc(l.getToStatus()));
            e.setDesc(transitionDesc(woStatusDesc(l.getFromStatus()), woStatusDesc(l.getToStatus())));
            e.setOperatorName(userName.get(l.getOperatorId()));
            e.setOperatorRole(l.getOperatorRole());
            e.setSource("WORKORDER");
            e.setAuto("SYSTEM".equals(l.getOperatorRole()));
            e.setRemark(l.getRemark());
            evs.add(e);
        }
        for (DispatchRecord r : safe(dispatchRecordMapper.selectByWorkOrderId(wo.getId()))) {
            TrackEventVO e = new TrackEventVO();
            LocalDateTime t = r.getCreateTime() != null ? r.getCreateTime() : wo.getDispatchTime();
            e.setTime(fmt(t));
            boolean auto = "AUTO".equals(r.getStrategy());
            e.setTitle(auto ? "自动派单" : "人工派单");
            e.setDesc(r.getReason() != null ? r.getReason() : (auto ? "系统自动派单" : "调度人员手动派单"));
            e.setOperatorName(userName.get(r.getInstallerId()));
            e.setOperatorRole(auto ? "系统" : "调度人员");
            e.setSource("DISPATCH");
            e.setAuto(auto);
            e.setRemark(r.getReason());
            evs.add(e);
        }
        evs.removeIf(e -> e.getTime() == null);
        evs.sort(Comparator.comparing(TrackEventVO::getTime));
        return evs;
    }

    /* ============================ 公共工具 ============================ */

    /** 多工单：取「主工单」= 非 CANCELLED/AUTO_CANCELLED 的最新一条；全取消则回退到最新一条。 */
    private WorkOrder pickMainWorkOrder(List<WorkOrder> wos) {
        if (wos == null || wos.isEmpty()) return null;
        WorkOrder main = null;
        for (WorkOrder w : wos) {
            WorkOrderStatus st = w.getStatusEnum();
            if (st == WorkOrderStatus.CANCELLED || st == WorkOrderStatus.AUTO_CANCELLED) continue;
            if (main == null || (w.getCreateTime() != null && w.getCreateTime().isAfter(main.getCreateTime()))) {
                main = w;
            }
        }
        if (main == null) {
            main = wos.stream()
                    .max(Comparator.comparing(WorkOrder::getCreateTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
        }
        return main;
    }

    private DispatchRecord firstDispatch(Long workOrderId) {
        List<DispatchRecord> rs = safe(dispatchRecordMapper.selectByWorkOrderId(workOrderId));
        return rs.isEmpty() ? null : rs.get(0);
    }

    private boolean isAuto(DispatchRecord dr) {
        return dr != null && "AUTO".equals(dr.getStrategy());
    }

    private List<StageVO> buildStages(String[] codes, String[] names, String[] roles,
                                      String[] ops, Boolean[] autos, LocalDateTime[] times) {
        List<StageVO> stages = new ArrayList<>(codes.length);
        for (int i = 0; i < codes.length; i++) {
            StageVO s = new StageVO();
            s.setCode(codes[i]);
            s.setName(names[i]);
            s.setState(PENDING); // 临时值，后续由 assignStates 覆盖
            s.setTime(fmt(times[i]));
            s.setOperatorName(ops[i]);
            s.setOperatorRole(roles[i]);
            s.setAuto(autos[i]);
            stages.add(s);
        }
        return stages;
    }

    private void assignStates(List<StageVO> stages, LocalDateTime[] times,
                              int currentIndex, boolean terminal, boolean exception, int exceptionIndex) {
        for (int i = 0; i < stages.size(); i++) {
            int idx = i + 1;
            StageVO s = stages.get(i);
            if (exception && idx == exceptionIndex) {
                s.setState(EXCEPTION);
            } else if (idx == currentIndex && !terminal) {
                s.setState(CURRENT);
            } else if (idx == currentIndex && terminal) {
                s.setState(DONE);
            } else if (times[i] != null) {
                s.setState(DONE);
            } else {
                s.setState(PENDING);
            }
        }
    }

    /** 最后一个拥有真实时间的节点索引（1-based），全空返回 1。 */
    private int frontierIndex(LocalDateTime[] times) {
        for (int i = times.length - 1; i >= 0; i--) {
            if (times[i] != null) return i + 1;
        }
        return 1;
    }

    private LocalDateTime lastNonNull(LocalDateTime[] times) {
        for (int i = times.length - 1; i >= 0; i--) {
            if (times[i] != null) return times[i];
        }
        return null;
    }

    private Map<Long, String> resolveUserNames(Collection<Long> ids) {
        Map<Long, String> m = new HashMap<>();
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (list.isEmpty()) return m;
        try {
            List<SysUser> users = userMapper.selectBatchIds(list);
            for (SysUser u : users) {
                String n = (u.getNickname() != null && !u.getNickname().isBlank()) ? u.getNickname()
                        : (u.getRealName() != null && !u.getRealName().isBlank()) ? u.getRealName()
                        : u.getUsername();
                m.put(u.getId(), n);
            }
        } catch (Exception ex) {
            log.warn("解析用户姓名失败: {}", ex.getMessage());
        }
        return m;
    }

    private Map<Long, String> resolveCustomerNames(Collection<Long> ids) {
        Map<Long, String> m = new HashMap<>();
        List<Long> list = ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (list.isEmpty()) return m;
        try {
            List<Customer> cs = customerMapper.selectByIds(list);
            for (Customer c : cs) m.put(c.getId(), c.getName());
        } catch (Exception ex) {
            log.warn("解析客户姓名失败: {}", ex.getMessage());
        }
        return m;
    }

    private String orderEventTitle(String toStatus) {
        if (toStatus == null) return "订单事件";
        OrderStatus s = OrderStatus.fromCode(toStatus);
        if (s == null) return toStatus;
        switch (s) {
            case CREATED:    return "创建订单";
            case AUDITED:    return "订单审核通过";
            case REJECTED:   return "订单驳回";
            case CANCELLED:  return "订单取消";
            case DISPATCHED: return "订单派单";
            default:         return s.getDesc();
        }
    }

    private String orderStatusDesc(String status) {
        if (status == null || status.isBlank()) return null;
        OrderStatus s = OrderStatus.fromCode(status);
        return s == null ? status : s.getDesc();
    }

    private String woStatusDesc(String status) {
        if (status == null || status.isBlank()) return null;
        WorkOrderStatus s = WorkOrderStatus.fromCode(status);
        return s == null ? status : s.getDesc();
    }

    private String transitionDesc(String from, String to) {
        if (from == null || from.isBlank()) return to == null ? "状态更新" : to;
        if (to == null || to.isBlank()) return from;
        return from + " → " + to;
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static void addIfNotNull(Set<Long> set, Long... ids) {
        for (Long id : ids) if (id != null) set.add(id);
    }

    private static String fmt(LocalDateTime t) {
        return t == null ? null : t.format(FMT);
    }

    /** 时长中文格式化：X天X小时X分钟 / X小时X分钟 / X分钟 / X秒。 */
    private static String formatDuration(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long mins = d.toMinutesPart();
        long secs = d.toSecondsPart();
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (mins > 0) sb.append(mins).append("分钟");
        if (sb.length() == 0) sb.append(secs).append("秒");
        return sb.toString();
    }
}
