package com.bbpms.common.annotation;

import java.lang.annotation.*;

/** Marker for methods whose queries must enforce data-scope filtering. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /** SpEL expression describing the scope ("all", "dept", "dept_and_child", "self", etc.). */
    String value() default "dept";
    /** Column on which to filter for tenant/dept, default `dept_id`. */
    String deptIdColumn() default "dept_id";
}
