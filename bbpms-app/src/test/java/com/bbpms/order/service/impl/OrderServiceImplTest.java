package com.bbpms.order.service.impl;

import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.statemachine.OrderStateMachine;
import com.bbpms.common.util.SnowflakeIdGenerator;
import com.bbpms.order.config.OrderProperties;
import com.bbpms.order.dto.OrderCancelReq;
import com.bbpms.order.entity.BroadbandOrder;
import com.bbpms.order.entity.OrderAuditLog;
import com.bbpms.order.mapper.AppointmentMapper;
import com.bbpms.order.mapper.BroadbandOrderMapper;
import com.bbpms.order.mapper.CustomerMapper;
import com.bbpms.order.mapper.OrderAuditLogMapper;
import com.bbpms.order.service.CustomerService;
import com.bbpms.order.service.OrderTimelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private BroadbandOrderMapper orderMapper;
    @Mock private OrderAuditLogMapper auditLogMapper;
    @Mock private AppointmentMapper appointmentMapper;
    @Mock private CustomerMapper customerMapper;
    @Mock private CustomerService customerService;
    @Mock private ApplicationEventPublisher publisher;
    @Mock private OrderTimelineService orderTimelineService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orderMapper, auditLogMapper, appointmentMapper, customerMapper,
                customerService, new OrderProperties(), new OrderStateMachine(), publisher,
                new SnowflakeIdGenerator(1, 2), orderTimelineService);
    }

    @Test
    void rejectedOrderResubmitUsesAtomicResetAndAddsAuditLog() {
        BroadbandOrder order = order(20L, OrderStatus.REJECTED);
        when(orderMapper.selectById(20L)).thenReturn(order);
        when(orderMapper.resubmitRejected(20L, "REJECTED", "CREATED", 2L)).thenReturn(1);

        service.resubmit(20L, 2L);

        verify(orderMapper).resubmitRejected(20L, "REJECTED", "CREATED", 2L);
        ArgumentCaptor<OrderAuditLog> log = ArgumentCaptor.forClass(OrderAuditLog.class);
        verify(auditLogMapper).insert(log.capture());
        assertEquals("REJECTED", log.getValue().getFromStatus());
        assertEquals("CREATED", log.getValue().getToStatus());
    }

    @Test
    void cancelPersistsCancelledStatusAndPublishesEvent() {
        BroadbandOrder order = order(20L, OrderStatus.CREATED);
        when(orderMapper.selectById(20L)).thenReturn(order);
        when(orderMapper.updateById(any(BroadbandOrder.class))).thenReturn(1);
        OrderCancelReq request = new OrderCancelReq();
        request.setReason("客户撤销");

        service.cancel(20L, request, 2L);

        ArgumentCaptor<BroadbandOrder> update = ArgumentCaptor.forClass(BroadbandOrder.class);
        verify(orderMapper).updateById(update.capture());
        assertEquals("CANCELLED", update.getValue().getStatus());
        verify(auditLogMapper).insert(any(OrderAuditLog.class));
        verify(publisher).publishEvent(any(BbpmsEvents.OrderCancelledEvent.class));
    }

    private static BroadbandOrder order(Long id, OrderStatus status) {
        BroadbandOrder order = new BroadbandOrder();
        order.setId(id);
        order.setOrderNo("BBDEMO20260001");
        order.setStatus(status.name());
        order.setVersion(0);
        return order;
    }
}
