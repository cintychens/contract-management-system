package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.config.LocalFileStorageProperties;
import com.contract.contract_backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final LocalFileStorageProperties storageProperties;

    @Override
    public String uploadFile(MultipartFile file, String objectKey) {
        try {
            String baseDir = storageProperties.getBaseDir();

            if (baseDir == null || baseDir.isBlank()) {
                throw new RuntimeException("本地文件存储目录未配置");
            }

            Path fullPath = Paths.get(baseDir, objectKey).normalize();
            Path parent = fullPath.getParent();

            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            file.transferTo(fullPath.toFile());

            return objectKey;

        } catch (IOException e) {
            throw new RuntimeException("保存文件到本地目录失败", e);
        }
    }
}