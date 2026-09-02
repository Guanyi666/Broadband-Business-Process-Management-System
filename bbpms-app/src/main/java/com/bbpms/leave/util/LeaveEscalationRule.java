package com.bbpms.leave.util;

import com.bbpms.leave.config.LeaveProperties;

/**
 * Single source of truth for the leave escalation rule.
 *
 * <p>Default: single-level approval.
 * Auto-escalates to 2 levels when:
 * <ul>
 *   <li>leave type is SICK or COMPASSIONATE, OR</li>
 *   <li>total hours &gt;= {@link LeaveProperties#getEscalationThresholdHours()}</li>
 * </ul>
 */
public final class LeaveEscalationRule {

    private LeaveEscalationRule() {}

    public static int requiredLevel(String leaveType, double totalHours, LeaveProperties props) {
        if (leaveType == null) return 1;
        String t = leaveType.toUpperCase();
        if ("SICK".equals(t) || "COMPASSIONATE".equals(t)) return 2;
        double threshold = props == null || props.getEscalationThresholdHours() == null
                ? 16.0 : props.getEscalationThresholdHours();
        return totalHours >= threshold ? 2 : 1;
    }
}
