package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.ContractFieldResponse;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.contract.contract_backend.dto.ContractGenerateDto;

import java.util.List;

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

    @PostMapping("/generate-draft")
    public Result<ContractGenerateDto.GenerateResp> generateDraft(
            @RequestBody ContractGenerateDto.GenerateReq req
    ) {
        return Result.success(contractService.generateDraft(req));
    }

    @PostMapping("/confirm-generated")
    public Result<ContractGenerateDto.ConfirmResp> confirmGenerated(
            @RequestBody ContractGenerateDto.ConfirmReq req
    ) {
        return Result.success(contractService.confirmGeneratedContract(req));
    }

    @GetMapping("/{contractId}/fields")
    public Result<List<ContractFieldResponse>> getContractFields(@PathVariable Long contractId) {
        return Result.success(contractService.getContractFields(contractId));
    }
}