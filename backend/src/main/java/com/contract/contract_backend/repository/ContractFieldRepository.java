package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractFieldRepository extends JpaRepository<ContractField, Long> {

    List<ContractField> findByContractIdOrderBySortOrderAsc(Long contractId);

    Optional<ContractField> findByContractIdAndFieldKey(Long contractId, String fieldKey);

    boolean existsByFieldKey(String fieldKey);

    void deleteByContractId(Long contractId);
}