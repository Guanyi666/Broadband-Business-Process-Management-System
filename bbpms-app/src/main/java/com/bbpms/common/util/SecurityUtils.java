package com.bbpms.common.util;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.security.SecurityContextHolder;
import com.bbpms.common.security.SecurityUser;

import java.util.List;

/** Convenience accessors for the SecurityUser in the current request thread. */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static SecurityUser getCurrentUser() { return SecurityContextHolder.get(); }

    public static Long getCurrentUserId() {
        SecurityUser u = SecurityContextHolder.get();
        return u == null ? null : u.getUserId();
    }

    public static String getCurrentUsername() {
        SecurityUser u = SecurityContextHolder.get();
        return u == null ? null : u.getUsername();
    }

    public static List<String> getCurrentRoles() {
        SecurityUser u = SecurityContextHolder.get();
        return u == null ? null : u.getRoles();
    }

    public static boolean hasRole(String role) {
        SecurityUser u = SecurityContextHolder.get();
        if (u == null || u.getRoles() == null) return false;
        return u.getRoles().contains(role);
    }

    public static boolean isAuthenticated() { return SecurityContextHolder.get() != null; }

    public static Long requireUserId() {
        Long id = getCurrentUserId();
        if (id == null) throw new BizException(ResultCode.UNAUTHORIZED, "未登录");
        return id;
    }
}
