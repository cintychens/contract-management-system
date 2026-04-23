package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contract_milestone")
public class ContractMilestone {

    // ⭐ 主键（必须明确自增策略）
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐ 关联合同
    @Column(nullable = false)
    private Long contractId;

    // ⭐ 节点名称（发货/到货等）
    @Column(nullable = false, length = 50)
    private String name;

    // ⭐ 顺序（1,2,3,4）
    @Column(nullable = false)
    private Integer sortOrder;

    // ⭐ 计划时间
    private LocalDateTime dueDate;

    // ⭐ 实际完成时间
    private LocalDateTime actualDate;

    // ⭐ 状态（PENDING / COMPLETED）
    @Column(nullable = false, length = 20)
    private String status;

    // ⭐ 负责人角色
    @Column(nullable = false, length = 20)
    private String responsibleRole;

    // ⭐ 是否锁定（防止修改）
    private Boolean isLocked = false;
}