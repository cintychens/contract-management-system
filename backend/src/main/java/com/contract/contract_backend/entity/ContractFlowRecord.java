package com.contract.contract_backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "CONTRACT_FLOW_RECORD", schema = "PUBLIC",
        indexes = {
                @Index(name = "idx_flow_contract_id", columnList = "CONTRACT_ID"),
                @Index(name = "idx_flow_operator_id", columnList = "OPERATOR_ID")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractFlowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FLOW_ID")
    private Long flowId;

    @Column(name = "CONTRACT_ID", nullable = false)
    private Long contractId;

    /**
     * 流转前状态
     */
    @Column(name = "FROM_STATUS", length = 32)
    private String fromStatus;

    /**
     * 流转后状态
     */
    @Column(name = "TO_STATUS", length = 32)
    private String toStatus;

    /**
     * 来源角色
     */
    @Column(name = "FROM_ROLE", length = 32)
    private String fromRole;

    /**
     * 目标角色
     */
    @Column(name = "TO_ROLE", length = 32)
    private String toRole;

    /**
     * SUBMIT / APPROVE / REJECT / COMPLETE / TERMINATE
     */
    @Column(name = "ACTION_TYPE", nullable = false, length = 32)
    private String actionType;

    /**
     * 操作人ID
     */
    @Column(name = "OPERATOR_ID", nullable = false)
    private Long operatorId;

    /**
     * 审批/退回意见
     */
    @Column(name = "COMMENT", length = 1000)
    private String comment;

    @Column(name = "CREATED_AT", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}