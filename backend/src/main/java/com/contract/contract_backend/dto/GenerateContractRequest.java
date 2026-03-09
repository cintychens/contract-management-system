package com.contract.contract_backend.dto;

import lombok.Data;

import java.util.Map;

@Data
public class GenerateContractRequest {
    private Long templateId;
    private Map<String, String> variables;
}