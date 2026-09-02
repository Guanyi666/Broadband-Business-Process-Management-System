package com.bbpms.common.statemachine;

import com.bbpms.common.enums.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Phase-5 work-order state machine transition table.
 *
 * <p>Each test maps one legal or illegal transition. Pure JUnit 5.</p>
 */
class WorkOrderStateMachineTest {

    private final WorkOrderStateMachine sm = new WorkOrderStateMachine();

    /* ---------- legal transitions from the original Phase-0 set ---------- */

    @Test
    void pendingToDispatched() {
        assertEquals(WorkOrderStatus.DISPATCHED,
                sm.next(WorkOrderStatus.PENDING, WorkOrderStatus.DISPATCHED));
    }

    @Test
    void dispatchedToAccepted() {
        assertEquals(WorkOrderStatus.ACCEPTED,
                sm.next(WorkOrderStatus.DISPATCHED, WorkOrderStatus.ACCEPTED));
    }

    @Test
    void inProgressToCompleted() {
        assertEquals(WorkOrderStatus.COMPLETED,
                sm.next(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED));
    }

    @Test
    void inProgressToFailed() {
        assertEquals(WorkOrderStatus.FAILED,
                sm.next(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.FAILED));
    }

    /* ---------- Phase 5 additions: legal ---------- */

    @Test
    void dispatchedToStalled() {
        assertEquals(WorkOrderStatus.STALLED,
                sm.next(WorkOrderStatus.DISPATCHED, WorkOrderStatus.STALLED));
    }

    @Test
    void dispatchedToAutoCancelled() {
        assertEquals(WorkOrderStatus.AUTO_CANCELLED,
                sm.next(WorkOrderStatus.DISPATCHED, WorkOrderStatus.AUTO_CANCELLED));
    }

    @Test
    void dispatchedToReassigning() {
        assertEquals(WorkOrderStatus.REASSIGNING,
                sm.next(WorkOrderStatus.DISPATCHED, WorkOrderStatus.REASSIGNING));
    }

    @Test
    void inProgressToStalled() {
        assertEquals(WorkOrderStatus.STALLED,
                sm.next(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.STALLED));
    }

    @Test
    void stalledToInProgress() {
        assertEquals(WorkOrderStatus.IN_PROGRESS,
                sm.next(WorkOrderStatus.STALLED, WorkOrderStatus.IN_PROGRESS));
    }

    @Test
    void stalledToReassigning() {
        assertEquals(WorkOrderStatus.REASSIGNING,
                sm.next(WorkOrderStatus.STALLED, WorkOrderStatus.REASSIGNING));
    }

    @Test
    void stalledToAutoCancelled() {
        assertEquals(WorkOrderStatus.AUTO_CANCELLED,
                sm.next(WorkOrderStatus.STALLED, WorkOrderStatus.AUTO_CANCELLED));
    }

    @Test
    void reassigningToDispatched() {
        assertEquals(WorkOrderStatus.DISPATCHED,
                sm.next(WorkOrderStatus.REASSIGNING, WorkOrderStatus.DISPATCHED));
    }

    @Test
    void inProgressToAutoCancelled() {
        assertEquals(WorkOrderStatus.AUTO_CANCELLED,
                sm.next(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.AUTO_CANCELLED));
    }

    /* ---------- illegal transitions ---------- */

    @Test
    void pendingToCompleted_isRejected() {
        assertThrows(RuntimeException.class,
                () -> sm.next(WorkOrderStatus.PENDING, WorkOrderStatus.COMPLETED));
    }

    @Test
    void dispatchedToCompleted_isRejected() {
        assertThrows(RuntimeException.class,
                () -> sm.next(WorkOrderStatus.DISPATCHED, WorkOrderStatus.COMPLETED));
    }

    @Test
    void completedToAnything_isRejected() {
        // Terminal state: nothing can come out.
        for (WorkOrderStatus to : WorkOrderStatus.values()) {
            if (to == WorkOrderStatus.COMPLETED) continue;
            assertThrows(RuntimeException.class,
                    () -> sm.next(WorkOrderStatus.COMPLETED, to),
                    "COMPLETED should not transition to " + to);
        }
    }

    @Test
    void cancelledToAnything_isRejected() {
        for (WorkOrderStatus to : WorkOrderStatus.values()) {
            if (to == WorkOrderStatus.CANCELLED) continue;
            assertThrows(RuntimeException.class,
                    () -> sm.next(WorkOrderStatus.CANCELLED, to),
                    "CANCELLED should not transition to " + to);
        }
    }

    @Test
    void autoCancelledToAnything_isRejected() {
        for (WorkOrderStatus to : WorkOrderStatus.values()) {
            if (to == WorkOrderStatus.AUTO_CANCELLED) continue;
            assertThrows(RuntimeException.class,
                    () -> sm.next(WorkOrderStatus.AUTO_CANCELLED, to),
                    "AUTO_CANCELLED should not transition to " + to);
        }
    }

    @Test
    void failedToAnything_isRejected() {
        for (WorkOrderStatus to : WorkOrderStatus.values()) {
            if (to == WorkOrderStatus.FAILED) continue;
            assertThrows(RuntimeException.class,
                    () -> sm.next(WorkOrderStatus.FAILED, to),
                    "FAILED should not transition to " + to);
        }
    }

    /* ---------- system transitions skip role check ---------- */

    @Test
    void systemCanTransition() {
        // assertSystemTransition should not throw for legal transitions even
        // without a SecurityUser.
        assertDoesNotThrow(() ->
                sm.assertSystemTransition(WorkOrderStatus.DISPATCHED, WorkOrderStatus.STALLED));
    }

    @Test
    void systemCannotTransitionToIllegal() {
        assertThrows(RuntimeException.class, () ->
                sm.assertSystemTransition(WorkOrderStatus.DISPATCHED, WorkOrderStatus.COMPLETED));
    }

    /* ---------- terminal predicate ---------- */

    @Test
    void terminalPredicate() {
        assertTrue(WorkOrderStatus.isTerminal(WorkOrderStatus.COMPLETED));
        assertTrue(WorkOrderStatus.isTerminal(WorkOrderStatus.CANCELLED));
        assertTrue(WorkOrderStatus.isTerminal(WorkOrderStatus.AUTO_CANCELLED));
        assertTrue(WorkOrderStatus.isTerminal(WorkOrderStatus.FAILED));
        assertFalse(WorkOrderStatus.isTerminal(WorkOrderStatus.IN_PROGRESS));
        assertFalse(WorkOrderStatus.isTerminal(WorkOrderStatus.STALLED));
        assertFalse(WorkOrderStatus.isTerminal(WorkOrderStatus.DISPATCHED));
    }
}
