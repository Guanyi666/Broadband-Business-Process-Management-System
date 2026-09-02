package com.bbpms.common.util;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/** Lightweight guard helpers that raise BizException on failure. */
public final class AssertUtils {

    private AssertUtils() {}

    public static void notNull(Object o) {
        if (o == null) throw new BizException(ResultCode.BAD_REQUEST, "参数不能为空");
    }

    public static void notNull(Object o, String msg) {
        if (o == null) throw new BizException(ResultCode.BAD_REQUEST, msg);
    }

    public static void notNull(Object o, ResultCode rc) {
        if (o == null) throw new BizException(rc);
    }

    public static void notEmpty(String s) {
        if (s == null || s.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, "字符串不能为空");
    }

    public static void notEmpty(String s, String msg) {
        if (s == null || s.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, msg);
    }

    public static void notEmpty(Collection<?> c) {
        if (c == null || c.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, "集合不能为空");
    }

    public static void notEmpty(Collection<?> c, String msg) {
        if (c == null || c.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, msg);
    }

    public static void notEmpty(Map<?, ?> m) {
        if (m == null || m.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, "Map不能为空");
    }

    public static void notEmpty(Map<?, ?> m, String msg) {
        if (m == null || m.isEmpty()) throw new BizException(ResultCode.BAD_REQUEST, msg);
    }

    public static void isTrue(boolean b) {
        if (!b) throw new BizException(ResultCode.BAD_REQUEST, "断言失败");
    }

    public static void isTrue(boolean b, String msg) {
        if (!b) throw new BizException(ResultCode.BAD_REQUEST, msg);
    }

    public static void isTrue(boolean b, ResultCode rc) {
        if (!b) throw new BizException(rc);
    }

    /** Custom supplier hook for callers wanting a specific error code. */
    public static void check(Supplier<Boolean> cond, ResultCode rc, String msg) {
        if (!Boolean.TRUE.equals(cond.get())) throw new BizException(rc, msg);
    }
}
