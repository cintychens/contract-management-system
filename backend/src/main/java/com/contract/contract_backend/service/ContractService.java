package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.ContractUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ContractService {
    ContractUploadResponse uploadContract(MultipartFile file, String title, String contractType);
}