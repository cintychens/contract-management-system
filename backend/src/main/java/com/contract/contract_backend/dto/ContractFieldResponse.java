package com.contract.contract_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractFieldResponse {
    private Long fieldId;
    private Long contractId;
    private String fieldKey;
    private String fieldName;
    private String fieldValue;
    private String sourceRef;
    private Double confidence;
}