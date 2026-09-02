package com.bbpms.common.security;

/** ThreadLocal-backed holder for SecurityUser. Always call clear() in a finally block. */
public final class SecurityContextHolder {

    private static final ThreadLocal<SecurityUser> TL = new ThreadLocal<>();

    private SecurityContextHolder() {}

    public static void set(SecurityUser u) { TL.set(u); }

    public static SecurityUser get() { return TL.get(); }

    public static void clear() { TL.remove(); }

    public static Long userId() {
        SecurityUser u = TL.get();
        return u == null ? null : u.getUserId();
    }

    public static String username() {
        SecurityUser u = TL.get();
        return u == null ? null : u.getUsername();
    }
}
