package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminTemplateDto;
import com.contract.contract_backend.dto.PageResult;

import java.util.List;

public interface TemplateService {

    PageResult<AdminTemplateDto.TemplateRow> pageTemplates(
            int page,
            int size,
            String keyword,
            String contractType,
            String status
    );

    AdminTemplateDto.TemplateDetail getTemplate(Long templateId);

    AdminTemplateDto.TemplateRow createTemplate(AdminTemplateDto.SaveReq req);

    AdminTemplateDto.TemplateRow updateTemplate(Long templateId, AdminTemplateDto.SaveReq req);

    AdminTemplateDto.TemplateRow updateStatus(Long templateId, AdminTemplateDto.StatusReq req);

    void deleteTemplate(Long templateId);

    AdminTemplateDto.Stats stats();

    List<AdminTemplateDto.TemplateFieldRow> listTemplateFields(Long templateId);
}