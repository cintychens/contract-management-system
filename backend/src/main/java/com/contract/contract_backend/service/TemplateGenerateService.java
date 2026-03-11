package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.GenerateContractRequest;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractField;
import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.ContractFieldRepository;
import com.contract.contract_backend.repository.ContractRepository;
import com.contract.contract_backend.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private final ContractRepository contractRepository;
    private final ContractFieldRepository contractFieldRepository;

    /**
     * 同时支持 ${variable} 和 {{variable}}
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
     * 根据模板和变量生成合同内容，并自动保存合同 + 字段
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

        // 自动生成合同编号
        String contractNo = generateContractNo();

        // 自动生成合同标题
        String title = generateContractTitle(template, inputVariables, contractNo);

        // 保存合同
        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .title(title)
                .contractType(template.getContractType())
                .status("DRAFT")
                .currentVersionId(null)
                .createdBy(1L) // 先写死，后面接登录用户再改
                .createdAt(LocalDateTime.now())
                .templateId(template.getTemplateId())
                .content(generatedContent)
                .build();

        Contract savedContract = contractRepository.save(contract);

        // ✅ 关键新增：保存结构化字段
        saveGeneratedFields(savedContract.getContractId(), inputVariables);

        Map<String, Object> result = new HashMap<>();
        result.put("templateId", template.getTemplateId());
        result.put("templateName", template.getName());
        result.put("contractType", template.getContractType());
        result.put("requiredVariables", requiredVariables);
        result.put("usedVariables", inputVariables);
        result.put("generatedContent", generatedContent);

        // 保存后的合同信息
        result.put("contractId", savedContract.getContractId());
        result.put("contractNo", savedContract.getContractNo());
        result.put("title", savedContract.getTitle());
        result.put("status", savedContract.getStatus());

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

    /**
     * 生成唯一合同编号
     */
    private String generateContractNo() {
        String contractNo;
        do {
            contractNo = "CT" + System.currentTimeMillis();
        } while (contractRepository.existsByContractNo(contractNo));
        return contractNo;
    }

    /**
     * 生成合同标题
     */
    private String generateContractTitle(Template template, Map<String, String> variables, String contractNo) {
        String partyA = variables != null ? variables.get("partyA") : null;
        String partyB = variables != null ? variables.get("partyB") : null;

        if (partyA != null && !partyA.isBlank() && partyB != null && !partyB.isBlank()) {
            return partyA + " 与 " + partyB + " - " + template.getName();
        }

        return template.getName() + " - " + contractNo;
    }

    /**
     * 保存模板生成时的字段
     */
    private void saveGeneratedFields(Long contractId, Map<String, String> variables) {
        if (contractId == null || variables == null || variables.isEmpty()) {
            return;
        }

        contractFieldRepository.deleteByContractId(contractId);

        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String fieldKey = entry.getKey();
            String fieldValue = entry.getValue();

            if (fieldValue == null || fieldValue.isBlank()) {
                continue;
            }

            ContractField field = ContractField.builder()
                    .contractId(contractId)
                    .fieldKey(fieldKey)
                    .fieldName(toFieldName(fieldKey))
                    .fieldValue(fieldValue.trim())
                    .sourceRef("template_generate")
                    .confidence(1.0)
                    .updatedBy(1L)
                    .updatedAt(LocalDateTime.now())
                    .build();

            contractFieldRepository.save(field);
        }
    }

    /**
     * 字段名转换
     */
    private String toFieldName(String fieldKey) {
        return switch (fieldKey) {
            case "partyA" -> "甲方名称";
            case "partyB" -> "乙方名称";
            case "cargoName" -> "货物名称";
            case "cargoCategory" -> "货物类别";
            case "cargoQuantity" -> "预计数量";
            case "specialRequirement" -> "特殊要求";
            case "warehouseAddress" -> "仓储地点";
            case "inboundDate" -> "入库日期";
            case "outboundDate" -> "出库日期";
            case "storagePeriod" -> "仓储期限";
            case "amount" -> "合同金额";
            case "paymentMethod" -> "结算方式";
            case "paymentTerm" -> "结算周期";
            case "disputeCourt" -> "争议法院";
            case "signDate" -> "签署日期";
            default -> fieldKey;
        };
    }
}