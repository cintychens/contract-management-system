package com.contract.contract_backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：统一当成 400/401/403/409 这种更合理（这里先用 400）
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String msg = e.getMessage() == null ? "请求失败" : e.getMessage();

        // 登录失败
        if (msg.contains("密码错误") || msg.contains("用户不存在") || msg.contains("账号")) {
            status = HttpStatus.UNAUTHORIZED;
        }

        // 注册冲突
        if (msg.contains("用户名已存在")) {
            status = HttpStatus.CONFLICT;
        }

        // 禁用账号
        if (msg.contains("禁用")) {
            status = HttpStatus.FORBIDDEN;
        }

        return ResponseEntity.status(status).body(Map.of(
                "code", status.value(),
                "message", msg
        ));
    }

    // 参数异常
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        String msg = e.getMessage() == null ? "参数错误" : e.getMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "code", 400,
                "message", msg
        ));
    }

    // 兜底：真正的服务器错误
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        e.printStackTrace();

        String msg = e.getMessage() == null ? "服务器内部错误" : e.getMessage();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "服务器内部错误",
                "error", e.getClass().getSimpleName(),
                "detail", msg
        ));
    }
}