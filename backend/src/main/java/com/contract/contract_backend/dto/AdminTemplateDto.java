package com.contract.contract_backend.dto;

import lombok.*;

import java.time.LocalDateTime;

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
}