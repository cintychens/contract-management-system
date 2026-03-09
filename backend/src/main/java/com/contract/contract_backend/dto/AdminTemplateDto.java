package com.contract.contract_backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class AdminTemplateDto {

    /**
     * 列表行
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateRow {
        private Long templateId;
        private String name;
        private String contractType;
        private String status;
        private String remark;
        private String updatedBy;
        private String fileName;
        private String fileObjectKey;
        private LocalDateTime updatedAt;
    }

    /**
     * 详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateDetail {
        private Long templateId;
        private String name;
        private String contractType;
        private String content;
        private String remark;
        private String status;
        private String updatedBy;
        private String fileName;
        private String fileObjectKey;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * 新增/编辑请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SaveReq {
        private String name;
        private String contractType;
        private String content;
        private String remark;
        private String status;
        private String updatedBy;

        /**
         * 模板附件
         */
        private String fileName;
        private String fileObjectKey;
    }

    /**
     * 状态切换请求
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusReq {
        private String status;
        private String updatedBy;
    }

    /**
     * 统计
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Stats {
        private long total;
        private long enabled;
        private long disabled;
    }

    /**
     * 模板字段行
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateFieldRow {
        private Long fieldId;
        private Long templateId;
        private String fieldKey;
        private String fieldName;
        private String fieldType;
        private Boolean requiredFlag;
        private Integer sortOrder;
    }

    /**
     * 模板详情 + 字段列表（后续如果你要一次性返回模板和字段，可以用这个）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemplateDetailWithFields {
        private Long templateId;
        private String name;
        private String contractType;
        private String content;
        private String remark;
        private String status;
        private String updatedBy;
        private String fileName;
        private String fileObjectKey;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<TemplateFieldRow> fields;
    }
}