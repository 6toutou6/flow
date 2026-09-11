package com.company.flow.sys.base.exception;

import com.company.flow.sys.base.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理（与 gzfb / 内网框架同款）：业务校验异常统一返回友好提示，避免直接 500。
 * service 层主动 throw new RuntimeException("提示语")，前端请求层会把它弹出来。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常（service 层主动抛出） */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getMessage() == null || e.getMessage().isEmpty() ? "操作失败" : e.getMessage());
    }

    /** 兜底：未预期的系统异常（同样返回 Result 结构，避免前端拿到 Spring 默认错误体） */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail("系统异常，请稍后重试");
    }
}
