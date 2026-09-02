package com.bbpms.common.exception;
import com.bbpms.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int code;
    public BizException(String message) { super(message); this.code = ResultCode.BAD_REQUEST.getCode(); }
    public BizException(int code, String message) { super(message); this.code = code; }
    public BizException(ResultCode rc) { super(rc.getMsg()); this.code = rc.getCode(); }
    public BizException(ResultCode rc, String message) { super(message); this.code = rc.getCode(); }
}
