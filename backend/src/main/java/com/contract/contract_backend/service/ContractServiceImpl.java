package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.config.ContractUploadProperties;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractVersion;
import com.contract.contract_backend.repository.ContractRepository;
import com.contract.contract_backend.repository.ContractVersionRepository;
import com.contract.contract_backend.service.ContractService;
import com.contract.contract_backend.service.MinioService;
import com.contract.contract_backend.common.utils.ContractNoGenerator;
import com.contract.contract_backend.common.utils.FileTypeUtil;
import com.contract.contract_backend.common.utils.HashUtil;
import com.contract.contract_backend.common.utils.ObjectKeyUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ContractVersionRepository contractVersionRepository;
    private final MinioService minioService;
    private final ContractUploadProperties uploadProperties;

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

        String savedObjectKey = minioService.uploadFile(file, objectKey);

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

        return ContractUploadResponse.builder()
                .contractId(contract.getContractId())
                .contractNo(contract.getContractNo())
                .versionId(version.getVersionId())
                .status(contract.getStatus())
                .build();
    }

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

    private String generateUniqueContractNo() {
        String contractNo;
        do {
            contractNo = ContractNoGenerator.generate();
        } while (contractRepository.existsByContractNo(contractNo));
        return contractNo;
    }
}