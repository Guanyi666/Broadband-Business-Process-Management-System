package com.bbpms.workorder;

import com.bbpms.workorder.config.WorkOrderSlaProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity tests for the SLA config binding defaults.
 * The actual config-property wiring is exercised by Spring's binding,
 * which we don't run here.
 */
class WorkOrderSlaPropertiesTest {

    @Test
    void defaultValuesAreSane() {
        WorkOrderSlaProperties p = new WorkOrderSlaProperties();
        assertNotNull(p);
        assertEquals(Long.valueOf(30_000L), p.getScanIntervalMs());
        assertEquals(Integer.valueOf(30), p.getAcceptTimeoutMinutes());
        assertEquals(Integer.valueOf(4), p.getProgressHeartbeatTimeoutHours());
        assertEquals(Integer.valueOf(24), p.getStalledRecoverHours());
        assertTrue(p.getEnabled());
    }

    @Test
    void canOverrideAllValues() {
        WorkOrderSlaProperties p = new WorkOrderSlaProperties();
        p.setScanIntervalMs(60_000L);
        p.setAcceptTimeoutMinutes(15);
        p.setProgressHeartbeatTimeoutHours(2);
        p.setStalledRecoverHours(8);
        p.setEnabled(false);
        assertEquals(Long.valueOf(60_000L), p.getScanIntervalMs());
        assertEquals(Integer.valueOf(15), p.getAcceptTimeoutMinutes());
        assertEquals(Integer.valueOf(2), p.getProgressHeartbeatTimeoutHours());
        assertEquals(Integer.valueOf(8), p.getStalledRecoverHours());
        assertEquals(Boolean.FALSE, p.getEnabled());
    }
}
