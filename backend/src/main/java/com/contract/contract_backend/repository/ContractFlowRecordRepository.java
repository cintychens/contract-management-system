package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractFlowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractFlowRecordRepository extends JpaRepository<ContractFlowRecord, Long> {
    List<ContractFlowRecord> findByContractIdOrderByCreatedAtAsc(Long contractId);
}