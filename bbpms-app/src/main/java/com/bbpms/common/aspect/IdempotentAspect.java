package com.bbpms.common.aspect;

import com.bbpms.common.annotation.Idempotent;
import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.result.R;
import com.bbpms.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** SETNX-based first-write-wins idempotency. Caches the serialized result for the TTL window. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final StringRedisTemplate redis;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(anno)")
    public Object around(ProceedingJoinPoint pjp, Idempotent anno) throws Throwable {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String key = "idem:" + resolveKey(anno.key(), method, pjp.getArgs());
        String token = UUID.randomUUID().toString();

        Boolean won = redis.opsForValue().setIfAbsent(key, token, anno.ttl(), anno.timeUnit());
        if (!Boolean.TRUE.equals(won)) {
            String cached = redis.opsForValue().get(key + ":result");
            if (cached != null) return JsonUtils.parse(cached, R.class);
            throw new BizException(anno.errorCode(), "请求处理中,请勿重复提交");
        }
        try {
            Object out = pjp.proceed();
            if (out != null) {
                redis.opsForValue().set(key + ":result", JsonUtils.toJson(out), anno.ttl(), anno.timeUnit());
            }
            return out;
        } catch (Throwable t) {
            // on failure free the slot so retries can run
            redis.delete(key);
            throw t;
        }
    }

    private String resolveKey(String spEl, Method method, Object[] args) {
        EvaluationContext ctx = new StandardEvaluationContext();
        String[] names = nameDiscoverer.getParameterNames(method);
        if (names != null) for (int i = 0; i < names.length; i++) ctx.setVariable(names[i], args[i]);
        Object v = parser.parseExpression(spEl).getValue(ctx);
        return v == null ? "default" : v.toString();
    }

    @SuppressWarnings("unused")
    private ResultCode defaultCode() { return ResultCode.IDEMPOTENT_REPLAY; }
}
