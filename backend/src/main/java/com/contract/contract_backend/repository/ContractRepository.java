package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByContractNo(String contractNo);
}