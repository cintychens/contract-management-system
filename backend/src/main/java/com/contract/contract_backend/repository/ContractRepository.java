package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractNo(String contractNo);

    Page<Contract> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Contract> findByStatus(String status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, String status, Pageable pageable);
}