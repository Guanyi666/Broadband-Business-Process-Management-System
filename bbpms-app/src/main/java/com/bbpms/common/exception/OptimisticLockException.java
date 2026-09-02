package com.bbpms.common.exception;
public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException() { super("数据版本冲突，请刷新重试"); }
    public OptimisticLockException(String message) { super(message); }
}
