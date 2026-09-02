package com.bbpms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum WorkOrderStatus {
    PENDING("PENDING", "待派发"),
    DISPATCHED("DISPATCHED", "已派单"),
    ACCEPTED("ACCEPTED", "已接单"),
    IN_PROGRESS("IN_PROGRESS", "施工中"),
    STALLED("STALLED", "已停滞"),
    REASSIGNING("REASSIGNING", "改派中"),
    COMPLETED("COMPLETED", "已完成"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消"),
    AUTO_CANCELLED("AUTO_CANCELLED", "自动取消");

    private final String code;
    private final String desc;

    public static WorkOrderStatus fromCode(String code) {
        if (code == null) return null;
        for (WorkOrderStatus s : values()) {
            if (s.code.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        return null;
    }

    /** Terminal states (no further transitions allowed). */
    public static boolean isTerminal(WorkOrderStatus s) {
        return s == COMPLETED || s == CANCELLED || s == FAILED || s == AUTO_CANCELLED;
    }
}
