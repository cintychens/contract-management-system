package com.contract.contract_backend.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * 上传文件到存储系统（本地文件存储）
     * @param file 上传文件
     * @param objectKey 相对路径
     * @return 存储后的相对路径
     */
    String uploadFile(MultipartFile file, String objectKey);
}