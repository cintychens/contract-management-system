package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.common.utils.ContractNoGenerator;
import com.contract.contract_backend.common.utils.FileTypeUtil;
import com.contract.contract_backend.common.utils.HashUtil;
import com.contract.contract_backend.common.utils.ObjectKeyUtil;
import com.contract.contract_backend.config.ContractUploadProperties;
import com.contract.contract_backend.dto.ContractFieldResponse;
import com.contract.contract_backend.dto.ContractGenerateDto;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractField;
import com.contract.contract_backend.entity.ContractVersion;
import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.repository.ContractFieldRepository;
import com.contract.contract_backend.repository.ContractRepository;
import com.contract.contract_backend.repository.ContractVersionRepository;
import com.contract.contract_backend.repository.TemplateRepository;
import com.contract.contract_backend.service.ContractParseService;
import com.contract.contract_backend.service.ContractService;
import com.contract.contract_backend.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractFieldRepository contractFieldRepository;
    private final TemplateRepository templateRepository;
    private final FileStorageService fileStorageService;
    private final ContractUploadProperties uploadProperties;
    private final ContractParseService contractParseService;

    /**
     * =========================
     * 1. 你原有逻辑：合同文件上传
     * =========================
     */
    @Override
    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file, String title, String contractType) {
        validateUpload(file, title, contractType);

        String originalFileName = FileTypeUtil.sanitizeFileName(file.getOriginalFilename());
        String extension = FileTypeUtil.getExtension(originalFileName);

        String contractNo = generateUniqueContractNo();
        String objectKey = ObjectKeyUtil.buildContractObjectKey(contractNo, originalFileName);

        String fileHash;
        try (InputStream inputStream = file.getInputStream()) {
            fileHash = HashUtil.sha256(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("读取上传文件失败", e);
        }

        String savedObjectKey = fileStorageService.uploadFile(file, objectKey);

        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .title(title)
                .contractType(contractType)
                .status("UPLOADED")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        contract = contractRepository.save(contract);

        ContractVersion version = ContractVersion.builder()
                .contractId(contract.getContractId())
                .versionNo(1)
                .fileName(originalFileName)
                .fileType(extension)
                .fileSize(file.getSize())
                .fileObjectKey(savedObjectKey)
                .fileHash(fileHash)
                .changeNote("初始上传")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        version = contractVersionRepository.save(version);

        contract.setCurrentVersionId(version.getVersionId());
        contractRepository.save(contract);

        // 保留你原有逻辑：上传后立即解析
        contractParseService.parseContract(contract.getContractId());

        return ContractUploadResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .versionId(version.getVersionId())
                .status("PARSED")
                .build();
    }

    /**
     * =========================
     * 2. 你原有逻辑：获取解析字段
     * =========================
     */
    @Override
    public List<ContractFieldResponse> getContractFields(Long contractId) {
        List<ContractField> fields = contractFieldRepository.findByContractId(contractId);

        return fields.stream().map(field -> ContractFieldResponse.builder()
                .fieldId(field.getFieldId())
                .contractId(field.getContractId())
                .fieldKey(field.getFieldKey())
                .fieldName(field.getFieldName())
                .fieldValue(field.getFieldValue())
                .sourceRef(field.getSourceRef())
                .confidence(field.getConfidence())
                .build()
        ).toList();
    }

    /**
     * =========================
     * 3. 新增逻辑：基于模板生成 AI 合同草案
     * =========================
     */
    @Override
    public ContractGenerateDto.GenerateResp generateDraft(ContractGenerateDto.GenerateReq req) {
        validateGenerateReq(req);

        Template template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        if (!"ENABLED".equalsIgnoreCase(template.getStatus())) {
            throw new IllegalArgumentException("模板未启用，不能用于生成合同");
        }

        String draftContent = generateByLlmOrFallback(template, req);

        return ContractGenerateDto.GenerateResp.builder()
                .templateId(template.getTemplateId())
                .templateName(template.getName())
                .contractType(template.getContractType())
                .draftContent(draftContent)
                .build();
    }

    /**
     * =========================
     * 4. 新增逻辑：人工确认后保存 AI 草案
     * =========================
     */
    @Override
    @Transactional
    public ContractGenerateDto.ConfirmResp confirmGeneratedContract(ContractGenerateDto.ConfirmReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("合同标题不能为空");
        }
        if (req.getContractType() == null || req.getContractType().isBlank()) {
            throw new IllegalArgumentException("合同类型不能为空");
        }
        if (req.getDraftContent() == null || req.getDraftContent().isBlank()) {
            throw new IllegalArgumentException("合同草案不能为空");
        }

        String contractNo = generateUniqueContractNo();

        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .title(req.getTitle().trim())
                .contractType(req.getContractType().trim())
                .status("DRAFT")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        contract = contractRepository.save(contract);

        String content = req.getDraftContent().trim();

        ContractVersion version = ContractVersion.builder()
                .contractId(contract.getContractId())
                .versionNo(1)
                .fileName(req.getTitle().trim() + ".txt")
                .fileType("GENERATED")
                .fileSize((long) content.getBytes(StandardCharsets.UTF_8).length)
                .fileObjectKey(null)
                .fileHash(sha256(content))
                // 这里要求你的 ContractVersion 已经新增了 contentText 字段
                .contentText(content)
                .changeNote("AI生成草案并人工确认保存")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        version = contractVersionRepository.save(version);

        contract.setCurrentVersionId(version.getVersionId());
        // 如果你的 Contract 实体里已经有 content 字段，这里顺手保存正文，方便详情直接显示
        contract.setContent(content);
        contractRepository.save(contract);

        // 保存结构化字段，便于后续展示/查询
        contractFieldRepository.deleteByContractId(contract.getContractId());

        saveManualField(contract.getContractId(), "party_a", "甲方名称", req.getPartyA());
        saveManualField(contract.getContractId(), "party_b", "乙方名称", req.getPartyB());
        saveManualField(contract.getContractId(), "amount", "合同金额", req.getAmount());
        saveManualField(contract.getContractId(), "sign_date", "签署日期", req.getSignDate());
        saveManualField(contract.getContractId(), "effective_date", "生效日期", req.getEffectiveDate());
        saveManualField(contract.getContractId(), "expire_date", "到期日期", req.getExpireDate());
        saveManualField(contract.getContractId(), "service_content", "服务内容", req.getServiceContent());
        saveManualField(contract.getContractId(), "payment_terms", "付款方式", req.getPaymentTerms());
        saveManualField(contract.getContractId(), "breach_liability", "违约责任", req.getBreachLiability());

        return ContractGenerateDto.ConfirmResp.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .versionId(version.getVersionId())
                .status(contract.getStatus())
                .build();
    }

    /**
     * =========================
     * 5. 新增逻辑：合同列表
     * =========================
     */
    @Override
    public Map<String, Object> getContracts(int page, int size, String keyword, String status) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), size);

        Page<Contract> contractPage;

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
        boolean hasStatus = status != null && !status.trim().isEmpty();

        if (hasKeyword && hasStatus) {
            contractPage = contractRepository.findByTitleContainingIgnoreCaseAndStatus(
                    keyword.trim(), status.trim(), pageable
            );
        } else if (hasKeyword) {
            contractPage = contractRepository.findByTitleContainingIgnoreCase(
                    keyword.trim(), pageable
            );
        } else if (hasStatus) {
            contractPage = contractRepository.findByStatus(
                    status.trim(), pageable
            );
        } else {
            contractPage = contractRepository.findAll(pageable);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", contractPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("records", contractPage.getContent());

        return result;
    }

    /**
     * =========================
     * 6. 新增逻辑：合同详情
     * =========================
     */
    @Override
    public Contract getContractDetail(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在，contractId=" + contractId));
    }

    /**
     * =========================
     * 7. 你原有逻辑：上传校验
     * =========================
     */
    private void validateUpload(MultipartFile file, String title, String contractType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("合同标题不能为空");
        }
        if (contractType == null || contractType.isBlank()) {
            throw new IllegalArgumentException("合同类型不能为空");
        }

        if (file.getSize() > uploadProperties.getMaxSize()) {
            throw new IllegalArgumentException("文件大小超过限制");
        }

        String extension = FileTypeUtil.getExtension(file.getOriginalFilename());
        Set<String> allowed = new HashSet<>(uploadProperties.getAllowedExtensions());
        if (!FileTypeUtil.isAllowedExtension(extension, allowed)) {
            throw new IllegalArgumentException("仅支持 PDF/DOC/DOCX 格式");
        }

        if (!FileTypeUtil.isAllowedContentType(file)) {
            throw new IllegalArgumentException("文件 MIME 类型不合法");
        }
    }

    /**
     * =========================
     * 8. 新增逻辑：生成草案请求校验
     * =========================
     */
    private void validateGenerateReq(ContractGenerateDto.GenerateReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (req.getTemplateId() == null) {
            throw new IllegalArgumentException("模板ID不能为空");
        }
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new IllegalArgumentException("合同标题不能为空");
        }
        if (req.getPartyA() == null || req.getPartyA().isBlank()) {
            throw new IllegalArgumentException("甲方不能为空");
        }
        if (req.getPartyB() == null || req.getPartyB().isBlank()) {
            throw new IllegalArgumentException("乙方不能为空");
        }
    }

    /**
     * =========================
     * 9. 新增逻辑：优先调用 LLM，失败则本地兜底
     * =========================
     */
    private String generateByLlmOrFallback(Template template, ContractGenerateDto.GenerateReq req) {
        String llmApiUrl = System.getenv("LLM_API_URL");
        String llmApiKey = System.getenv("LLM_API_KEY");

        if (llmApiUrl == null || llmApiUrl.isBlank()) {
            return buildDraftFallback(template, req);
        }

        try {
            String prompt = buildPrompt(template, req);

            String requestBody = """
                    {
                      "model": "gpt-4o-mini",
                      "messages": [
                        {
                          "role": "system",
                          "content": "你是物流合同生成助手。请基于模板和用户输入，生成正式、完整、结构清晰的中文合同草案。不得编造明显缺失的关键事实；无法确定的内容用【待确认】标记。"
                        },
                        {
                          "role": "user",
                          "content": %s
                        }
                      ],
                      "temperature": 0.2
                    }
                    """.formatted(toJsonString(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(llmApiUrl))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + llmApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                String text = extractLlmText(response.body());
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        } catch (Exception e) {
            System.err.println("调用LLM失败，转本地兜底生成：" + e.getMessage());
        }

        return buildDraftFallback(template, req);
    }

    /**
     * =========================
     * 10. 新增逻辑：构造 Prompt
     * =========================
     */
    private String buildPrompt(Template template, ContractGenerateDto.GenerateReq req) {
        return """
                请根据以下合同模板和输入信息，生成一份正式的物流合同草案。

                【模板名称】
                %s

                【合同类型】
                %s

                【模板内容】
                %s

                【用户输入关键要素】
                合同标题：%s
                甲方：%s
                乙方：%s
                合同金额：%s
                签署日期：%s
                生效日期：%s
                到期日期：%s
                服务内容：%s
                付款方式：%s
                违约责任：%s
                补充要求：%s

                要求：
                1. 输出完整中文合同草案；
                2. 保留正式合同语气；
                3. 若信息不足，不要虚构，使用【待确认】；
                4. 条款应尽量结构化，如：合同主体、服务内容、金额与支付、履约要求、违约责任、争议解决等。
                """.formatted(
                nullToEmpty(template.getName()),
                nullToEmpty(template.getContractType()),
                nullToEmpty(template.getContent()),
                nullToEmpty(req.getTitle()),
                nullToEmpty(req.getPartyA()),
                nullToEmpty(req.getPartyB()),
                nullToEmpty(req.getAmount()),
                nullToEmpty(req.getSignDate()),
                nullToEmpty(req.getEffectiveDate()),
                nullToEmpty(req.getExpireDate()),
                nullToEmpty(req.getServiceContent()),
                nullToEmpty(req.getPaymentTerms()),
                nullToEmpty(req.getBreachLiability()),
                nullToEmpty(req.getExtraRequirements())
        );
    }

    /**
     * =========================
     * 11. 新增逻辑：本地兜底生成
     * =========================
     */
    private String buildDraftFallback(Template template, ContractGenerateDto.GenerateReq req) {
        String content = template.getContent() == null ? "" : template.getContent();

        content = replaceVar(content, "title", req.getTitle());
        content = replaceVar(content, "partyA", req.getPartyA());
        content = replaceVar(content, "partyB", req.getPartyB());
        content = replaceVar(content, "amount", req.getAmount());
        content = replaceVar(content, "signDate", req.getSignDate());
        content = replaceVar(content, "effectiveDate", req.getEffectiveDate());
        content = replaceVar(content, "expireDate", req.getExpireDate());
        content = replaceVar(content, "serviceContent", req.getServiceContent());
        content = replaceVar(content, "paymentTerms", req.getPaymentTerms());
        content = replaceVar(content, "breachLiability", req.getBreachLiability());

        StringBuilder sb = new StringBuilder();
        sb.append(req.getTitle()).append("\n\n");

        if (!content.isBlank()) {
            sb.append(content).append("\n\n");
        }

        sb.append("甲方：").append(nullOrPending(req.getPartyA())).append("\n");
        sb.append("乙方：").append(nullOrPending(req.getPartyB())).append("\n");
        sb.append("合同金额：").append(nullOrPending(req.getAmount())).append("\n");
        sb.append("签署日期：").append(nullOrPending(req.getSignDate())).append("\n");
        sb.append("生效日期：").append(nullOrPending(req.getEffectiveDate())).append("\n");
        sb.append("到期日期：").append(nullOrPending(req.getExpireDate())).append("\n\n");

        sb.append("一、服务内容\n");
        sb.append(nullOrPending(req.getServiceContent())).append("\n\n");

        sb.append("二、付款方式\n");
        sb.append(nullOrPending(req.getPaymentTerms())).append("\n\n");

        sb.append("三、违约责任\n");
        sb.append(nullOrPending(req.getBreachLiability())).append("\n\n");

        if (req.getExtraRequirements() != null && !req.getExtraRequirements().isBlank()) {
            sb.append("四、补充约定\n");
            sb.append(req.getExtraRequirements().trim()).append("\n\n");
        }

        sb.append("本合同由双方在平等、自愿的基础上签订，具体未尽事宜由双方协商确定。\n");

        return sb.toString().trim();
    }

    /**
     * =========================
     * 12. 新增逻辑：保存人工确认字段
     * =========================
     */
    private void saveManualField(Long contractId, String fieldKey, String fieldName, String fieldValue) {
        if (fieldValue == null || fieldValue.isBlank()) {
            return;
        }

        ContractField field = ContractField.builder()
                .contractId(contractId)
                .fieldKey(fieldKey)
                .fieldName(fieldName)
                .fieldValue(fieldValue.trim())
                .sourceRef("manual_generate")
                .confidence(1.0)
                .updatedBy(1L)
                .updatedAt(LocalDateTime.now())
                .build();

        contractFieldRepository.save(field);
    }

    /**
     * =========================
     * 13. 你原有逻辑：生成唯一合同号
     * =========================
     */
    private String generateUniqueContractNo() {
        String contractNo;
        do {
            contractNo = ContractNoGenerator.generate();
        } while (contractRepository.existsByContractNo(contractNo));
        return contractNo;
    }

    /**
     * =========================
     * 14. 新增工具方法
     * =========================
     */
    private String replaceVar(String text, String key, String value) {
        String v = nullOrPending(value);
        return text
                .replace("${" + key + "}", v)
                .replace("{{" + key + "}}", v);
    }

    private String nullOrPending(String s) {
        return (s == null || s.isBlank()) ? "【待确认】" : s.trim();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("生成内容摘要失败", e);
        }
    }

    private String toJsonString(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t") + "\"";
    }

    /**
     * 按常见 LLM 返回格式做简单提取
     */
    private String extractLlmText(String body) {
        if (body == null || body.isBlank()) return null;

        String marker = "\"content\":\"";
        int idx = body.indexOf(marker);
        if (idx < 0) return null;

        int start = idx + marker.length();
        StringBuilder sb = new StringBuilder();
        boolean escape = false;

        for (int i = start; i < body.length(); i++) {
            char c = body.charAt(i);

            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                break;
            }

            sb.append(c);
        }

        return sb.toString();
    }
}