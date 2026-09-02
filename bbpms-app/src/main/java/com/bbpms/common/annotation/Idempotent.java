package com.bbpms.common.annotation;

import com.bbpms.common.enums.ResultCode;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/** First-call-wins idempotency. Returns cached result for subsequent calls within TTL. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {
    /** SpEL expression evaluated to the idempotency key. */
    String key();
    long ttl() default 60;
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    ResultCode errorCode() default ResultCode.IDEMPOTENT_REPLAY;
}
