package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TEMPLATE_FIELD", schema = "PUBLIC")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FIELD_ID")
    private Long fieldId;

    // 原 fieldKey 保留，作为字段编码
    @Column(name = "FIELD_KEY", nullable = false, unique = true, length = 100)
    private String fieldKey;

    @Column(name = "FIELD_NAME", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "FIELD_TYPE", nullable = false, length = 50)
    private String fieldType;

    @Column(name = "BUSINESS_TYPE", length = 50)
    private String businessType;

    @Column(name = "MODULE_NAME", length = 100)
    private String moduleName;

    @Column(name = "REQUIRED_FLAG", nullable = false)
    private Boolean requiredFlag;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "DEFAULT_VALUE", length = 500)
    private String defaultValue;

    @Column(name = "PLACEHOLDER", length = 200)
    private String placeholder;

    @Column(name = "REMARK", length = 500)
    private String remark;
}