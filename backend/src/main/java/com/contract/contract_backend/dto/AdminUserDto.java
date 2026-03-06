package com.contract.contract_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ✅ 管理端-用户管理相关 DTO（全部合在一个文件里）
 */
public class AdminUserDto {

    /**
     * 列表行 DTO（返回给前端表格用）
     * 注意：不包含 passwordHash（安全）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserRow {
        private Long userId;
        private String username;
        private String fullName;
        private String remark;
        private String roleCode;   // USER / ADMIN
        private String status;     // ENABLED / DISABLED
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastLoginAt;
    }

    /**
     * 统计卡片 DTO（总数/启用/禁用/角色数量）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private long total;
        private long enabled;
        private long disabled;

        // ✅ 新增：角色数量
        private long adminCount;
        private long userCount;
    }

    /**
     * 编辑用户请求 DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateReq {
        private String roleCode;  // USER / ADMIN
        private String status;    // ENABLED / DISABLED
        private String fullName;
        private String remark;
    }

    /**
     * 重置密码请求 DTO（可选）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResetPwdReq {
        private String newPassword;
    }
}