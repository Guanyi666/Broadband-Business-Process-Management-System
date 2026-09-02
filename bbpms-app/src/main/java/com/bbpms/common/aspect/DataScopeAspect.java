package com.bbpms.common.aspect;

import com.bbpms.common.annotation.DataScope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Marker aspect. Actual SQL filtering is performed by
 * {@link com.bbpms.common.config.DataScopeInnerInterceptor} which reads the
 * {@link com.bbpms.common.security.SecurityUser} from the {@link com.bbpms.common.security.SecurityContextHolder}.
 *
 * This class only logs which method was annotated so developers can trace scope decisions.
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    @Before("@annotation(anno) || @within(anno)")
    public void before(JoinPoint jp, DataScope anno) {
        if (anno == null) {
            // Spring AOP can match the disjunction without binding the parameter
            // (e.g. the @within branch matched a type while @annotation found
            // nothing on the method). Actual SQL filtering is done by
            // DataScopeInnerInterceptor; here we only log and skip.
            log.debug("DataScope joinpoint matched without annotation binding: {}",
                    jp.getSignature().toShortString());
            return;
        }
        log.debug("DataScope active on {}.{} : {} (deptCol={})",
                jp.getSignature().getDeclaringTypeName(),
                jp.getSignature().getName(),
                anno.value(), anno.deptIdColumn());
    }
}
