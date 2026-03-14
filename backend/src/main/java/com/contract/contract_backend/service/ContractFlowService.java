package com.contract.contract_backend.service;

import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.common.constant.FlowActionType;
import com.contract.contract_backend.common.constant.RoleCode;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractFlowRecord;
import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.ContractFlowRecordRepository;
import com.contract.contract_backend.repository.ContractRepository;
import com.contract.contract_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractFlowService {

    private final ContractRepository contractRepository;
    private final ContractFlowRecordRepository flowRecordRepository;
    private final UserRepository userRepository;

    public void submitForLegalReview(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.BUSINESS, "只有业务角色可以提交法务审核");
        validateStatus(contract, ContractStatus.DRAFT, "当前合同不是草稿状态，不能提交审核");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_LEGAL);
        contract.setCurrentHandlerRole(RoleCode.LEGAL);
        contract.setCurrentHandlerId(null);
        contract.setSubmittedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_LEGAL,
                oldRole,
                RoleCode.LEGAL,
                FlowActionType.SUBMIT,
                operatorId,
                comment
        );
    }

    public void legalApprove(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.LEGAL, "只有法务角色可以执行法务审批");
        validateStatus(contract, ContractStatus.PENDING_LEGAL, "当前合同不在待法务审核状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_FINANCE);
        contract.setCurrentHandlerRole(RoleCode.FINANCE);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_FINANCE,
                oldRole,
                RoleCode.FINANCE,
                FlowActionType.APPROVE,
                operatorId,
                comment
        );
    }

    public void legalReject(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.LEGAL, "只有法务角色可以退回合同");
        validateStatus(contract, ContractStatus.PENDING_LEGAL, "当前合同不在待法务审核状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.DRAFT);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.DRAFT,
                oldRole,
                RoleCode.BUSINESS,
                FlowActionType.REJECT,
                operatorId,
                comment
        );
    }

    public void financeApprove(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.FINANCE, "只有财务角色可以执行财务审批");
        validateStatus(contract, ContractStatus.PENDING_FINANCE, "当前合同不在待财务审核状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_APPROVAL);
        contract.setCurrentHandlerRole(RoleCode.APPROVER);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_APPROVAL,
                oldRole,
                RoleCode.APPROVER,
                FlowActionType.APPROVE,
                operatorId,
                comment
        );
    }

    public void financeReject(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.FINANCE, "只有财务角色可以退回合同");
        validateStatus(contract, ContractStatus.PENDING_FINANCE, "当前合同不在待财务审核状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_LEGAL);
        contract.setCurrentHandlerRole(RoleCode.LEGAL);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_LEGAL,
                oldRole,
                RoleCode.LEGAL,
                FlowActionType.REJECT,
                operatorId,
                comment
        );
    }

    public void approverApprove(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.APPROVER, "只有审批角色可以执行最终审批");
        validateStatus(contract, ContractStatus.PENDING_APPROVAL, "当前合同不在待审批状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());
        contract.setApprovedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.ACTIVE,
                oldRole,
                RoleCode.BUSINESS,
                FlowActionType.APPROVE,
                operatorId,
                comment
        );
    }

    public void approverReject(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.APPROVER, "只有审批角色可以驳回合同");
        validateStatus(contract, ContractStatus.PENDING_APPROVAL, "当前合同不在待审批状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_FINANCE);
        contract.setCurrentHandlerRole(RoleCode.FINANCE);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_FINANCE,
                oldRole,
                RoleCode.FINANCE,
                FlowActionType.REJECT,
                operatorId,
                comment
        );
    }

    public List<ContractFlowRecord> getFlowRecords(Long contractId) {
        getContractOrThrow(contractId);
        return flowRecordRepository.findByContractIdOrderByCreatedAtAsc(contractId);
    }

    private Contract getContractOrThrow(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("合同不存在，contractId=" + contractId));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在，userId=" + userId));
    }

    private void validateStatus(Contract contract, String expectedStatus, String message) {
        if (!expectedStatus.equals(contract.getStatus())) {
            throw new RuntimeException(message);
        }
    }

    private void validateRole(User user, String expectedRole, String message) {
        if (!expectedRole.equals(user.getRoleCode())) {
            throw new RuntimeException(message);
        }
    }

    private void saveFlowRecord(
            Long contractId,
            String fromStatus,
            String toStatus,
            String fromRole,
            String toRole,
            String actionType,
            Long operatorId,
            String comment
    ) {
        ContractFlowRecord record = ContractFlowRecord.builder()
                .contractId(contractId)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .fromRole(fromRole)
                .toRole(toRole)
                .actionType(actionType)
                .operatorId(operatorId)
                .comment(comment)
                .build();

        flowRecordRepository.save(record);
    }
}