package com.bbpms.common.annotation;

import com.bbpms.common.enums.ResultCode;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** Method-level distributed lock using Redis/Redisson. SpEL `key` is evaluated against args. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedLock {
    /** SpEL expression evaluated to a lock key. */
    String key();
    long waitTime() default 3;
    long leaseTime() default 30;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    ResultCode errorCode() default ResultCode.LOCK_CONTENDED;
}
