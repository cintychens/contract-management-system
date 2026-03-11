package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/templates")
public class TemplateOpenController {

    private final TemplateRepository templateRepository;

    /**
     * 普通用户可访问：查询所有已启用模板（给智能生成合同弹窗下拉框使用）
     */
    @GetMapping("/enabled")
    public List<Map<String, Object>> listEnabledTemplates() {
        return templateRepository.findByStatusIgnoreCase("ENABLED")
                .stream()
                .map(template -> Map.<String, Object>of(
                        "templateId", template.getTemplateId(),
                        "name", template.getName(),
                        "contractType", template.getContractType()
                ))
                .toList();
    }
}