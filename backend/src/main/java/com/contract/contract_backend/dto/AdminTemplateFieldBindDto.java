package com.contract.contract_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminTemplateFieldBindDto {

    private Long bindId;
    private Long templateId;

    private String fieldKey;
    private String itemKey;
    private String itemName;
    private String valueType;
    private String moduleName;
    private Boolean requiredFlag;
    private String itemValue;
    private String status;
    private Integer sortOrder;
}