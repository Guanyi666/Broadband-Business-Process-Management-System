package com.bbpms.order.service;

import com.bbpms.order.dto.OrderTimelineVO;

import java.util.List;

/**
 * Aggregates the audit trail of an order from two sources:
 *
 * <ol>
 *   <li>{@code order_audit_log} (local — inserted by OrderService);</li>
 *   <li>{@code work_order_timeline} (queried via direct local injection of
 *       {@code WorkOrderTimelineService} — no Feign in the monolith).</li>
 * </ol>
 */
public interface OrderTimelineService {

    /**
     * @return chronologically-merged list of timeline events.
     */
    List<OrderTimelineVO> getTimeline(Long orderId);
}
