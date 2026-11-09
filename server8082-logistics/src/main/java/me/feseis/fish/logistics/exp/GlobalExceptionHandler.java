package me.feseis.fish.logistics.exp;

import java.time.OffsetDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常出口：统一转换成 RFC 9457 的 ProblemDetail。
 * 成功响应保持裸 DTO，不在这里包壳。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ProblemDetail handleBiz(BizException e) {
        return build(e.getErrorCode(), e.getDetail());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(null);
        return build(ErrorCode.INVALID_PARAM, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException e) {
        return build(ErrorCode.INVALID_PARAM, "请求体格式错误");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNotFound(NoResourceFoundException e) {
        return build(ErrorCode.NOT_FOUND, e.getResourcePath());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception e) {
        log.error("未处理异常", e);
        return build(ErrorCode.INTERNAL, null);
    }

    private ProblemDetail build(ErrorCode errorCode, String detail) {
        String resolvedDetail = detail == null || detail.isBlank() ? errorCode.message() : detail;
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.status(), resolvedDetail);
        problem.setTitle(errorCode.message());
        problem.setProperty("code", errorCode.code());
        problem.setProperty("timestamp", OffsetDateTime.now());
        return problem;
    }
}
