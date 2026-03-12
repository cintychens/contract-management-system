package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.ContractFieldResponse;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.contract.contract_backend.dto.ContractGenerateDto;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.service.ContractExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final ContractExportService contractExportService;

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

    @GetMapping
    public Result<Map<String, Object>> getContracts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return Result.success(contractService.getContracts(page, size, keyword, status));
    }

    @GetMapping("/{contractId}")
    public Result<Contract> getContractDetail(@PathVariable Long contractId) {
        return Result.success(contractService.getContractDetail(contractId));
    }

    @GetMapping("/{contractId}/export/word")
    public ResponseEntity<byte[]> exportWord(@PathVariable Long contractId) {
        Contract contract = contractService.getContractDetail(contractId);
        byte[] data = contractExportService.exportWord(contractId);

        String fileName = buildFileName(contract.getTitle(), ".docx");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                ))
                .body(data);
    }

    @GetMapping("/{contractId}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long contractId) {
        Contract contract = contractService.getContractDetail(contractId);
        byte[] data = contractExportService.exportPdf(contractId);

        String fileName = buildFileName(contract.getTitle(), ".pdf");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    private String buildFileName(String title, String suffix) {
        String safeTitle = (title == null || title.isBlank()) ? "合同文件" : title.trim();
        return URLEncoder.encode(safeTitle + suffix, StandardCharsets.UTF_8);
    }
}