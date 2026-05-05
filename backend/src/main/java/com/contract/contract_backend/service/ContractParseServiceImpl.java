package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.common.utils.FileTypeUtil;
import com.contract.contract_backend.common.utils.LocalStoredFileUtil;
import com.contract.contract_backend.config.LocalFileStorageProperties;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractField;
import com.contract.contract_backend.entity.ContractVersion;
import com.contract.contract_backend.repository.ContractFieldRepository;
import com.contract.contract_backend.repository.ContractRepository;
import com.contract.contract_backend.repository.ContractVersionRepository;
import com.contract.contract_backend.service.ContractParseService;
import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.TemplateRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class ContractParseServiceImpl implements ContractParseService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractFieldRepository contractFieldRepository;
    private final LocalFileStorageProperties localFileStorageProperties;
    private final TemplateRepository templateRepository;

    @Override
    public void parseContract(Long contractId) {

        // 1. 查合同
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getCurrentVersionId() == null) {
            throw new RuntimeException("合同当前版本不存在");
        }

        // 2. 拿合同文本（只截取原合同，避免AI污染）
        String text = extractOriginalContract(contract.getContent());

        LocalDateTime now = LocalDateTime.now();

        // 3. 模板字段
        Template template = templateRepository.findById(contract.getTemplateId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        Map<String, String> labelMap = extractFieldLabels(template.getContent());

        // 4. 文本解析结果
        Map<String, String> kvMap = extractAllKeyValue(text);
        // ⭐⭐⭐ 兜底提取甲乙方（确保100%命中）
        String partyA = extractByRegex(text, "甲方.*?[：:](.+)");
        String partyB = extractByRegex(text, "乙方.*?[：:](.+)");

        // ⭐⭐⭐ 兜底提取法院（关键修复）
        String court = extractByRegex(text, "([\\u4e00-\\u9fa5]{2,}(人民法院|仲裁委员会))");

        if (court != null && !court.isBlank()) {
            saveField(contractId, "disputeCourt", "争议法院", court.trim(), 0.9);
        }

        if (partyA != null && !partyA.isBlank()) {
            saveField(contractId, "partyA", "甲方", partyA.trim(), 0.9);
        }

        if (partyB != null && !partyB.isBlank()) {
            saveField(contractId, "partyB", "乙方", partyB.trim(), 0.9);
        }
        Map<String, String> varMap = extractVariables(text);
        Map<String, String> varToField = buildVarToFieldMap();

        // 5. 字段融合（核心修复版）
        labelMap.forEach((key, label) -> {

            String pureLabel = cleanLabel(label);

            Optional<ContractField> existing = contractFieldRepository
                    .findByContractIdAndFieldKey(contractId, key);

            String finalValue = null;
            boolean fromUser = false;

            // ⭐ 优先用用户填写
            if (existing.isPresent()
                    && existing.get().getFieldValue() != null
                    && !existing.get().getFieldValue().isBlank()
                    && !"【待确认】".equals(existing.get().getFieldValue())) {

                finalValue = existing.get().getFieldValue();
                fromUser = true;
            }

            if (finalValue == null) {

                // ⭐ ① 变量匹配（最关键）
                for (String var : varMap.keySet()) {

                    String mappedKey = varToField.get(var);

                    if (key.equals(mappedKey)) {
                        finalValue = var; // 先用变量名
                        break;
                    }
                }

                // ⭐ ② 再走原解析
                if (finalValue == null) {
                    String parsed = matchValue(pureLabel, kvMap);
                    if (parsed != null) {
                        finalValue = parsed;
                    }
                }
            }

            // ⭐ 最后兜底
            if (finalValue == null) {
                finalValue = "【待确认】";
            }

            // =========================
            // ⭐⭐⭐ 核心：不覆盖用户数据
            // =========================
            if (existing.isPresent()) {

                ContractField field = existing.get();

                // ❗只允许更新“解析字段”
                String oldValue = field.getFieldValue();

                boolean oldIsPending =
                        oldValue == null
                                || oldValue.isBlank()
                                || "【待确认】".equals(oldValue);

// ⭐ 只要旧值是【待确认】，就允许解析结果覆盖
                if (oldIsPending) {

                    String cleanedValue = postProcessValue(key, finalValue);
                    field.setFieldValue(cleanedValue);
                    field.setSourceRef(fromUser ? "form_input" : "auto_parse");
                    field.setConfidence(fromUser ? 1.0 :
                            (finalValue.equals("【待确认】") ? 0.0 : 0.8));
                    field.setUpdatedAt(now);

                    contractFieldRepository.save(field);
                }

            } else {

                // ⭐ 没有才新增
                ContractField field = ContractField.builder()
                        .contractId(contractId)
                        .fieldKey(key)
                        .fieldName(pureLabel)
                        .fieldValue(finalValue)
                        .sourceRef(fromUser ? "form_input" : "auto_parse")
                        .confidence(fromUser ? 1.0 :
                                (finalValue.equals("【待确认】") ? 0.0 : 0.8))
                        .updatedBy(1L)
                        .updatedAt(now)
                        .build();

                contractFieldRepository.save(field);
            }
        });

        contractRepository.save(contract);
    }

    private String postProcessValue(String key, String value) {

        if (value == null) return null;

        value = value.trim();

        switch (key) {

            case "amount":
                // 提取数字
                String amount = extractByRegex(value, "(\\d+(\\.\\d+)?)");
                return amount != null ? amount : value;

            case "timeLimitHours":
                String time = extractByRegex(value, "(\\d+)");
                return time != null ? time : value;

            case "disputeCourt":
                String court = extractByRegex(value, "([\\u4e00-\\u9fa5]{2,}(人民法院|仲裁委员会))");
                return court != null ? court : value;

            case "paymentTerm":
                String days = extractByRegex(value, "(\\d+)");
                return days != null ? days : value;

            case "cargoWeight":
                return value.replaceAll("[^0-9.]", "") + "kg";

            case "contactPhone":
                return value.replaceAll("[^0-9]", "");

            default:
                // 去掉句号后的内容（通用）
                return value.replaceAll("[。；;].*", "").trim();
        }
    }

    private String extractOriginalContract(String text) {
        int idx = text.indexOf("【风险标注】");
        if (idx > 0) return text.substring(0, idx);
        return text;
    }

    private Map<String, String> extractFieldLabels(String content) {

        Map<String, String> map = new LinkedHashMap<>();

        Pattern pattern = Pattern.compile("([^\\n：:]{2,})[：:]\\s*\\$\\{([a-zA-Z0-9_]+)}");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            map.put(matcher.group(2), matcher.group(1));
        }

        return map;
    }

    private Map<String, String> extractAllKeyValue(String text) {

        Map<String, String> map = new LinkedHashMap<>();

        String[] lines = text.split("\\n");

        for (String line : lines) {

            line = line.trim();

            if (!line.contains("：") && !line.contains(":")) continue;

            String[] parts = line.split("：", 2);

            if (parts.length < 2) continue;

            String rawKey = parts[0].trim();
            String value = parts[1].trim();

            if (value.isEmpty()) continue;

            // ⭐⭐⭐ 核心：统一清洗 key
            String cleanKey = rawKey
                    .replaceAll("^\\d+\\.\\s*", "")      // 去 1. 2.
                    .replaceAll("[（(].*?[)）]", "")     // 去（xxx）
                    .trim();

            map.put(cleanKey, value);
        }

        // ⭐⭐⭐ 强制提取甲方乙方（关键修复）
        Pattern partyAPattern = Pattern.compile("甲方.*?[：:](.+)");
        Pattern partyBPattern = Pattern.compile("乙方.*?[：:](.+)");

        Matcher mA = partyAPattern.matcher(text);
        if (mA.find()) {
            map.put("甲方", mA.group(1).trim());
        }

        Matcher mB = partyBPattern.matcher(text);
        if (mB.find()) {
            map.put("乙方", mB.group(1).trim());
        }

        return map;
    }

    private String cleanLabel(String label) {

        return label
                .trim()
                .replaceAll("^\\d+\\.\\s*", "")        // 去 1.
                .replaceAll("[（(].*?[)）]", "")       // 去括号
                .trim();
    }

    private String matchValue(String label, Map<String, String> kvMap) {

        String target = normalize(label);

        String bestValue = null;
        int bestScore = 0;

        for (Map.Entry<String, String> entry : kvMap.entrySet()) {

            String key = normalize(cleanLabel(entry.getKey()));
            String value = entry.getValue();

            int score = calculateScore(target, key);

            if (score > bestScore) {
                bestScore = score;
                bestValue = value;
            }
        }

        // ⭐ 分数太低，不匹配
        if (bestScore < 2) return null;

        return bestValue;
    }

    private int calculateScore(String a, String b) {

        int score = 0;

        // 完全相同
        if (a.equals(b)) return 100;

        // 包含关系
        if (a.contains(b) || b.contains(a)) score += 3;

        // 关键词匹配（逐字）
        for (int i = 0; i < a.length(); i++) {
            if (b.contains(String.valueOf(a.charAt(i)))) {
                score++;
            }
        }

        return score;
    }

    private String normalize(String str) {
        return str
                .replaceAll("^\\d+\\.\\s*", "")
                .replaceAll("[（(].*?[)）]", "")
                .replaceAll("总计|人民币|费用", "")
                .trim();
    }

    private boolean hasCommonWord(String a, String b) {

        for (int i = 0; i < a.length() - 1; i++) {
            String sub = a.substring(i, i + 2);
            if (b.contains(sub)) {
                return true;
            }
        }

        return false;
    }

    private String extractText(Path fullPath, String fileName) {
        String extension = FileTypeUtil.getExtension(fileName);

        try {
            switch (extension) {
                case "txt":
                    return Files.readString(fullPath, StandardCharsets.UTF_8);

                case "pdf":
                    // 这里后面可以接 PDFBox
                    return "【暂未接入 PDF 解析器】";

                case "doc":
                case "docx":
                    // 这里后面可以接 Apache POI / Tika
                    return "【暂未接入 Word 解析器】";

                default:
                    throw new RuntimeException("暂不支持解析该文件类型：" + extension);
            }
        } catch (IOException e) {
            throw new RuntimeException("读取合同文件失败", e);
        }
    }

    private String extractByRegex(String text, String regex) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private void saveField(Long contractId, String fieldKey, String fieldName, String fieldValue, Double confidence) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return;
        }

        ContractField field = ContractField.builder()
                .contractId(contractId)
                .fieldKey(fieldKey)
                .fieldName(fieldName)
                .fieldValue(fieldValue)
                .sourceRef("auto_parse")
                .confidence(confidence)
                .updatedBy(1L)
                .updatedAt(LocalDateTime.now())
                .build();

        contractFieldRepository.save(field);
    }

    // ⭐ 提取变量（paymentDeadline）
    private Map<String, String> extractVariables(String text) {

        Map<String, String> map = new LinkedHashMap<>();

        Pattern p = Pattern.compile("\\b([a-zA-Z]+[A-Z][a-zA-Z0-9]*)\\b");
        Matcher m = p.matcher(text);

        while (m.find()) {
            map.put(m.group(1), m.group(1));
        }

        return map;
    }

    // ⭐ 变量 → 字段映射
    private Map<String, String> buildVarToFieldMap() {

        Map<String, String> map = new LinkedHashMap<>();

        map.put("paymentDeadline", "paymentTerm");
        map.put("paymentMethod", "paymentMethod");
        map.put("amount", "amount");

        return map;
    }
}