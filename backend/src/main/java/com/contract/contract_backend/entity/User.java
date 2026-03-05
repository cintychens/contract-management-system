package com.contract.contract_backend.entity;

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

    @Column(name = "USERNAME", nullable = false, unique = true, length = 64)
    private String username;

    /**
     * ❗安全关键：
     * 不允许返回给前端
     */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 120)
    @JsonIgnore
    private String passwordHash;

    /**
     * USER / ADMIN
     */
    @Column(name = "ROLE_CODE", nullable = false, length = 32)
    private String roleCode;

    /**
     * ENABLED / DISABLED
     */
    @Column(name = "STATUS", nullable = false, length = 16)
    private String status;

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
     * 插入前自动赋值
     */
    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        if (this.roleCode == null) {
            this.roleCode = "USER";
        }

        if (this.status == null) {
            this.status = "ENABLED";
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