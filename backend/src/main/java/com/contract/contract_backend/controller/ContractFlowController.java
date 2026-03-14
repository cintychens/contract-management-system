package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.contractflow.FlowActionRequest;
import com.contract.contract_backend.entity.ContractFlowRecord;
import com.contract.contract_backend.service.ContractFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contracts/{contractId}/flow")
@RequiredArgsConstructor
public class ContractFlowController {

    private final ContractFlowService contractFlowService;

    /**
     * 业务提交法务审核
     */
    @PostMapping("/submit")
    public String submitForLegalReview(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.submitForLegalReview(contractId, request.getOperatorId(), request.getComment());
        return "提交法务审核成功";
    }

    /**
     * 法务通过
     */
    @PostMapping("/legal/approve")
    public String legalApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.legalApprove(contractId, request.getOperatorId(), request.getComment());
        return "法务审批通过";
    }

    /**
     * 法务退回
     */
    @PostMapping("/legal/reject")
    public String legalReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.legalReject(contractId, request.getOperatorId(), request.getComment());
        return "法务已退回合同";
    }

    /**
     * 财务通过
     */
    @PostMapping("/finance/approve")
    public String financeApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.financeApprove(contractId, request.getOperatorId(), request.getComment());
        return "财务审批通过";
    }

    /**
     * 财务退回
     */
    @PostMapping("/finance/reject")
    public String financeReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.financeReject(contractId, request.getOperatorId(), request.getComment());
        return "财务已退回合同";
    }

    /**
     * 审批人通过
     */
    @PostMapping("/approver/approve")
    public String approverApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverApprove(contractId, request.getOperatorId(), request.getComment());
        return "合同最终审批通过，合同已生效";
    }

    /**
     * 审批人驳回
     */
    @PostMapping("/approver/reject")
    public String approverReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverReject(contractId, request.getOperatorId(), request.getComment());
        return "审批人已驳回合同";
    }

    /**
     * 查看流转记录
     */
    @GetMapping("/records")
    public List<ContractFlowRecord> getFlowRecords(@PathVariable Long contractId) {
        return contractFlowService.getFlowRecords(contractId);
    }
}