package com.bbpms.common.util;

import jakarta.servlet.http.HttpServletRequest;

/** Resolve the originating client IP even when behind proxies. */
public final class IpUtils {

    private IpUtils() {}

    private static final String[] HEADERS = {
            "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"
    };

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        for (String h : HEADERS) {
            String v = request.getHeader(h);
            if (isValid(v)) return first(v);
        }
        String r = request.getRemoteAddr();
        return r == null ? "unknown" : r;
    }

    private static boolean isValid(String v) {
        return v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v);
    }

    private static String first(String v) {
        int i = v.indexOf(',');
        return i < 0 ? v.trim() : v.substring(0, i).trim();
    }
}
