package com.contract.contract_backend.entity;

import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.common.constant.RoleCode;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_no", nullable = false, unique = true, length = 64)
    private String contractNo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "contract_type", nullable = false, length = 64)
    private String contractType;

    /**
     * DRAFT / PENDING_LEGAL / PENDING_FINANCE / PENDING_APPROVAL
     * / ACTIVE / IN_PROGRESS / COMPLETED / TERMINATED / ARCHIVED
     */
    @Column(name = "status", nullable = false, length = 32)
    private String status;

    /**
     * 当前版本ID
     */
    @Column(name = "current_version_id")
    private Long currentVersionId;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 当前待处理角色
     * BUSINESS / LEGAL / FINANCE / APPROVER / ADMIN
     */
    @Column(name = "current_handler_role", length = 32)
    private String currentHandlerRole;

    /**
     * 当前处理人ID（可为空）
     */
    @Column(name = "current_handler_id")
    private Long currentHandlerId;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 提交审批时间
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * 最终审批通过时间
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * 合同关闭时间（完成/终止/归档）
     */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    /**
     * 使用的模板ID
     */
    @Column(name = "template_id")
    private Long templateId;

    /**
     * 合同正文内容
     */
    @Lob
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }

        if (this.status == null || this.status.isBlank()) {
            this.status = ContractStatus.DRAFT;
        }

        if (this.currentHandlerRole == null || this.currentHandlerRole.isBlank()) {
            this.currentHandlerRole = RoleCode.BUSINESS;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}