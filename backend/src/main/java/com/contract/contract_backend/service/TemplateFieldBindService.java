package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminTemplateFieldBindDto;

import java.util.List;

public interface TemplateFieldBindService {

    List<AdminTemplateFieldBindDto> listByTemplateId(Long templateId);
}