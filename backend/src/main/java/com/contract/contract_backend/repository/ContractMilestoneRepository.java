package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContractMilestoneRepository
        extends JpaRepository<ContractMilestone, Long> {

    List<ContractMilestone> findByContractIdOrderBySortOrder(Long contractId);
}