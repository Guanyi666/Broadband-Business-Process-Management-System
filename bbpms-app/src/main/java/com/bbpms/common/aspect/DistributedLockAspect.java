package com.bbpms.common.aspect;

import com.bbpms.common.annotation.DistributedLock;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** Applies {@link DistributedLock} on annotated methods. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAspect {

    private final com.bbpms.common.lock.DistributedLock distributedLock;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(lockAnno)")
    public Object around(ProceedingJoinPoint pjp, DistributedLock lockAnno) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String key = resolveKey(lockAnno.key(), method, pjp.getArgs());
        String token = key + ":" + UUID.randomUUID();

        boolean acquired = distributedLock.tryLock(key, lockAnno.waitTime(), lockAnno.leaseTime(), lockAnno.timeUnit());
        if (!acquired) {
            log.warn("distributed lock not acquired: {}", key);
            throw new BizException(lockAnno.errorCode(), "系统繁忙,请稍后重试");
        }
        try {
            return pjp.proceed();
        } finally {
            distributedLock.unlock(key);
            // token is held for unlock ownership checks if implementing one uses it; not used here.
        }
    }

    private String resolveKey(String spEl, Method method, Object[] args) {
        EvaluationContext ctx = new StandardEvaluationContext();
        String[] names = nameDiscoverer.getParameterNames(method);
        if (names != null) {
            for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
        }
        Expression exp = parser.parseExpression(spEl);
        Object v = exp.getValue(ctx);
        return v == null ? "lock:default" : v.toString();
    }

    // silence unused-warning (kept for future TTL extension)
    @SuppressWarnings("unused")
    private long toMillis(long t, TimeUnit u) { return u.toMillis(t); }

    @SuppressWarnings("unused")
    private ResultCode defaultCode() { return ResultCode.LOCK_CONTENDED; }
}
