package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "contract_milestone")
public class ContractMilestone {

    // ⭐ 主键
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐ 所属合同ID
    @Column(nullable = false)
    private Long contractId;

    // ⭐ 节点名称（发货/到货/验收/付款）
    @Column(nullable = false, length = 50)
    private String name;

    // ⭐ 顺序（流程控制关键）
    @Column(nullable = false)
    private Integer sortOrder;

    // ⭐ 负责人填写的预计完成时间
    private LocalDateTime expectedDate;

    // ⭐ 实际完成时间（系统记录）
    private LocalDateTime actualDate;

    // ⭐ 状态（建议统一枚举值）
    @Column(nullable = false, length = 20)
    private String status;

    // ⭐ 负责人角色（BUSINESS / LEGAL / FINANCE）
    @Column(nullable = false, length = 20)
    private String responsibleRole;

    // ⭐ 是否锁定（可用于禁止修改）
    @Column(nullable = false)
    private Boolean isLocked = false;
}