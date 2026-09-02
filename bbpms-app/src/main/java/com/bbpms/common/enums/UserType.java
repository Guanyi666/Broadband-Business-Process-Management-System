package com.bbpms.common.enums;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter @AllArgsConstructor
public enum UserType { SUPER_ADMIN(1), CUSTOMER_SERVICE(2), AUDITOR(3), DISPATCHER(4), INSTALLER(5), CUSTOMER(6);
    private final int code;
}
