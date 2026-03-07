package com.contract.contract_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ContractGenerateDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateReq {
        private Long templateId;
        private String title;
        private String partyA;
        private String partyB;
        private String amount;
        private String signDate;
        private String effectiveDate;
        private String expireDate;
        private String serviceContent;
        private String paymentTerms;
        private String breachLiability;
        private String extraRequirements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GenerateResp {
        private Long templateId;
        private String templateName;
        private String contractType;
        private String draftContent;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmReq {
        private Long templateId;
        private String title;
        private String contractType;
        private String draftContent;

        private String partyA;
        private String partyB;
        private String amount;
        private String signDate;
        private String effectiveDate;
        private String expireDate;
        private String serviceContent;
        private String paymentTerms;
        private String breachLiability;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfirmResp {
        private Long contractId;
        private String contractNo;
        private Long versionId;
        private String status;
    }
}