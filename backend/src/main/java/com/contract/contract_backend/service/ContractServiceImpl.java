package com.contract.contract_backend.service;

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
import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.entity.ContractFlowRecord;
import com.contract.contract_backend.entity.RoleCode;
import com.contract.contract_backend.repository.ContractFlowRecordRepository;
import com.contract.contract_backend.service.ContractMilestoneService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final ContractFlowRecordRepository contractFlowRecordRepository;
    private final ContractMilestoneService milestoneService;

    /**
     * =========================
     * 1. 你原有逻辑：合同文件上传
     * =========================
     */
    @Override
    @Transactional
    public ContractUploadResponse uploadContract(MultipartFile file, String title, String contractType) {
        // ⭐ 权限控制（放在方法最前面）
        String role = getCurrentUserRole();

        if (!RoleCode.BUSINESS.equals(role) && !RoleCode.ADMIN.equals(role)) {
            throw new RuntimeException("无权限创建合同");
        }
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
        String extractedContent = extractContentForPreview(file, title, contractType);

        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .title(title)
                .contractType(contractType)
                .status(ContractStatus.DRAFT)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .currentHandlerRole(RoleCode.BUSINESS)
                .currentHandlerId(1L)
                .content(extractedContent)
                .build();

        contract = contractRepository.save(contract);
        milestoneService.initMilestones(
                contract.getContractId(),
                LocalDate.now()
        );

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

        return ContractUploadResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .versionId(version.getVersionId())
                .status(ContractStatus.DRAFT)
                .build();
    }

    /**
     * =========================
     * 2. 你原有逻辑：获取解析字段
     * =========================
     */
    @Override
    public List<ContractFieldResponse> getContractFields(Long contractId) {
        List<ContractField> fields = contractFieldRepository.findByContractIdOrderBySortOrderAsc(contractId);

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

        // ⭐ 权限
        String role = getCurrentUserRole();
        if (!RoleCode.BUSINESS.equals(role) && !RoleCode.ADMIN.equals(role)) {
            throw new RuntimeException("无权限创建合同");
        }

        // ⭐ 参数校验
        if (req == null) throw new IllegalArgumentException("请求不能为空");
        if (req.getTitle() == null || req.getTitle().isBlank())
            throw new IllegalArgumentException("合同标题不能为空");
        if (req.getContractType() == null || req.getContractType().isBlank())
            throw new IllegalArgumentException("合同类型不能为空");
        if (req.getDraftContent() == null || req.getDraftContent().isBlank())
            throw new IllegalArgumentException("合同草案不能为空");

        String contractNo = generateUniqueContractNo();

        // ⭐ 创建合同
        Contract contract = Contract.builder()
                .contractNo(contractNo)
                .title(req.getTitle().trim())
                .contractType(req.getContractType().trim())
                .templateId(req.getTemplateId())
                .status(ContractStatus.DRAFT)
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .currentHandlerRole(RoleCode.BUSINESS)
                .currentHandlerId(1L)
                .content(req.getDraftContent().trim())
                .build();

        contract = contractRepository.save(contract);

        milestoneService.initMilestones(contract.getContractId(), LocalDate.now());

        String content = req.getDraftContent().trim();

// ⭐⭐⭐ 用用户填写的字段替换模板变量
        Map<String, Object> inputFields = req.getFields();

        if (inputFields != null) {
            for (Map.Entry<String, Object> entry : inputFields.entrySet()) {

                String key = entry.getKey();
                String value = entry.getValue() == null ? "" : entry.getValue().toString();

                if (!value.isBlank()) {
                    content = content.replace("${" + key + "}", value);

                    content = content.replaceAll("_{2,}", "【待确认】");
                }
            }
        }

        // ⭐ 版本
        ContractVersion version = ContractVersion.builder()
                .contractId(contract.getContractId())
                .versionNo(1)
                .fileName(req.getTitle().trim() + ".txt")
                .fileType("GENERATED")
                .fileSize((long) content.getBytes(StandardCharsets.UTF_8).length)
                .fileObjectKey(null)
                .fileHash(sha256(content))
                .contentText(content)
                .changeNote("AI生成草案并人工确认保存")
                .createdBy(1L)
                .createdAt(LocalDateTime.now())
                .build();

        version = contractVersionRepository.save(version);

        contract.setCurrentVersionId(version.getVersionId());
        contract.setContent(content);
        contractRepository.save(contract);

        // =========================
        // ⭐⭐⭐ 核心：字段处理
        // =========================

        Long contractId = contract.getContractId();

        // ⭐⭐⭐ 第一步：调用解析（NLP）
        contractParseService.parseContract(contractId);

// ⭐⭐⭐ 第二步：拿解析结果
        List<ContractField> parsedFields =
                contractFieldRepository.findByContractIdOrderBySortOrderAsc(contractId);

// ⭐⭐⭐ 转成 map
        Map<String, Object> parsedMap = new HashMap<>();
        for (ContractField f : parsedFields) {
            if (f.getFieldValue() != null && !f.getFieldValue().isBlank()) {
                parsedMap.put(f.getFieldKey(), f.getFieldValue());
            }
        }

        // ⭐ 1. 获取模板
        Template template = templateRepository.findById(req.getTemplateId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        // ⭐ 2. 提取所有字段 key
        Set<String> templateKeys = extractFieldKeys(template.getContent());

        // ⭐ 3. 提取中文 label
        Map<String, String> labelMap = extractFieldLabels(template.getContent());

        // ⭐ 5. 合并字段（核心！！！）
        Map<String, Object> finalFields = new LinkedHashMap<>();

// ⭐ 优先级：用户输入 > NLP解析 > 默认待确认

// 1️⃣ NLP解析结果
        if (parsedMap != null) {
            finalFields.putAll(parsedMap);
        }

// 2️⃣ 用户输入覆盖（最高优先级）
        if (inputFields != null) {
            inputFields.forEach((k, v) -> {
                if (v != null && !v.toString().isBlank()) {
                    finalFields.put(k, v);
                }
            });
        }

// 3️⃣ 补齐缺失字段
        for (String key : templateKeys) {
            finalFields.putIfAbsent(key, "【待确认】");
        }

        // ⭐ 清空旧字段
        contractFieldRepository.deleteByContractId(contractId);

        LocalDateTime now = LocalDateTime.now();

        // ⭐ 保存
        // ⭐ 保存字段
        finalFields.forEach((key, value) -> {

            String strValue = value == null ? "" : value.toString().trim();

            // 没有值就统一显示【待确认】
            if (strValue.isBlank()) {
                strValue = "【待确认】";
            }

            // 判断是不是用户真实填写
            boolean hasRealValue = !"【待确认】".equals(strValue);

            ContractField field = ContractField.builder()
                    .contractId(contractId)
                    .fieldKey(key)
                    .fieldName(labelMap.getOrDefault(key, key))
                    .fieldValue(strValue)
                    .sourceRef(hasRealValue ? "form_input" : "pending")
                    .confidence(hasRealValue ? 1.0 : 0.0)
                    .updatedBy(1L)
                    .updatedAt(now)
                    .build();

            contractFieldRepository.save(field);
        });

        return ContractGenerateDto.ConfirmResp.builder()
                .contractId(contractId)
                .contractNo(contract.getContractNo())
                .versionId(version.getVersionId())
                .status(contract.getStatus())
                .build();
    }

    private Set<String> extractFieldKeys(String content) {
        Set<String> keys = new LinkedHashSet<>();
        Matcher m = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}").matcher(content);
        while (m.find()) {
            keys.add(m.group(1));
        }
        return keys;
    }

    private Map<String, String> extractFieldLabels(String content) {

        Map<String, String> map = new LinkedHashMap<>();

        // 1️⃣ 先收集全部 key
        Pattern keyPattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
        Matcher keyMatcher = keyPattern.matcher(content);

        while (keyMatcher.find()) {
            map.putIfAbsent(keyMatcher.group(1), keyMatcher.group(1));
        }

        // 2️⃣ 再匹配中文 label
        Pattern labelPattern = Pattern.compile("([^\\n：:]{2,})[：:]\\s*\\$\\{([a-zA-Z0-9_]+)}");
        Matcher labelMatcher = labelPattern.matcher(content);

        while (labelMatcher.find()) {
            String label = labelMatcher.group(1).trim();
            String key = labelMatcher.group(2).trim();

            map.put(key, label);
        }

        return map;
    }
    /**
     * =========================
     * 5. 新增逻辑：合同列表
     * =========================
     */
    @Override
    public Map<String, Object> getContracts(int page, int size, String keyword, String status) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());

        Page<Contract> contractPage;

        String kw = (keyword == null || keyword.trim().isEmpty()) ? null : keyword.trim();
        String st = (status == null || status.trim().isEmpty()) ? null : status.trim();

        if (kw != null && st != null) {
            contractPage = contractRepository.search(kw, st, pageable);
        } else if (kw != null) {
            contractPage = contractRepository.search(kw, null, pageable);
        } else if (st != null) {
            contractPage = contractRepository.search(null, st, pageable);
        } else {
            contractPage = contractRepository.findAll(pageable);
        }

        // ⭐⭐⭐ 核心修改：转换数据
        List<Map<String, Object>> records = contractPage.getContent().stream().map(contract -> {

            String type = contract.getContractType();

            String typeName;

            switch (type) {
                case "transport_a":
                    typeName = "运输合同A类";
                    break;
                case "transport_b":
                    typeName = "运输合同B类";
                    break;
                case "transport_c":
                    typeName = "运输合同C类";
                    break;

                case "warehouse_a":
                    typeName = "仓储合同A类";
                    break;
                case "warehouse_b":
                    typeName = "仓储合同B类";
                    break;
                case "warehouse_c":
                    typeName = "仓储合同C类";
                    break;

                default:
                    typeName = type;
                    break;
            }

            Map<String, Object> map = new HashMap<>();
            map.put("contractId", contract.getContractId());
            map.put("contractNo", contract.getContractNo());
            map.put("title", contract.getTitle());
            map.put("contractType", type);
            map.put("contractTypeName", typeName); // ⭐⭐⭐ 关键
            map.put("status", contract.getStatus());
            map.put("createdAt", contract.getCreatedAt());

            return map;
        }).toList();

        Map<String, Object> result = new HashMap<>();
        result.put("records", records); // ⭐ 改这里
        result.put("total", contractPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", contractPage.getTotalPages());

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

    @Override
    @Transactional
    public void submitForLegalReview(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.DRAFT.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不是草稿状态，不能提交审批");
        }

        String fromStatus = contract.getStatus();
        String fromRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_LEGAL);
        contract.setCurrentHandlerRole(RoleCode.LEGAL);
        contract.setCurrentHandlerId(null);
        contract.setSubmittedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.PENDING_LEGAL,
                fromRole,
                RoleCode.LEGAL,
                "SUBMIT",
                operatorId,
                comment
        );
    }

    @Override
    @Transactional
    public void approveByLegal(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_LEGAL.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在法务审核阶段");
        }

        String fromStatus = contract.getStatus();

        contract.setStatus(ContractStatus.PENDING_FINANCE);
        contract.setCurrentHandlerRole(RoleCode.FINANCE);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.PENDING_FINANCE,
                RoleCode.LEGAL,
                RoleCode.FINANCE,
                "APPROVE",
                operatorId,
                comment
        );
    }

    @Override
    @Transactional
    public void rejectByLegal(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_LEGAL.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在法务审核阶段");
        }

        String fromStatus = contract.getStatus();

        contract.setStatus(ContractStatus.DRAFT);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.DRAFT,
                RoleCode.LEGAL,
                RoleCode.BUSINESS,
                "REJECT",
                operatorId,
                comment
        );
    }

    @Override
    @Transactional
    public void approveByFinance(Long contractId, Long operatorId, String comment) {

        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_FINANCE.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在财务审核阶段");
        }

        String fromStatus = contract.getStatus();

        boolean isTransportC =
                "transport_c".equalsIgnoreCase(contract.getContractType());

        if (isTransportC) {

            // ✅ C类 → 走审批人
            contract.setStatus(ContractStatus.PENDING_APPROVAL);
            contract.setCurrentHandlerRole(RoleCode.APPROVER);
            contract.setCurrentHandlerId(null);

        } else {

            // ✅ A/B类 → 直接生效
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setCurrentHandlerRole(RoleCode.BUSINESS);
            contract.setCurrentHandlerId(contract.getCreatedBy());
            contract.setApprovedAt(LocalDateTime.now());
        }

        // ⭐⭐⭐⭐⭐ 这一行是关键！！！
        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                isTransportC ? ContractStatus.PENDING_APPROVAL : ContractStatus.ACTIVE,
                RoleCode.FINANCE,
                isTransportC ? RoleCode.APPROVER : RoleCode.BUSINESS,
                "APPROVE",
                operatorId,
                comment
        );
    }

    @Override
    @Transactional
    public void rejectByFinance(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_FINANCE.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在财务审核阶段");
        }

        String fromStatus = contract.getStatus();

        contract.setStatus(ContractStatus.DRAFT);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.DRAFT,
                RoleCode.FINANCE,
                RoleCode.BUSINESS,
                "REJECT",
                operatorId,
                comment
        );
    }

    @Override
    @Transactional
    public void approveByApprover(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_APPROVAL.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在最终审批阶段");
        }

        String fromStatus = contract.getStatus();

        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());
        contract.setApprovedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.ACTIVE,
                RoleCode.APPROVER,
                RoleCode.BUSINESS,
                "APPROVE",
                operatorId,
                comment
        );
    }
    @Override
    @Transactional
    public void rejectByApprover(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);

        if (!ContractStatus.PENDING_APPROVAL.equals(contract.getStatus())) {
            throw new RuntimeException("当前合同不在最终审批阶段");
        }

        String fromStatus = contract.getStatus();

        contract.setStatus(ContractStatus.DRAFT);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                fromStatus,
                ContractStatus.DRAFT,
                RoleCode.APPROVER,
                RoleCode.BUSINESS,
                "REJECT",
                operatorId,
                comment
        );
    }

    @Override
    public List<ContractFlowRecord> getFlowRecords(Long contractId) {
        getContractOrThrow(contractId);
        return contractFlowRecordRepository.findByContractIdOrderByCreatedAtAsc(contractId);
    }

    private Contract getContractOrThrow(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在，ID=" + contractId));
    }

    private void saveFlowRecord(Long contractId,
                                String fromStatus,
                                String toStatus,
                                String fromRole,
                                String toRole,
                                String actionType,
                                Long operatorId,
                                String comment) {
        ContractFlowRecord record = ContractFlowRecord.builder()
                .contractId(contractId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromRole(fromRole)
                .toRole(toRole)
                .actionType(actionType)
                .operatorId(operatorId == null ? 0L : operatorId)
                .comment(comment)
                .build();

        contractFlowRecordRepository.save(record);
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
                            货物名称：%s
                            货物类别：%s
                            货物数量：%s
                            特殊要求：%s
                            仓储地址：%s
                            入库日期：%s
                            出库日期：%s
                            仓储期限：%s
                
                            起运仓库：%s
                            配送区域：%s
                            配送地址：%s
                            配送方式：%s
                            时效要求：%s
                            服务期限：%s
                            单件重量限制：%s
                            单件体积限制：%s
                            投诉索赔期限：%s
                            提货时限：%s
                            保管费标准：%s
                            配送时效：%s
                            保险方式：%s
                            滞纳金比例：%s
                
                            合同金额：%s
                            付款方式：%s
                            付款期限：%s
                            争议法院：%s
                            签署日期：%s
                            生效日期：%s
                            到期日期：%s
                            服务内容：%s
                            违约责任：%s
                            补充要求：%s
                            
                            甲方地址：%s
                            甲方法定代表人：%s
                            甲方联系电话：%s
                            乙方地址：%s
                            乙方法定代表人：%s
                            乙方联系电话：%s
                            服务范围说明：%s
                            服务标准：%s
                            服务期间说明：%s
                            费用构成：%s
                            付款日期：%s
                            异常处理机制：%s
                            违约金比例：%s

            要求：
            1. 输出完整中文合同草案；
            2. 保留正式合同语气；
            3. 若信息不足，不要虚构，使用【待确认】；
            4. 条款应尽量结构化，如：合同主体、货物信息、仓储地点与期限、费用结算、双方责任、违约责任、争议解决等。
            """.formatted(
                nullToEmpty(template.getName()),
                nullToEmpty(template.getContractType()),
                nullToEmpty(template.getContent()),
                nullToEmpty(req.getTitle()),
                nullToEmpty(req.getPartyA()),
                nullToEmpty(req.getPartyB()),
                nullToEmpty(req.getCargoName()),
                nullToEmpty(req.getCargoCategory()),
                nullToEmpty(req.getCargoQuantity()),
                nullToEmpty(req.getSpecialRequirement()),
                nullToEmpty(req.getWarehouseAddress()),
                nullToEmpty(req.getInboundDate()),
                nullToEmpty(req.getOutboundDate()),
                nullToEmpty(req.getStoragePeriod()),
                nullToEmpty(req.getAmount()),
                nullToEmpty(req.getPaymentMethod()),
                nullToEmpty(req.getPaymentTerm()),
                nullToEmpty(req.getDisputeCourt()),
                nullToEmpty(req.getSignDate()),
                nullToEmpty(req.getEffectiveDate()),
                nullToEmpty(req.getExpireDate()),
                nullToEmpty(req.getServiceContent()),
                nullToEmpty(req.getBreachLiability()),
                nullToEmpty(req.getExtraRequirements()),
                nullToEmpty(req.getOriginWarehouse()),
                nullToEmpty(req.getDeliveryArea()),
                nullToEmpty(req.getDeliveryAddress()),
                nullToEmpty(req.getDeliveryMode()),
                nullToEmpty(req.getDeliveryTimeRequirement()),
                nullToEmpty(req.getServicePeriod()),
                nullToEmpty(req.getSingleWeightLimit()),
                nullToEmpty(req.getSingleVolumeLimit()),
                nullToEmpty(req.getClaimPeriod()),
                nullToEmpty(req.getPickupTimeLimit()),
                nullToEmpty(req.getStorageFeeStandard()),
                nullToEmpty(req.getDeliveryTimeStandard()),
                nullToEmpty(req.getInsuranceOption()),
                nullToEmpty(req.getPenaltyRate()),
                nullToEmpty(req.getPartyAAddress()),
                nullToEmpty(req.getPartyALegalPerson()),
                nullToEmpty(req.getPartyAPhone()),
                nullToEmpty(req.getPartyBAddress()),
                nullToEmpty(req.getPartyBLegalPerson()),
                nullToEmpty(req.getPartyBPhone()),
                nullToEmpty(req.getServiceScope()),
                nullToEmpty(req.getServiceStandard()),
                nullToEmpty(req.getServicePeriod()),
                nullToEmpty(req.getFeeStructure()),
                nullToEmpty(req.getPaymentDate()),
                nullToEmpty(req.getExceptionHandling()),
                nullToEmpty(req.getPenaltyRate())
        );
    }

    /**
     * =========================
     * 11. 新增逻辑：本地兜底生成
     * =========================
     */
    private String buildDraftFallback(Template template, ContractGenerateDto.GenerateReq req) {
        String content = template.getContent() == null ? "" : template.getContent();

        // 通用字段
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
        content = replaceVar(content, "extraRequirements", req.getExtraRequirements());

        // 仓储类模板字段
        content = replaceVar(content, "cargoName", req.getCargoName());
        content = replaceVar(content, "cargoCategory", req.getCargoCategory());
        content = replaceVar(content, "cargoQuantity", req.getCargoQuantity());
        content = replaceVar(content, "specialRequirement", req.getSpecialRequirement());
        content = replaceVar(content, "warehouseAddress", req.getWarehouseAddress());
        content = replaceVar(content, "inboundDate", req.getInboundDate());
        content = replaceVar(content, "outboundDate", req.getOutboundDate());
        content = replaceVar(content, "storagePeriod", req.getStoragePeriod());
        content = replaceVar(content, "paymentMethod", req.getPaymentMethod());
        content = replaceVar(content, "paymentTerm", req.getPaymentTerm());
        content = replaceVar(content, "disputeCourt", req.getDisputeCourt());

        // 外包类模板字段
        content = replaceVar(content, "partyAAddress", req.getPartyAAddress());
        content = replaceVar(content, "partyALegalPerson", req.getPartyALegalPerson());
        content = replaceVar(content, "partyAPhone", req.getPartyAPhone());

        content = replaceVar(content, "partyBAddress", req.getPartyBAddress());
        content = replaceVar(content, "partyBLegalPerson", req.getPartyBLegalPerson());
        content = replaceVar(content, "partyBPhone", req.getPartyBPhone());

        content = replaceVar(content, "serviceScope", req.getServiceScope());
        content = replaceVar(content, "serviceStandard", req.getServiceStandard());
        content = replaceVar(content, "servicePeriod", req.getServicePeriod());

        content = replaceVar(content, "feeStructure", req.getFeeStructure());
        content = replaceVar(content, "paymentDate", req.getPaymentDate());

        content = replaceVar(content, "exceptionHandling", req.getExceptionHandling());
        content = replaceVar(content, "penaltyRate", req.getPenaltyRate());

        StringBuilder sb = new StringBuilder();

        if (req.getTitle() != null && !req.getTitle().isBlank()) {
            sb.append(req.getTitle().trim()).append("\n\n");
        }

        if (!content.isBlank()) {
            sb.append(content.trim());
        }

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
     * 上传后用于详情页展示的正文预览（临时占位）
     */
    private String extractContentForPreview(MultipartFile file, String title, String contractType) {
        try {
            String originalFileName = file.getOriginalFilename();
            String extension = FileTypeUtil.getExtension(originalFileName).toLowerCase();

            // 如果以后允许 txt，这里可以直接读取正文
            if ("txt".equals(extension)) {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            // 当前阶段先给一个可展示的占位正文
            return """
                合同标题：%s
                合同类型：%s

                该合同文件已上传成功。
                当前系统已完成合同主记录与版本记录保存。

                文件名称：%s
                说明：当前正文尚未从原始文件中完整提取，后续可由解析服务补充真实正文内容。
                """.formatted(
                    title == null ? "" : title,
                    contractType == null ? "" : contractType,
                    originalFileName == null ? "" : originalFileName
            );
        } catch (Exception e) {
            return "合同已上传成功，但正文提取失败：" + e.getMessage();
        }
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

    private String getCurrentUserRole() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities().isEmpty()) {
            throw new RuntimeException("未获取到用户角色");
        }

        return auth.getAuthorities().iterator().next().getAuthority();
    }
}
