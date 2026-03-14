package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractNo(String contractNo);

    // 添加根据ID查找合同的方法（Optional返回类型）
    Optional<Contract> findById(Long contractId);

    // 添加根据合同编号查找合同的方法（Optional返回类型）
    Optional<Contract> findByContractNo(String contractNo);

    Page<Contract> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Contract> findByStatus(String status, Pageable pageable);

    Page<Contract> findByTitleContainingIgnoreCaseAndStatus(String title, String status, Pageable pageable);
}