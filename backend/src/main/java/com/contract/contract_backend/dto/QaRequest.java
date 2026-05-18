package com.contract.contract_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QaRequest {

    private Long contractId;

    @NotBlank(message = "问题不能为空")
    private String question;
}
