package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractMilestoneLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractMilestoneLogRepository
        extends JpaRepository<ContractMilestoneLog, Long> {
}