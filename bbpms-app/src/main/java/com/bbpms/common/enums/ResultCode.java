package com.bbpms.common.enums;
import lombok.Getter;

@Getter
public enum ResultCode {
    SUCCESS(0, "ok"), BAD_REQUEST(400, "请求参数错误"), UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"), NOT_FOUND(404, "资源不存在"), INTERNAL_ERROR(500, "服务器内部错误"),
    VERSION_CONFLICT(409, "数据版本冲突"), LOCK_CONTENDED(423, "锁竞争激烈"), IDEMPOTENT_REPLAY(200, "幂等命中"),
    USER_NOT_FOUND(1001, "用户不存在"), PASSWORD_ERROR(1002, "密码错误"), USER_DISABLED(1003, "用户已被禁用"),
    TOKEN_EXPIRED(1010, "令牌已过期"), TOKEN_INVALID(1011, "令牌无效"),
    ORDER_STATUS_INVALID(2010, "订单状态不合法"), ORDER_NOT_FOUND(2001, "订单不存在"),
    CUSTOMER_NOT_FOUND(2002, "客户不存在"), PACKAGE_INVALID(2003, "套餐无效"), ADDRESS_NOT_COVERED(2004, "地址暂未覆盖"),
    WORKORDER_STATUS_INVALID(3001, "工单状态不合法"), WORKORDER_NOT_FOUND(3002, "工单不存在"),
    NO_DISPATCH_CANDIDATE(4001, "无可派单人员"), FILE_UPLOAD_FAILED(6001, "文件上传失败"), SMS_SEND_FAILED(7001, "短信发送失败");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}