package com.bbpms.common.config;

import com.bbpms.common.enums.ResultCode;
import com.bbpms.common.exception.BizException;
import com.bbpms.common.exception.OptimisticLockException;
import com.bbpms.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Void>> handleBiz(BizException ex, HttpServletRequest req) {
        log.warn("BizException uri={} msg={} code={}", req.getRequestURI(), ex.getMessage(), ex.getCode());
        // Map authentication failures (token missing/invalid/expired) to HTTP 401 and
        // authorization failures to HTTP 403 so frontends that branch on the HTTP
        // status (H5 redirects on 401, admin toasts on business code 401) agree.
        // Login business failures (1001/1002/1003) deliberately stay HTTP 200 — a
        // failed login is not an expired session.
        int code = ex.getCode();
        HttpStatus status = switch (code) {
            case 401, 1010, 1011 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.OK;
        };
        return ResponseEntity.status(status).body(R.fail(code, ex.getMessage()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public R<Void> handleOpt(OptimisticLockException ex) {
        log.warn("OptimisticLockException: {}", ex.getMessage());
        return R.fail(ResultCode.VERSION_CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(this::fmt)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraint(ConstraintViolationException ex) {
        return R.fail(ResultCode.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        return R.fail(ResultCode.BAD_REQUEST, "缺少参数: " + ex.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBadJson(HttpMessageNotReadableException ex) {
        return R.fail(ResultCode.BAD_REQUEST, "请求体格式错误");
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleDenied(AccessDeniedException ex) {
        return R.fail(ResultCode.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleOther(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception uri={}", req.getRequestURI(), ex);
        return R.fail(ResultCode.INTERNAL_ERROR, "服务器内部错误");
    }

    private String fmt(FieldError e) { return e.getField() + ":" + e.getDefaultMessage(); }
}
