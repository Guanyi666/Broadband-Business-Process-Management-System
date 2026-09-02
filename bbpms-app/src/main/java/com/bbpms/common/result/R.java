package com.bbpms.common.result;

import com.bbpms.common.enums.ResultCode;
import lombok.Data;
import java.io.Serializable;

@Data
public class R<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int code;
    private String msg;
    private T data;
    private String traceId;
    private long timestamp;

    public R() { this.timestamp = System.currentTimeMillis(); }
    public static <T> R<T> ok() { R<T> r = new R<>(); r.setCode(ResultCode.SUCCESS.getCode()); r.setMsg("ok"); return r; }
    public static <T> R<T> ok(T data) { R<T> r = ok(); r.setData(data); return r; }
    public static <T> R<T> ok(T data, String msg) { R<T> r = new R<>(); r.setCode(0); r.setMsg(msg); r.setData(data); return r; }
    public static <T> R<T> fail(int code, String msg) { R<T> r = new R<>(); r.setCode(code); r.setMsg(msg); return r; }
    public static <T> R<T> fail(ResultCode rc) { return fail(rc.getCode(), rc.getMsg()); }
    public static <T> R<T> fail(ResultCode rc, String msg) { return fail(rc.getCode(), msg); }
    public boolean isSuccess() { return this.code == ResultCode.SUCCESS.getCode(); }
}
