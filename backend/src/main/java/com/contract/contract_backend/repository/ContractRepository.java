package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractNo(String contractNo);

    Optional<Contract> findById(Long contractId);

    Optional<Contract> findByContractNo(String contractNo);

    // ⭐⭐ 核心：统一搜索（支持 keyword + status）
    @Query("""
        SELECT c FROM Contract c
        WHERE
            (:keyword IS NULL OR
             LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
             LOWER(c.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND
            (:status IS NULL OR c.status = :status)
    """)
    Page<Contract> search(
            @Param("keyword") String keyword,
            @Param("status") String status,
            Pageable pageable
    );
}