package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @GetMapping("/{id}/fields")
    public List<Map<String, Object>> getTemplateFields(@PathVariable Long id) {

        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        String content = template.getContent();

        // ⭐ 使用 LinkedHashMap 保证顺序
        Map<String, String> fieldMap = new LinkedHashMap<>();

        // ⭐⭐⭐ 核心正则（支持 1. / 中文：${}）
        Pattern pattern = Pattern.compile("(?:\\d+\\.\\s*)?([^:\\n：]{2,})[：:]\\s*\\$\\{([a-zA-Z0-9_]+)}");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String label = matcher.group(1).trim();
            String key = matcher.group(2).trim();

            fieldMap.put(key, label);
        }

        // ⭐ 返回给前端（带中文）
        return fieldMap.entrySet().stream()
                .map(e -> Map.<String, Object>of(
                        "fieldKey", e.getKey(),
                        "fieldName", e.getValue()
                ))
                .toList();
    }
}