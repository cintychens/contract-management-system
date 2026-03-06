package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping("/upload")
    public Result<ContractUploadResponse> uploadContract(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("contractType") String contractType
    ) {
        ContractUploadResponse response = contractService.uploadContract(file, title, contractType);
        return Result.success(response);
    }
}