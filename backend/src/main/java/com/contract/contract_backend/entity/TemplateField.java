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

    @Column(name = "TEMPLATE_ID", nullable = false)
    private Long templateId;

    @Column(name = "FIELD_KEY", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "FIELD_NAME", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "FIELD_TYPE", nullable = false, length = 50)
    private String fieldType;

    @Column(name = "REQUIRED_FLAG", nullable = false)
    private Boolean requiredFlag;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;
}