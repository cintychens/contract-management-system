package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.ContractFieldResponse;
import com.contract.contract_backend.dto.ContractUploadResponse;
import com.contract.contract_backend.dto.ContractGenerateDto;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractFlowRecord;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ContractService {

    ContractUploadResponse uploadContract(MultipartFile file, String title, String contractType);

    ContractGenerateDto.GenerateResp generateDraft(ContractGenerateDto.GenerateReq req);

    ContractGenerateDto.ConfirmResp confirmGeneratedContract(ContractGenerateDto.ConfirmReq req);

    List<ContractFieldResponse> getContractFields(Long contractId);

    Map<String, Object> getContracts(int page, int size, String keyword, String status);

    Contract getContractDetail(Long contractId);

    // ===== 审批流 =====
    void submitForLegalReview(Long contractId, Long operatorId, String comment);

    void approveByLegal(Long contractId, Long operatorId, String comment);

    void rejectByLegal(Long contractId, Long operatorId, String comment);

    void approveByFinance(Long contractId, Long operatorId, String comment);

    void rejectByFinance(Long contractId, Long operatorId, String comment);

    void approveByApprover(Long contractId, Long operatorId, String comment);

    void rejectByApprover(Long contractId, Long operatorId, String comment);

    List<ContractFlowRecord> getFlowRecords(Long contractId);
}