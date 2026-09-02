package com.bbpms.leave.util;

import com.bbpms.leave.config.LeaveProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LeaveEscalationRule}.
 *
 * <p>Covers the six documented cases plus a couple of edge cases.
 * Pure JUnit 5, no Spring context.</p>
 */
class LeaveEscalationRuleTest {

    private LeaveProperties props(double thresholdHours) {
        LeaveProperties p = new LeaveProperties();
        p.setEscalationThresholdHours(thresholdHours);
        return p;
    }

    @Test
    void casual8h_isSingleLevel() {
        assertEquals(1, LeaveEscalationRule.requiredLevel("CASUAL", 8.0, props(16.0)));
    }

    @Test
    void casual16h_isEscalatedToTwoLevels() {
        assertEquals(2, LeaveEscalationRule.requiredLevel("CASUAL", 16.0, props(16.0)));
    }

    @Test
    void sickAny_isEscalatedToTwoLevels() {
        assertEquals(2, LeaveEscalationRule.requiredLevel("SICK", 4.0, props(16.0)));
    }

    @Test
    void compassionateAny_isEscalatedToTwoLevels() {
        assertEquals(2, LeaveEscalationRule.requiredLevel("COMPASSIONATE", 2.0, props(16.0)));
    }

    @Test
    void annual40h_isEscalatedToTwoLevels() {
        assertEquals(2, LeaveEscalationRule.requiredLevel("ANNUAL", 40.0, props(16.0)));
    }

    @Test
    void unpaid1h_isSingleLevel() {
        assertEquals(1, LeaveEscalationRule.requiredLevel("UNPAID", 1.0, props(16.0)));
    }

    @Test
    void nullType_isSingleLevel() {
        assertEquals(1, LeaveEscalationRule.requiredLevel(null, 100.0, props(16.0)));
    }

    @Test
    void lowerCaseSick_isEscalated() {
        // Case-insensitive normalisation
        assertEquals(2, LeaveEscalationRule.requiredLevel("sick", 4.0, props(16.0)));
    }

    @Test
    void justUnderThreshold_isSingleLevel() {
        assertEquals(1, LeaveEscalationRule.requiredLevel("ANNUAL", 15.99, props(16.0)));
    }

    @Test
    void customThresholdIsHonoured() {
        // 8h threshold — 10h is escalated, and 8h is escalated too because the
        // rule uses >= (matches LeaveEscalationRule javadoc "total hours >= threshold").
        assertEquals(2, LeaveEscalationRule.requiredLevel("CASUAL", 8.0, props(8.0)));
        assertEquals(2, LeaveEscalationRule.requiredLevel("CASUAL", 10.0, props(8.0)));
        // just under the threshold stays single-level
        assertEquals(1, LeaveEscalationRule.requiredLevel("CASUAL", 7.99, props(8.0)));
    }
}
