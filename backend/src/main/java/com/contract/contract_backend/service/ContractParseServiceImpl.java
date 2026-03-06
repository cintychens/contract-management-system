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

@Service
@RequiredArgsConstructor
public class ContractParseServiceImpl implements ContractParseService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final ContractFieldRepository contractFieldRepository;
    private final LocalFileStorageProperties localFileStorageProperties;

    @Override
    public void parseContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        if (contract.getCurrentVersionId() == null) {
            throw new RuntimeException("合同当前版本不存在");
        }

        ContractVersion version = contractVersionRepository.findById(contract.getCurrentVersionId())
                .orElseThrow(() -> new RuntimeException("合同版本不存在"));

        String objectKey = version.getFileObjectKey();
        Path fullPath = LocalStoredFileUtil.buildFullPath(localFileStorageProperties.getBaseDir(), objectKey);

        if (!Files.exists(fullPath)) {
            throw new RuntimeException("本地合同文件不存在：" + fullPath);
        }

        contract.setStatus("PARSING");
        contractRepository.save(contract);

        String text = extractText(fullPath, version.getFileName());

        // 重新解析前，先清理旧字段
        contractFieldRepository.deleteByContractId(contractId);

        saveField(contractId, "party_a", "甲方名称", extractByRegex(text, "甲方[：: ]*([\\u4e00-\\u9fa5A-Za-z0-9（）()·\\-—_]+)"), 0.85);
        saveField(contractId, "party_b", "乙方名称", extractByRegex(text, "乙方[：: ]*([\\u4e00-\\u9fa5A-Za-z0-9（）()·\\-—_]+)"), 0.85);
        saveField(contractId, "amount", "合同金额", extractByRegex(text, "合同金额[：: ]*([0-9,.]+\\s*元?)"), 0.80);
        saveField(contractId, "sign_date", "签署日期", extractByRegex(text, "(\\d{4}[-年/.]\\d{1,2}[-月/.]\\d{1,2}日?)"), 0.75);

        contract.setStatus("PARSED");
        contractRepository.save(contract);
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
}