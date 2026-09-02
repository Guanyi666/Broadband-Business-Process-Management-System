package com.bbpms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    CREATED("CREATED", "已创建"),
    AUDITED("AUDITED", "已审核"),
    WAIT_DISPATCH("WAIT_DISPATCH", "待派单"),
    DISPATCHED("DISPATCHED", "已派单"),
    INSTALLING("INSTALLING", "安装中"),
    FINISHED("FINISHED", "已完成"),
    CLOSED("CLOSED", "已归档"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    public static OrderStatus fromCode(String code) {
        if (code == null) return null;
        for (OrderStatus s : values()) {
            if (s.code.equalsIgnoreCase(code) || s.name().equalsIgnoreCase(code)) {
                return s;
            }
        }
        return null;
    }
}