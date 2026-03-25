package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
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
    public Result<String> submitForLegalReview(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.submitForLegalReview(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("提交法务审核成功");
    }

    /**
     * 法务通过
     */
    @PostMapping("/legal/approve")
    public Result<String> legalApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.legalApprove(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("法务审批通过");
    }

    /**
     * 法务退回
     */
    @PostMapping("/legal/reject")
    public Result<String> legalReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.legalReject(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("法务已退回合同");
    }

    /**
     * 财务通过
     */
    @PostMapping("/finance/approve")
    public Result<String> financeApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.financeApprove(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("财务审批通过");
    }

    /**
     * 财务退回
     */
    @PostMapping("/finance/reject")
    public Result<String> financeReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.financeReject(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("财务已退回合同");
    }

    /**
     * 审批人通过
     */
    @PostMapping("/approver/approve")
    public Result<String> approverApprove(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverApprove(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("合同最终审批通过，合同已生效");
    }

    /**
     * 审批人驳回
     */
    @PostMapping("/approver/reject")
    public Result<String> approverReject(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverReject(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("审批人已驳回合同");
    }

    /**
     * 查看流转记录
     */
    @GetMapping("/records")
    public Result<List<ContractFlowRecord>> getFlowRecords(@PathVariable Long contractId) {
        return Result.success(contractFlowService.getFlowRecords(contractId));
    }

    /**
     * 完成合同
     */
    @PostMapping("/complete")
    public Result<String> completeContract(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.completeContract(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("合同已完成");
    }

    /**
     * 终止合同
     */
    @PostMapping("/request-termination")
    public Result<String> requestTermination(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.requestTerminateContract(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("已提交终止申请");
    }

    @PostMapping("/reject-termination")
    public Result<String> rejectTermination(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverRejectTermination(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("已驳回终止申请");
    }

    @PostMapping("/approve-termination")
    public Result<String> approveTermination(
            @PathVariable Long contractId,
            @RequestBody FlowActionRequest request
    ) {
        contractFlowService.approverApproveTermination(
                contractId,
                request.getOperatorId(),
                request.getComment()
        );
        return Result.success("合同已终止");
    }
}