package com.contract.contract_backend.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常：统一当成 400/401/403/409 这种更合理（这里先用 400）
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException e) {
        // 你也可以根据 message 细分 status，我这里先给你一个简单版本
        HttpStatus status = HttpStatus.BAD_REQUEST;

        String msg = e.getMessage() == null ? "请求失败" : e.getMessage();

        // 常见情况：登录失败更适合 401
        if (msg.contains("密码错误") || msg.contains("用户不存在") || msg.contains("账号")) {
            status = HttpStatus.UNAUTHORIZED; // 401
        }
        // 注册用户名已存在更适合 409
        if (msg.contains("用户名已存在")) {
            status = HttpStatus.CONFLICT; // 409
        }
        // 禁用账号更适合 403
        if (msg.contains("禁用")) {
            status = HttpStatus.FORBIDDEN; // 403
        }

        return ResponseEntity.status(status).body(Map.of(
                "code", status.value(),
                "message", msg
        ));
    }

    // 兜底：真正的服务器错误
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {

        // ✅ 关键：把真实错误打到控制台（否则你永远不知道 500 原因）
        e.printStackTrace();

        // ✅ 开发阶段建议把 message 返回（上线前可以删掉 message）
        String msg = e.getMessage() == null ? "服务器内部错误" : e.getMessage();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "code", 500,
                "message", "服务器内部错误",
                "error", e.getClass().getSimpleName(),
                "detail", msg
        ));
    }
}