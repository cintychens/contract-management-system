package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminTemplateDto;
import com.contract.contract_backend.dto.PageResult;
import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.entity.TemplateField;
import com.contract.contract_backend.entity.TemplateFieldBind;
import com.contract.contract_backend.repository.TemplateFieldBindRepository;
import com.contract.contract_backend.repository.TemplateFieldRepository;
import com.contract.contract_backend.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final TemplateFieldRepository templateFieldRepository;
    private final TemplateFieldBindRepository templateFieldBindRepository;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    @Override
    public PageResult<AdminTemplateDto.TemplateRow> pageTemplates(
            int page,
            int size,
            String keyword,
            String contractType,
            String status
    ) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        String kw = keyword == null ? "" : keyword.trim();
        String ct = contractType == null ? "" : contractType.trim();
        String st = status == null ? "" : status.trim().toUpperCase();

        boolean hasKw = !kw.isEmpty();
        boolean hasCt = !ct.isEmpty() && !"ALL".equalsIgnoreCase(ct);
        boolean hasSt = !st.isEmpty() && !"ALL".equalsIgnoreCase(st);

        Page<Template> p;

        if (hasKw && hasCt && hasSt) {
            p = templateRepository.findByNameContainingIgnoreCaseAndContractTypeAndStatus(kw, ct, st, pageable);
        } else if (hasKw && hasCt) {
            p = templateRepository.findByNameContainingIgnoreCaseAndContractType(kw, ct, pageable);
        } else if (hasKw && hasSt) {
            p = templateRepository.findByNameContainingIgnoreCaseAndStatus(kw, st, pageable);
        } else if (hasCt && hasSt) {
            p = templateRepository.findByContractTypeAndStatus(ct, st, pageable);
        } else if (hasKw) {
            p = templateRepository.findByNameContainingIgnoreCase(kw, pageable);
        } else if (hasCt) {
            p = templateRepository.findByContractType(ct, pageable);
        } else if (hasSt) {
            p = templateRepository.findByStatus(st, pageable);
        } else {
            p = templateRepository.findAll(pageable);
        }

        List<AdminTemplateDto.TemplateRow> rows = p.getContent().stream()
                .map(this::toRow)
                .toList();

        return PageResult.of(rows, p.getTotalElements(), page, pageSize);
    }

    @Override
    public AdminTemplateDto.TemplateDetail getTemplate(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));
        return toDetail(template);
    }

    @Override
    @Transactional
    public AdminTemplateDto.TemplateRow createTemplate(AdminTemplateDto.SaveReq req) {
        validateSaveReq(req);

        String name = req.getName().trim();
        String contractType = normalizeContractType(req.getContractType());

        if (templateRepository.existsByName(name)) {
            throw new IllegalArgumentException("模板名称已存在");
        }

        Template template = Template.builder()
                .name(name)
                .contractType(contractType)
                .content(req.getContent().trim())
                .remark(req.getRemark() == null ? null : req.getRemark().trim())
                .status(normalizeStatus(req.getStatus()))
                .updatedBy(req.getUpdatedBy() == null ? "admin" : req.getUpdatedBy().trim())
                .fileName(req.getFileName())
                .fileObjectKey(req.getFileObjectKey())
                .build();

        Template saved = templateRepository.save(template);

        // 新增：保存模板后，自动重建模板字段绑定关系
        rebuildTemplateFieldBinds(saved);

        return toRow(saved);
    }

    @Override
    @Transactional
    public AdminTemplateDto.TemplateRow updateTemplate(Long templateId, AdminTemplateDto.SaveReq req) {
        validateSaveReq(req);

        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        String name = req.getName().trim();
        String contractType = normalizeContractType(req.getContractType());

        if (templateRepository.existsByNameAndTemplateIdNot(name, templateId)) {
            throw new IllegalArgumentException("模板名称已存在");
        }

        template.setName(name);
        template.setContractType(contractType);
        template.setContent(req.getContent().trim());
        template.setRemark(req.getRemark() == null ? null : req.getRemark().trim());
        template.setStatus(normalizeStatus(req.getStatus()));
        template.setUpdatedBy(req.getUpdatedBy() == null ? "admin" : req.getUpdatedBy().trim());

        if (req.getFileName() != null && !req.getFileName().isBlank()) {
            template.setFileName(req.getFileName());
        }
        if (req.getFileObjectKey() != null && !req.getFileObjectKey().isBlank()) {
            template.setFileObjectKey(req.getFileObjectKey());
        }

        Template saved = templateRepository.save(template);

        // 新增：更新模板后，重新解析正文并重建字段绑定关系
        rebuildTemplateFieldBinds(saved);

        return toRow(saved);
    }

    @Override
    @Transactional
    public AdminTemplateDto.TemplateRow updateStatus(Long templateId, AdminTemplateDto.StatusReq req) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        template.setStatus(normalizeStatus(req.getStatus()));
        template.setUpdatedBy(req.getUpdatedBy() == null ? "admin" : req.getUpdatedBy().trim());

        Template saved = templateRepository.save(template);
        return toRow(saved);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        // 建议：先删绑定表，再删模板
        templateFieldBindRepository.deleteByTemplateId(templateId);
        templateRepository.delete(template);
    }

    @Override
    public AdminTemplateDto.Stats stats() {
        long total = templateRepository.count();
        long enabled = templateRepository.countByStatus("ENABLED");
        long disabled = templateRepository.countByStatus("DISABLED");

        return AdminTemplateDto.Stats.builder()
                .total(total)
                .enabled(enabled)
                .disabled(disabled)
                .build();
    }

    @Override
    public List<AdminTemplateDto.TemplateFieldRow> listTemplateFields(Long templateId) {
        if (!templateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }

        return templateFieldBindRepository.findByTemplateIdOrderBySortOrderAscIdAsc(templateId)
                .stream()
                .map(this::toFieldRow)
                .toList();
    }

    private void validateSaveReq(AdminTemplateDto.SaveReq req) {
        if (req == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }
        if (req.getContractType() == null || req.getContractType().isBlank()) {
            throw new IllegalArgumentException("合同类型不能为空");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new IllegalArgumentException("模板内容不能为空");
        }
    }

    private String normalizeStatus(String status) {
        String s = (status == null || status.isBlank()) ? "ENABLED" : status.trim().toUpperCase();
        if (!"ENABLED".equals(s) && !"DISABLED".equals(s)) {
            throw new IllegalArgumentException("模板状态不合法");
        }
        return s;
    }

    private String normalizeContractType(String contractType) {
        String s = contractType == null ? "" : contractType.trim().toLowerCase();
        if (!"transport".equals(s)
                && !"warehouse".equals(s)
                && !"supply".equals(s)
                && !"distribution".equals(s)
                && !"outsourcing".equals(s)) {
            throw new IllegalArgumentException("合同类型不合法");
        }
        return s;
    }

    private Set<String> extractFieldKeys(String content) {
        Set<String> fieldKeys = new LinkedHashSet<>();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(content);
        while (matcher.find()) {
            fieldKeys.add(matcher.group(1));
        }
        return fieldKeys;
    }

    @Transactional
    protected void rebuildTemplateFieldBinds(Template template) {
        templateFieldBindRepository.deleteByTemplateId(template.getTemplateId());

        Set<String> fieldKeys = extractFieldKeys(template.getContent());
        LocalDateTime now = LocalDateTime.now();

        for (String fieldKey : fieldKeys) {
            TemplateField field = templateFieldRepository.findByFieldKey(fieldKey)
                    .orElseThrow(() -> new IllegalArgumentException("模板中存在未定义字段: " + fieldKey));

            TemplateFieldBind bind = TemplateFieldBind.builder()
                    .templateId(template.getTemplateId())
                    .fieldKey(field.getFieldKey())
                    .fieldNameSnapshot(field.getFieldName())
                    .fieldTypeSnapshot(field.getFieldType())
                    .requiredFlag(field.getRequiredFlag())
                    .sortOrder(field.getSortOrder())
                    .status(field.getStatus() == null ? "ENABLED" : field.getStatus())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            templateFieldBindRepository.save(bind);
        }
    }

    private AdminTemplateDto.TemplateRow toRow(Template t) {
        return AdminTemplateDto.TemplateRow.builder()
                .templateId(t.getTemplateId())
                .name(t.getName())
                .contractType(t.getContractType())
                .status(t.getStatus())
                .remark(t.getRemark())
                .updatedBy(t.getUpdatedBy())
                .fileName(t.getFileName())
                .fileObjectKey(t.getFileObjectKey())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private AdminTemplateDto.TemplateDetail toDetail(Template t) {
        return AdminTemplateDto.TemplateDetail.builder()
                .templateId(t.getTemplateId())
                .name(t.getName())
                .contractType(t.getContractType())
                .content(t.getContent())
                .remark(t.getRemark())
                .status(t.getStatus())
                .updatedBy(t.getUpdatedBy())
                .fileName(t.getFileName())
                .fileObjectKey(t.getFileObjectKey())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    private AdminTemplateDto.TemplateFieldRow toFieldRow(TemplateFieldBind b) {
        return AdminTemplateDto.TemplateFieldRow.builder()
                .fieldId(null)
                .templateId(b.getTemplateId())
                .fieldKey(b.getFieldKey())
                .fieldName(b.getFieldNameSnapshot())
                .fieldType(b.getFieldTypeSnapshot())
                .requiredFlag(b.getRequiredFlag())
                .sortOrder(b.getSortOrder())
                .build();
    }
}