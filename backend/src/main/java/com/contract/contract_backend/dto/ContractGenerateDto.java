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

        // 通用字段
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
        private String servicePeriod;   // ✅ 通用保留一份
        private String penaltyRate;     // ✅ 通用保留一份

        // 仓储类模板字段
        private String cargoName;
        private String cargoCategory;
        private String cargoQuantity;
        private String specialRequirement;
        private String warehouseAddress;
        private String inboundDate;
        private String outboundDate;
        private String storagePeriod;
        private String paymentMethod;
        private String paymentTerm;
        private String disputeCourt;

        // 配送类模板字段
        private String originWarehouse;
        private String deliveryArea;
        private String deliveryAddress;
        private String deliveryMode;
        private String deliveryTimeRequirement;
        private String singleWeightLimit;
        private String singleVolumeLimit;
        private String claimPeriod;
        private String pickupTimeLimit;
        private String storageFeeStandard;
        private String deliveryTimeStandard;
        private String insuranceOption;

        // 外包类模板字段
        private String partyAAddress;
        private String partyALegalPerson;
        private String partyAPhone;

        private String partyBAddress;
        private String partyBLegalPerson;
        private String partyBPhone;

        private String serviceScope;
        private String serviceStandard;
        private String feeStructure;
        private String paymentDate;
        private String exceptionHandling;
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

        // 通用字段
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
        private String servicePeriod;   // ✅ 通用保留一份
        private String penaltyRate;     // ✅ 通用保留一份

        // 仓储类模板字段
        private String cargoName;
        private String cargoCategory;
        private String cargoQuantity;
        private String specialRequirement;
        private String warehouseAddress;
        private String inboundDate;
        private String outboundDate;
        private String storagePeriod;
        private String paymentMethod;
        private String paymentTerm;
        private String disputeCourt;

        // 配送类模板字段
        private String originWarehouse;
        private String deliveryArea;
        private String deliveryAddress;
        private String deliveryMode;
        private String deliveryTimeRequirement;
        private String singleWeightLimit;
        private String singleVolumeLimit;
        private String claimPeriod;
        private String pickupTimeLimit;
        private String storageFeeStandard;
        private String deliveryTimeStandard;
        private String insuranceOption;

        // 外包类模板字段
        private String partyAAddress;
        private String partyALegalPerson;
        private String partyAPhone;

        private String partyBAddress;
        private String partyBLegalPerson;
        private String partyBPhone;

        private String serviceScope;
        private String serviceStandard;
        private String feeStructure;
        private String paymentDate;
        private String exceptionHandling;
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