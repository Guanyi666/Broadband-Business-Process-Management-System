package com.bbpms.order.service;

import com.bbpms.common.dto.OrderSummaryDTO;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.result.PageResp;
import com.bbpms.order.dto.OrderAuditReq;
import com.bbpms.order.dto.OrderCancelReq;
import com.bbpms.order.dto.OrderCreateReq;
import com.bbpms.order.dto.OrderDetailVO;
import com.bbpms.order.dto.OrderQueryReq;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.vo.OrderVO;

/**
 * Order lifecycle service. Every write that crosses tables runs in a
 * Spring {@code @Transactional} so the audit log + order update are
 * atomic — there is no distributed coordinator in the monolith.
 *
 * <p>Outbound coupling to other modules is done via Spring
 * {@code ApplicationEventPublisher} (replacing RabbitMQ) and direct
 * {@code @Autowired} of same-process services (replacing Feign).</p>
 */
public interface OrderService {

    /**
     * Create a new order in {@code CREATED} status. Sensitive customer
     * fields are SM4-encrypted; a {@code CREATE} audit row is appended; an
     * {@link com.bbpms.common.event.BbpmsEvents.OrderCreatedEvent} is
     * published.
     *
     * @return the new order's primary key
     */
    Long create(OrderCreateReq req, Long csUserId);

    /**
     * Audit an order.
     *
     * @param req           audit decision (pass=true moves CREATED -> AUDITED,
     *                      pass=false moves CREATED -> CANCELLED)
     * @param auditorId     id of the auditor
     */
    void audit(Long orderId, OrderAuditReq req, Long auditorId);

    /**
     * Cancel an order. Allowed from CREATED or AUDITED.
     */
    void cancel(Long orderId, OrderCancelReq req, Long operatorId);

    /**
     * Get the full detail composite (order + customer + appointment + timeline).
     */
    OrderDetailVO getDetail(Long orderId);

    /**
     * Page query with row-level {@link com.bbpms.common.annotation.DataScope} applied.
     */
    PageResp<OrderVO> page(OrderQueryReq req);

    /**
     * Find by business order number (UK).
     */
    BroadbandOrder findByOrderNo(String orderNo);

    /**
     * Direct internal API: flip the persisted status only (used by
     * other modules in the monolith — e.g. workorder calls
     * {@code updateStatus(orderId, DISPATCHED)} after auto-dispatch).
     */
    void updateStatus(Long orderId, OrderStatus status, Long operatorId);

    /**
     * Find by primary key. Throws BizException(NOT_FOUND) if missing.
     */
    BroadbandOrder findByOrderId(Long orderId);

    /**
     * Lightweight cross-module snapshot used by the workorder module to
     * denormalise address / phone / package onto the work order row.
     */
    OrderSummaryDTO snapshot(Long orderId);

    /**
     * Mark an order as {@code FINISHED}. Called by the install module when
     * the installer reports complete and BSS activation succeeded. Validates
     * that the order is currently {@code DISPATCHED} or {@code INSTALLING}
     * (BSS-success flow).
     *
     * @throws com.bbpms.common.exception.BizException if the order is not
     *         in a state that allows transitioning to FINISHED.
     */
    void markFinished(Long orderId, Long operatorId);
}