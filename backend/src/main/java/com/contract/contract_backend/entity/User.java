package com.contract.contract_backend.entity;

import com.contract.contract_backend.common.constant.RoleCode;
import com.contract.contract_backend.common.constant.UserStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "USERS",
        schema = "PUBLIC",
        indexes = {
                @Index(name = "idx_users_username", columnList = "USERNAME", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long userId;

    /**
     * 登录账号
     */
    @Column(name = "USERNAME", nullable = false, unique = true, length = 64)
    private String username;

    /**
     * ❗安全关键：不允许返回前端
     */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 120)
    @JsonIgnore
    private String passwordHash;

    /**
     * BUSINESS / LEGAL / FINANCE / APPROVER / ADMIN
     */
    @Column(name = "ROLE_CODE", nullable = false, length = 32)
    private String roleCode;

    /**
     * ENABLED / DISABLED
     */
    @Column(name = "STATUS", nullable = false, length = 16)
    private String status;

    /**
     * 用户真实姓名
     */
    @Column(name = "FULL_NAME", length = 100)
    private String fullName;

    /**
     * 备注
     */
    @Column(name = "REMARK", length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @Column(name = "CREATED_AT")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "UPDATED_AT")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 最后登录时间
     */
    @Column(name = "LAST_LOGIN_AT")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    /**
     * 插入前自动赋值
     */
    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.roleCode == null || this.roleCode.isBlank()) {
            this.roleCode = RoleCode.BUSINESS;
        }

        if (this.status == null || this.status.isBlank()) {
            this.status = UserStatus.ENABLED;
        }
    }

    /**
     * 更新前自动更新时间
     */
    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}