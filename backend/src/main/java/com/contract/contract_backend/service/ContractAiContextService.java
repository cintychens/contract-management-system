package com.contract.contract_backend.service;

import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractField;
import com.contract.contract_backend.entity.ContractFlowRecord;
import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.repository.ContractFieldRepository;
import com.contract.contract_backend.repository.ContractFlowRecordRepository;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContractAiContextService {

    private static final int MAX_CONTEXT_CHARS = 24000;
    private static final int MAX_CONTRACT_CONTENT_CHARS = 2500;

    private final ContractRepository contractRepository;
    private final ContractFieldRepository contractFieldRepository;
    private final ContractFlowRecordRepository contractFlowRecordRepository;
    private final ContractMilestoneRepository contractMilestoneRepository;

    public String buildAllContractsContext() {
        List<Contract> contracts = contractRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        if (contracts.isEmpty()) {
            return "There are no contracts in the system.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("You are answering questions for a logistics contract management system. ");
        sb.append("Use only the following contract records from the database. ");
        sb.append("Answer in Chinese. If information is missing, say it is not found in the contracts.\n\n");
        sb.append("Total contracts: ").append(contracts.size()).append("\n\n");

        for (Contract contract : contracts) {
            appendContract(sb, contract);
            if (sb.length() >= MAX_CONTEXT_CHARS) {
                sb.append("\nMore contracts exist, but the context was truncated because it is too long.");
                break;
            }
        }

        return limit(sb.toString(), MAX_CONTEXT_CHARS);
    }

    private void appendContract(StringBuilder sb, Contract contract) {
        Long contractId = contract.getContractId();
        sb.append("Contract ID: ").append(contractId).append("\n");
        sb.append("Title: ").append(value(contract.getTitle())).append("\n");
        sb.append("Contract No: ").append(value(contract.getContractNo())).append("\n");
        sb.append("Type: ").append(value(contract.getContractType())).append("\n");
        sb.append("Status: ").append(value(contract.getStatus())).append("\n");
        sb.append("Current handler role: ").append(value(contract.getCurrentHandlerRole())).append("\n");
        sb.append("Created at: ").append(contract.getCreatedAt() == null ? "-" : contract.getCreatedAt()).append("\n");
        sb.append("Submitted at: ").append(contract.getSubmittedAt() == null ? "-" : contract.getSubmittedAt()).append("\n");
        sb.append("Approved at: ").append(contract.getApprovedAt() == null ? "-" : contract.getApprovedAt()).append("\n");

        appendFields(sb, contractId);
        appendFlowRecords(sb, contractId);
        appendMilestones(sb, contractId);

        if (contract.getContent() != null && !contract.getContent().isBlank()) {
            sb.append("Content:\n")
                    .append(limit(contract.getContent(), MAX_CONTRACT_CONTENT_CHARS))
                    .append("\n");
        }
        sb.append("\n");
    }

    private void appendFields(StringBuilder sb, Long contractId) {
        List<ContractField> fields = contractFieldRepository.findByContractIdOrderBySortOrderAsc(contractId);
        if (fields.isEmpty()) {
            return;
        }

        sb.append("Fields:\n");
        fields.stream()
                .filter(field -> field.getFieldValue() != null && !field.getFieldValue().isBlank())
                .limit(12)
                .forEach(field -> sb.append("- ")
                        .append(value(field.getFieldName()))
                        .append(": ")
                        .append(field.getFieldValue().trim())
                        .append("\n"));
    }

    private void appendFlowRecords(StringBuilder sb, Long contractId) {
        List<ContractFlowRecord> records =
                contractFlowRecordRepository.findByContractIdOrderByCreatedAtAsc(contractId);
        if (records.isEmpty()) {
            return;
        }

        sb.append("Flow records:\n");
        records.stream().limit(8).forEach(record -> sb.append("- ")
                .append(value(record.getActionType()))
                .append(": ")
                .append(value(record.getFromStatus()))
                .append(" -> ")
                .append(value(record.getToStatus()))
                .append(", comment: ")
                .append(value(record.getComment()))
                .append(", time: ")
                .append(record.getCreatedAt() == null ? "-" : record.getCreatedAt())
                .append("\n"));
    }

    private void appendMilestones(StringBuilder sb, Long contractId) {
        List<ContractMilestone> milestones =
                contractMilestoneRepository.findByContractIdOrderBySortOrder(contractId);
        if (milestones.isEmpty()) {
            return;
        }

        sb.append("Milestones:\n");
        milestones.stream().limit(10).forEach(milestone -> sb.append("- ")
                .append(value(milestone.getName()))
                .append(": ")
                .append(value(milestone.getStatus()))
                .append(", expected: ")
                .append(milestone.getExpectedDate() == null ? "-" : milestone.getExpectedDate())
                .append(", actual: ")
                .append(milestone.getActualDate() == null ? "-" : milestone.getActualDate())
                .append("\n"));
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String limit(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[truncated]";
    }
}
