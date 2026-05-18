package com.contract.contract_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QaResponse {

    private Long contractId;
    private String question;
    private String answer;
    private String source;
    private List<String> references;
}
