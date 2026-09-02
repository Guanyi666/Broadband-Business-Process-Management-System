package com.bbpms.order.service.impl;

import com.bbpms.order.dto.OrderTimelineVO;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.order.service.OrderTimelineService;
import com.bbpms.workorder.service.WorkOrderTimelineService;
import com.bbpms.workorder.vo.WorkOrderTimelineVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

    @Override
    public List<OrderTimelineVO> getTimeline(Long orderId) {
        List<OrderTimelineVO> all = new ArrayList<>();

        // 1. Local audit log (order_audit_log)
        for (OrderAuditLog l : auditLogMapper.selectByOrderId(orderId)) {
            OrderTimelineVO v = new OrderTimelineVO();
            v.setEventTime(l.getCreateTime());
            v.setSource("ORDER_AUDIT");
            v.setEventType("AUDIT");
            v.setDescription(String.format("%s -> %s%s",
                    l.getFromStatus(), l.getToStatus(),
                    l.getRemark() != null ? " : " + l.getRemark() : ""));
            v.setOperatorId(l.getAuditorId());
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
                v.setDescription(w.getRemark() != null ? w.getRemark() : "");
                v.setOperatorId(w.getOperatorId());
                v.setOperatorName(w.getOperatorName());
                all.add(v);
            }
        } catch (Exception ex) {
            log.warn("Workorder timeline join failed for order {}: {}", orderId, ex.getMessage());
        }

        return all.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OrderTimelineVO::getEventTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }
}
