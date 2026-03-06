package com.contract.contract_backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {
    String uploadFile(MultipartFile file, String objectKey);
}