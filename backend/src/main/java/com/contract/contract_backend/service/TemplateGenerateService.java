package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.GenerateContractRequest;
import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateGenerateService {

    private final TemplateRepository templateRepository;

    /**
     * 匹配 {{variable}} 形式的占位符
     */
    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\$\\{\\s*([^}]+)\\s*\\}|\\{\\{\\s*(.*?)\\s*\\}\\}");

    /**
     * 根据模板ID解析变量
     */
    public Map<String, Object> getTemplateVariables(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在，templateId=" + templateId));

        List<String> variables = extractVariables(template.getContent());

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", template.getTemplateId());
        result.put("templateName", template.getName());
        result.put("contractType", template.getContractType());
        result.put("variables", variables);

        return result;
    }

    /**
     * 根据模板和变量生成合同内容
     */
    public Map<String, Object> generateContract(GenerateContractRequest request) {
        if (request == null || request.getTemplateId() == null) {
            throw new RuntimeException("templateId不能为空");
        }

        Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("模板不存在，templateId=" + request.getTemplateId()));

        String content = template.getContent();
        List<String> requiredVariables = extractVariables(content);
        Map<String, String> inputVariables = request.getVariables();

        for (String variable : requiredVariables) {
            if (inputVariables == null || !inputVariables.containsKey(variable)) {
                throw new RuntimeException("缺少变量：" + variable);
            }
        }

        String generatedContent = replaceVariables(content, inputVariables);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", template.getTemplateId());
        result.put("templateName", template.getName());
        result.put("contractType", template.getContractType());
        result.put("requiredVariables", requiredVariables);
        result.put("usedVariables", inputVariables);
        result.put("generatedContent", generatedContent);

        return result;
    }

    /**
     * 提取模板中的变量，去重并保持顺序
     */

    private List<String> extractVariables(String content) {
        LinkedHashSet<String> variableSet = new LinkedHashSet<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(content == null ? "" : content);

        while (matcher.find()) {
            String variableName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            if (variableName != null && !variableName.trim().isEmpty()) {
                variableSet.add(variableName.trim());
            }
        }

        return new ArrayList<>(variableSet);
    }

    /**
     * 替换模板变量
     */
    private String replaceVariables(String content, Map<String, String> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(content == null ? "" : content);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String variableName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            variableName = variableName.trim();

            String replacement = variables.getOrDefault(variableName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}