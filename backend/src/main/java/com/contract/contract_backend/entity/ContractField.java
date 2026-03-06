package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_field")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "field_id")
    private Long fieldId;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "field_value", length = 2000)
    private String fieldValue;

    @Column(name = "source_ref", length = 500)
    private String sourceRef;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}