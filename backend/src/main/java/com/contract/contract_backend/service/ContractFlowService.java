package com.contract.contract_backend.service;

import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractFlowRecord;
import com.contract.contract_backend.entity.FlowActionType;
import com.contract.contract_backend.entity.RoleCode;
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

        // ⭐⭐⭐ 核心判断
        boolean isTransportC =
                "transport_c".equalsIgnoreCase(contract.getContractType());

        if (isTransportC) {

            // ✅ 只有C类走审批
            contract.setStatus(ContractStatus.PENDING_APPROVAL);
            contract.setCurrentHandlerRole(RoleCode.APPROVER);
            contract.setCurrentHandlerId(null);

        } else {

            // ✅ 其他直接生效
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setCurrentHandlerRole(RoleCode.BUSINESS);
            contract.setCurrentHandlerId(contract.getCreatedBy());
            contract.setApprovedAt(LocalDateTime.now());
        }

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                isTransportC ? ContractStatus.PENDING_APPROVAL : ContractStatus.ACTIVE,
                oldRole,
                isTransportC ? RoleCode.APPROVER : RoleCode.BUSINESS,
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

        List<ContractFlowRecord> records =
                flowRecordRepository.findByContractIdOrderByCreatedAtAsc(contractId);

        for (ContractFlowRecord record : records) {
            if (record.getOperatorId() != null) {
                User user = userRepository.findById(record.getOperatorId()).orElse(null);
                if (user != null) {
                    if (user.getFullName() != null && !user.getFullName().isBlank()) {
                        record.setOperatorName(user.getFullName());
                    } else {
                        record.setOperatorName(user.getUsername());
                    }
                } else {
                    record.setOperatorName("未知用户");
                }
            } else {
                record.setOperatorName("-");
            }
        }

        return records;
    }

    public void completeContract(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.BUSINESS, "只有业务角色可以完成合同");
        validateStatus(contract, ContractStatus.ACTIVE, "当前合同不是已生效状态，不能完成");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.COMPLETED);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());
        contract.setClosedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.COMPLETED,
                oldRole,
                RoleCode.BUSINESS,
                FlowActionType.COMPLETE,
                operatorId,
                comment
        );
    }

    public void requestTerminateContract(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.BUSINESS, "只有业务角色可以发起终止申请");
        validateStatus(contract, ContractStatus.ACTIVE, "当前合同不是已生效状态，不能申请终止");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.PENDING_TERMINATION);
        contract.setCurrentHandlerRole(RoleCode.APPROVER);
        contract.setCurrentHandlerId(null);

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.PENDING_TERMINATION,
                oldRole,
                RoleCode.APPROVER,
                FlowActionType.REQUEST_TERMINATION,
                operatorId,
                comment
        );
    }

    public void approverApproveTermination(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        validateRole(operator, RoleCode.APPROVER, "只有审批角色可以确认终止");
        validateStatus(contract, ContractStatus.PENDING_TERMINATION, "当前合同不是待终止审批状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        contract.setStatus(ContractStatus.TERMINATED);

        contract.setCurrentHandlerRole(null);
        contract.setCurrentHandlerId(null);

        contract.setClosedAt(LocalDateTime.now());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.TERMINATED,
                oldRole,
                RoleCode.BUSINESS,
                FlowActionType.TERMINATE,
                operatorId,
                comment
        );
    }

    public void approverRejectTermination(Long contractId, Long operatorId, String comment) {
        Contract contract = getContractOrThrow(contractId);
        User operator = getUserOrThrow(operatorId);

        // 只有审批人可以驳回
        validateRole(operator, RoleCode.APPROVER, "只有审批角色可以驳回终止申请");
        // 必须处于待终止审批
        validateStatus(contract, ContractStatus.PENDING_TERMINATION, "当前合同不在待终止审批状态");

        String oldStatus = contract.getStatus();
        String oldRole = contract.getCurrentHandlerRole();

        // 回到执行中（你当前用 ACTIVE 表示执行中）
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setCurrentHandlerRole(RoleCode.BUSINESS);
        contract.setCurrentHandlerId(contract.getCreatedBy());

        contractRepository.save(contract);

        saveFlowRecord(
                contractId,
                oldStatus,
                ContractStatus.ACTIVE,
                oldRole,
                RoleCode.BUSINESS,
                FlowActionType.REJECT_TERMINATION,
                operatorId,
                comment
        );
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
