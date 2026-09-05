package com.bbpms.order.service.impl;

import com.bbpms.order.dto.OrderTimelineVO;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.order.service.OrderTimelineService;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.user.entity.SysUser;
import com.bbpms.user.mapper.SysUserMapper;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Aggregates order_audit_log (local) with work_order_timeline (local —
 * same-process injection of {@link WorkOrderTimelineService}).
 *
 * <p>If workorder module is unavailable the local timeline is still returned
 * (workorder is on the same DB and same transaction context, so this only
 * fails when the row really doesn't exist).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTimelineServiceImpl implements OrderTimelineService {

    private final OrderAuditLogMapper auditLogMapper;
    private final WorkOrderTimelineService workOrderTimelineService;
    private final SysUserMapper userMapper;

    @Override
    public List<OrderTimelineVO> getTimeline(Long orderId) {
        List<OrderTimelineVO> all = new ArrayList<>();

        // 1. Local audit log (order_audit_log)
        for (OrderAuditLog l : auditLogMapper.selectByOrderId(orderId)) {
            OrderTimelineVO v = new OrderTimelineVO();
            v.setEventTime(l.getCreateTime());
            v.setSource("ORDER_AUDIT");
            v.setEventType(orderEventType(l.getFromStatus(), l.getToStatus()));
            v.setFromStatus(l.getFromStatus());
            v.setFromStatusDesc(orderStatusDesc(l.getFromStatus()));
            v.setToStatus(l.getToStatus());
            v.setToStatusDesc(orderStatusDesc(l.getToStatus()));
            v.setDescription(transitionDescription(v.getFromStatusDesc(), v.getToStatusDesc()));
            v.setOperatorId(l.getAuditorId());
            v.setOperatorRole(l.getAuditorId() == null ? "SYSTEM" : "ORDER_OPERATOR");
            v.setRemark(l.getRemark());
            all.add(v);
        }

        // 2. Work-order timeline rows joined via the local workorder module.
        try {
            // For a single order there is typically one work order — the
            // service looks up by orderId and returns ordered rows.
            List<WorkOrderTimelineVO> rows = workOrderTimelineService.listByOrderId(orderId);
            for (WorkOrderTimelineVO w : rows) {
                OrderTimelineVO v = new OrderTimelineVO();
                v.setEventTime(w.getCreateTime());
                v.setSource("WORKORDER");
                v.setEventType(w.getToStatus() == null ? null : w.getToStatus().name());
                v.setFromStatus(w.getFromStatus() == null ? null : w.getFromStatus().name());
                v.setFromStatusDesc(w.getFromStatusDesc());
                v.setToStatus(w.getToStatus() == null ? null : w.getToStatus().name());
                v.setToStatusDesc(w.getToStatusDesc());
                v.setDescription(transitionDescription(w.getFromStatusDesc(), w.getToStatusDesc()));
                v.setOperatorId(w.getOperatorId());
                v.setOperatorName(w.getOperatorName());
                v.setOperatorRole(w.getOperatorRole());
                v.setRemark(w.getRemark());
                all.add(v);
            }
        } catch (Exception ex) {
            log.warn("Workorder timeline join failed for order {}: {}", orderId, ex.getMessage());
        }

        List<OrderTimelineVO> sorted = all.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OrderTimelineVO::getEventTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        enrichOperatorNames(sorted);
        return sorted;
    }

    private void enrichOperatorNames(List<OrderTimelineVO> events) {
        List<Long> operatorIds = events.stream()
                .filter(event -> event.getOperatorName() == null || event.getOperatorName().isBlank())
                .map(OrderTimelineVO::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (operatorIds.isEmpty()) return;

        try {
            Map<Long, SysUser> users = userMapper.selectBatchIds(operatorIds).stream()
                    .collect(Collectors.toMap(SysUser::getId, Function.identity(), (left, right) -> left));
            for (OrderTimelineVO event : events) {
                SysUser user = users.get(event.getOperatorId());
                if (user != null && (event.getOperatorName() == null || event.getOperatorName().isBlank())) {
                    event.setOperatorName(user.getRealName() == null || user.getRealName().isBlank()
                            ? user.getUsername() : user.getRealName());
                }
            }
        } catch (Exception ex) {
            log.warn("Unable to enrich order timeline operator names: {}", ex.getMessage());
        }
    }

    private String orderEventType(String fromStatus, String toStatus) {
        if (fromStatus == null && OrderStatus.CREATED.name().equals(toStatus)) return "CREATE";
        if (OrderStatus.REJECTED.name().equals(toStatus)) return "AUDIT_REJECT";
        if (OrderStatus.REJECTED.name().equals(fromStatus) && OrderStatus.CREATED.name().equals(toStatus)) return "RESUBMIT";
        if (OrderStatus.CANCELLED.name().equals(toStatus)) return "CANCEL";
        if (OrderStatus.AUDITED.name().equals(toStatus)) return "AUDIT_PASS";
        return toStatus == null ? "ORDER_EVENT" : toStatus;
    }

    private String orderStatusDesc(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            OrderStatus value = OrderStatus.valueOf(status);
            return value.getDesc() == null || value.getDesc().isBlank() ? value.name() : value.getDesc();
        } catch (IllegalArgumentException ex) {
            return status;
        }
    }

    private String transitionDescription(String from, String to) {
        if (from == null || from.isBlank()) return to == null ? "状态更新" : to;
        if (to == null || to.isBlank()) return from;
        return from + " → " + to;
    }
}
