package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_dict_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SysDictItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 字典类型：
     * CONTRACT_FIELD / NODE_TYPE / ALERT_RULE / ENUM_VALUE
     */
    @Column(name = "dict_type", nullable = false, length = 50)
    private String dictType;

    /**
     * 字典编码
     * 例如：party_a / delivery_date / CONTRACT_TYPE
     */
    @Column(name = "item_key", nullable = false, length = 100)
    private String itemKey;

    /**
     * 字典名称
     * 例如：甲方名称 / 交付日期 / 合同类型
     */
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    /**
     * 值类型
     * 例如：字符串 / 数字 / 日期 / 枚举 / 天数
     */
    @Column(name = "value_type", length = 50)
    private String valueType;

    /**
     * 所属模块
     * 例如：基本信息 / 财务信息 / 履约节点 / 预警规则
     */
    @Column(name = "module_name", length = 100)
    private String moduleName;

    /**
     * 是否必填
     */
    @Column(name = "required_flag")
    private Boolean requiredFlag;

    /**
     * 字典值或枚举值
     * 例如：运输/仓储/供应链/配送/外包
     * 或规则参数：3
     */
    @Column(name = "item_value", length = 1000)
    private String itemValue;

    /**
     * 启用状态：ENABLED / DISABLED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 排序
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

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
            this.status = "ENABLED";
        }
        if (this.requiredFlag == null) {
            this.requiredFlag = false;
        }
        if (this.sortOrder == null) {
            this.sortOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}