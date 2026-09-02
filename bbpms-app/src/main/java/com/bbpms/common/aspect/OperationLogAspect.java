package com.bbpms.common.aspect;

import com.bbpms.common.annotation.OperationLog;
import com.bbpms.common.event.BbpmsEvents;
import com.bbpms.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Wraps methods tagged with {@link OperationLog} and emits an {@link BbpmsEvents.OperationLogEvent}. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final ApplicationEventPublisher publisher;

    @Around("@annotation(anno)")
    public Object around(ProceedingJoinPoint pjp, OperationLog anno) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String value = anno.value().isEmpty() ? method.getName() : anno.value();
        String module = anno.module();
        Object result = null;
        Throwable error = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> detail = new HashMap<>();
            detail.put("method", method.getDeclaringClass().getName() + "." + method.getName());
            detail.put("value", value);
            detail.put("module", module);
            detail.put("costMs", cost);
            detail.put("userId", SecurityUtils.getCurrentUserId());
            detail.put("username", SecurityUtils.getCurrentUsername());
            if (anno.saveParams()) detail.put("args", pjp.getArgs());
            if (anno.saveResult()) detail.put("result", result);
            if (error != null) detail.put("error", error.getMessage());
            try {
                BbpmsEvents.OperationLogEvent evt = new BbpmsEvents.OperationLogEvent();
                evt.setUserId(SecurityUtils.getCurrentUserId());
                evt.setUsername(SecurityUtils.getCurrentUsername());
                evt.setModule(module);
                evt.setAction(value);
                evt.setCostMs(cost);
                if (error != null) evt.setError(error.getMessage());
                publisher.publishEvent(evt);
            } catch (Exception ex) {
                log.warn("publish OperationLogEvent failed: {}", ex.getMessage());
            }
        }
    }
}
