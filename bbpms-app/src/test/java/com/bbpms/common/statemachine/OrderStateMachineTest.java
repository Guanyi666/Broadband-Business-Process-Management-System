package com.bbpms.common.statemachine;

import com.bbpms.common.enums.OrderEvent;
import com.bbpms.common.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStateMachineTest {

    private final OrderStateMachine stateMachine = new OrderStateMachine();

    @Test
    void rejectedOrderCanBeResubmittedToCreated() {
        assertEquals(OrderStatus.CREATED,
                stateMachine.next(OrderStatus.REJECTED, OrderEvent.RESUBMIT));
    }

    @Test
    void createdAndRejectedOrdersCanBeCancelled() {
        assertEquals(OrderStatus.CANCELLED,
                stateMachine.next(OrderStatus.CREATED, OrderEvent.CANCEL));
        assertEquals(OrderStatus.CANCELLED,
                stateMachine.next(OrderStatus.REJECTED, OrderEvent.CANCEL));
    }

    @Test
    void auditedOrderCannotUseTheEarlyStageCancelAction() {
        assertThrows(RuntimeException.class,
                () -> stateMachine.next(OrderStatus.AUDITED, OrderEvent.CANCEL));
    }
}
