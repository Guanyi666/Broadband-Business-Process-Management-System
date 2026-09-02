package com.bbpms.common.annotation;

import java.lang.annotation.*;

/** Tag a method for inclusion in the operation-log persistence pipeline. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    /** Human-readable summary of the operation. */
    String value() default "";
    /** Module name (e.g. "order", "user"). */
    String module() default "";
    boolean saveParams() default true;
    boolean saveResult() default false;
}
