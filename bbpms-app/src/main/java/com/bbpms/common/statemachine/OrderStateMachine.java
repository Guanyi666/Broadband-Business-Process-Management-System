package com.bbpms.common.statemachine;

import com.bbpms.common.enums.OrderEvent;
import com.bbpms.common.enums.OrderStatus;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.security.SecurityUser;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Order status state machine. Pre-defined transitions with role-based authorization. */
@Component
public class OrderStateMachine {

    private final Map<OrderStatus, Map<OrderEvent, OrderTransition>> table = new EnumMap<>(OrderStatus.class);
    private final Map<OrderStatus, Boolean> finalStates = new HashMap<>();

    public OrderStateMachine() {
        put(OrderStatus.CREATED,    OrderEvent.AUDIT_PASS,      OrderStatus.AUDITED,      List.of(3));
        put(OrderStatus.CREATED,    OrderEvent.AUDIT_REJECT,    OrderStatus.REJECTED,     List.of(3));
        put(OrderStatus.CREATED,    OrderEvent.CANCEL,          OrderStatus.CANCELLED,    List.of(2, 6));
        put(OrderStatus.REJECTED,   OrderEvent.RESUBMIT,        OrderStatus.CREATED,      List.of(2));
        put(OrderStatus.REJECTED,   OrderEvent.CANCEL,          OrderStatus.CANCELLED,    List.of(2, 6));
        put(OrderStatus.AUDITED,    OrderEvent.START_DISPATCH,  OrderStatus.WAIT_DISPATCH,List.of(4));
        put(OrderStatus.WAIT_DISPATCH, OrderEvent.DISPATCH_OK,  OrderStatus.DISPATCHED,   List.of(4));
        put(OrderStatus.WAIT_DISPATCH, OrderEvent.DISPATCH_TIMEOUT, OrderStatus.CANCELLED,List.of(4));
        put(OrderStatus.DISPATCHED, OrderEvent.ACCEPT,          OrderStatus.INSTALLING,   List.of(5));
        put(OrderStatus.DISPATCHED, OrderEvent.TRANSFER,        OrderStatus.WAIT_DISPATCH,List.of(4, 5));
        put(OrderStatus.INSTALLING, OrderEvent.COMPLETE,        OrderStatus.FINISHED,     List.of(5));
        put(OrderStatus.FINISHED,   OrderEvent.CONFIRM,         OrderStatus.CLOSED,       List.of(2, 6));

        finalStates.put(OrderStatus.CLOSED, true);
        finalStates.put(OrderStatus.CANCELLED, true);
    }

    private void put(OrderStatus from, OrderEvent ev, OrderStatus to, List<Integer> roles) {
        table.computeIfAbsent(from, k -> new EnumMap<>(OrderEvent.class))
                .put(ev, new OrderTransition(from, ev, to, roles));
    }

    public OrderStatus next(OrderStatus from, OrderEvent ev) {
        Map<OrderEvent, OrderTransition> m = table.get(from);
        if (m == null) throw new BizException(ResultCode.ORDER_STATUS_INVALID, "当前状态不允许任何事件");
        OrderTransition t = m.get(ev);
        if (t == null) throw new BizException(ResultCode.ORDER_STATUS_INVALID,
                from + " -> " + ev + " 不允许的状态流转");
        return t.to();
    }

    public void assertTransition(OrderStatus from, OrderEvent ev, SecurityUser user) {
        Map<OrderEvent, OrderTransition> m = table.get(from);
        if (m == null) throw new BizException(ResultCode.ORDER_STATUS_INVALID, "当前状态不允许任何事件");
        OrderTransition t = m.get(ev);
        if (t == null) throw new BizException(ResultCode.ORDER_STATUS_INVALID,
                from + " -> " + ev + " 不允许的状态流转");
        if (user != null && user.getRoles() != null && t.allowedRoles() != null) {
            // roles are stored as Integer codes in UserType; SecurityUser roles are Strings -> compare by name prefix 'role_<code>' or direct match
            boolean allowed = false;
            for (Integer code : t.allowedRoles()) {
                String token = "role_" + code;
                if (user.getRoles().contains(token) || user.getRoles().contains(String.valueOf(code))) {
                    allowed = true; break;
                }
            }
            if (!allowed) throw new BizException(ResultCode.FORBIDDEN, "无权限执行该事件");
        }
    }

    /** Convenience: returns the next status after authorization + transition. */
    public OrderStatus transit(Long orderId, OrderStatus current, OrderEvent ev, SecurityUser user) {
        assertTransition(current, ev, user);
        return next(current, ev);
    }

    public boolean isFinal(OrderStatus s) { return finalStates.getOrDefault(s, false); }
}
