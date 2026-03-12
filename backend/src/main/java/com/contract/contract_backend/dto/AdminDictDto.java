package com.contract.contract_backend.dto;

import lombok.*;

public class AdminDictDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveReq {
        private String dictType;
        private String itemKey;
        private String itemName;
        private String valueType;
        private String moduleName;
        private Boolean requiredFlag;
        private String itemValue;
        private String status;
        private Integer sortOrder;
        private String remark;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusReq {
        private String status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Row {
        private Long id;
        private String dictType;
        private String itemKey;
        private String itemName;
        private String valueType;
        private String moduleName;
        private Boolean requiredFlag;
        private String itemValue;
        private String status;
        private Integer sortOrder;
        private String remark;
        private Long createdBy;
        private Long updatedBy;
        private String createdAt;
        private String updatedAt;
    }
}