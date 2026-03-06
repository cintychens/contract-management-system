package com.contract.contract_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractUploadResponse {
    private Long contractId;
    private String contractNo;
    private Long versionId;
    private String status;
}