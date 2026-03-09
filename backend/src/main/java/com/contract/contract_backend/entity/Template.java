package com.contract.contract_backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "TEMPLATE",
        schema = "PUBLIC",
        indexes = {
                @Index(name = "idx_template_name", columnList = "NAME", unique = true)
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TEMPLATE_ID")
    private Long templateId;

    @Column(name = "NAME", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "CONTRACT_TYPE", nullable = false, length = 50)
    private String contractType;

    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @Column(name = "REMARK", length = 500)
    private String remark;

    /**
     * ENABLED / DISABLED
     */
    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "UPDATED_BY", length = 64)
    private String updatedBy;

    /**
     * 模板附件原始文件名
     */
    @Column(name = "FILE_NAME", length = 255)
    private String fileName;

    /**
     * 模板附件在本地存储中的路径标识
     */
    @Column(name = "FILE_OBJECT_KEY", length = 500)
    private String fileObjectKey;

    @Column(name = "CREATED_AT")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;

        if (this.status == null || this.status.isBlank()) {
            this.status = "ENABLED";
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}