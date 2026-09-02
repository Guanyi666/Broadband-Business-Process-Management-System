package com.bbpms.common.statemachine;
import com.bbpms.common.enums.OrderEvent;
import com.bbpms.common.enums.OrderStatus;
import java.util.List;
public record OrderTransition(OrderStatus from, OrderEvent event, OrderStatus to, List<Integer> allowedRoles) {}
