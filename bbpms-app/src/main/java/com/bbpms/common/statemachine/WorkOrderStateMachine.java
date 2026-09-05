package com.bbpms.common.statemachine;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.enums.WorkOrderStatus;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.security.SecurityUser;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Work-order status state machine with full Phase 5 transitions.
 *
 * <p>Authorisation table (role codes: 2 = CS, 4 = dispatcher, 5 = installer,
 * 6 = customer).  SYSTEM means triggered by the SLA scheduler; SUPER_ADMIN is
 * role 1.</p>
 */
@Component
public class WorkOrderStateMachine {

    private final Map<WorkOrderStatus, Map<WorkOrderStatus, List<Integer>>> table = new EnumMap<>(WorkOrderStatus.class);
    private final Map<WorkOrderStatus, Boolean> finalStates = new HashMap<>();

    public WorkOrderStateMachine() {
        /* Original Phase-0 transitions */
        put(WorkOrderStatus.PENDING,     WorkOrderStatus.DISPATCHED,   List.of(4));
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.ACCEPTED,     List.of(5));
        put(WorkOrderStatus.ACCEPTED,    WorkOrderStatus.IN_PROGRESS,  List.of(5));
        put(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED,    List.of(5));
        put(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.FAILED,       List.of(5));
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.PENDING,      List.of(4, 5));
        put(WorkOrderStatus.ACCEPTED,    WorkOrderStatus.PENDING,      List.of(4, 5));
        put(WorkOrderStatus.ACCEPTED,    WorkOrderStatus.CANCELLED,    List.of(2, 4));

        /* Phase 5 additions */
        // DISPATCHED — too long without an accept → STALLED or AUTO_CANCELLED
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.STALLED,         List.of(4, 1, 0));   // dispatcher, super_admin, system
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.AUTO_CANCELLED,   List.of(4, 1, 0));
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.REASSIGNING,      List.of(4, 1));
        put(WorkOrderStatus.DISPATCHED,  WorkOrderStatus.CANCELLED,        List.of(2, 4, 1));
        // ACCEPTED — installer rejects / falls back
        put(WorkOrderStatus.ACCEPTED,    WorkOrderStatus.STALLED,         List.of(4, 5, 1));
        put(WorkOrderStatus.ACCEPTED,    WorkOrderStatus.REASSIGNING,      List.of(4, 5, 1));
        // IN_PROGRESS — heartbeat timeout, equipment failure, address wrong
        put(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.STALLED,         List.of(4, 5, 1, 0));
        put(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.REASSIGNING,      List.of(4, 1));
        put(WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.AUTO_CANCELLED,   List.of(4, 1, 0));
        // STALLED — resume, auto-recover, force close
        put(WorkOrderStatus.STALLED,     WorkOrderStatus.IN_PROGRESS,     List.of(4, 5, 1));
        put(WorkOrderStatus.STALLED,     WorkOrderStatus.REASSIGNING,      List.of(4, 1));
        put(WorkOrderStatus.STALLED,     WorkOrderStatus.AUTO_CANCELLED,   List.of(4, 1, 0));
        // REASSIGNING — short-lived; always back to DISPATCHED
        put(WorkOrderStatus.REASSIGNING, WorkOrderStatus.DISPATCHED,       List.of(4, 1, 0));

        finalStates.put(WorkOrderStatus.COMPLETED, true);
        finalStates.put(WorkOrderStatus.CANCELLED, true);
        finalStates.put(WorkOrderStatus.FAILED, true);
        finalStates.put(WorkOrderStatus.AUTO_CANCELLED, true);
    }

    private void put(WorkOrderStatus from, WorkOrderStatus to, List<Integer> roles) {
        table.computeIfAbsent(from, k -> new EnumMap<>(WorkOrderStatus.class)).put(to, roles);
    }

    public WorkOrderStatus next(WorkOrderStatus from, WorkOrderStatus to) {
        if (!table.getOrDefault(from, Map.of()).containsKey(to)) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单状态不允许从 " + from + " 到 " + to);
        }
        return to;
    }

    public void assertTransition(WorkOrderStatus from, WorkOrderStatus to, SecurityUser user) {
        List<Integer> allowed = table.getOrDefault(from, Map.of()).get(to);
        if (allowed == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单状态不允许从 " + from + " 到 " + to);
        }
        if (user != null && user.getRoles() != null) {
            boolean ok = false;
            for (Integer code : allowed) {
                String token = "role_" + code;
                if (user.getRoles().contains(token) || user.getRoles().contains(String.valueOf(code))) {
                    ok = true; break;
                }
            }
            if (!ok) throw new BizException(ResultCode.FORBIDDEN, "无权限执行该状态流转");
        }
    }

    /** System-driven transition — skip role check. */
    public void assertSystemTransition(WorkOrderStatus from, WorkOrderStatus to) {
        if (!table.getOrDefault(from, Map.of()).containsKey(to)) {
            throw new BizException(ResultCode.BAD_REQUEST, "工单状态不允许从 " + from + " 到 " + to);
        }
    }

    public WorkOrderStatus transit(WorkOrderStatus current, WorkOrderStatus to, SecurityUser user) {
        assertTransition(current, to, user);
        return next(current, to);
    }

    public boolean isFinal(WorkOrderStatus s) { return finalStates.getOrDefault(s, false); }
}
