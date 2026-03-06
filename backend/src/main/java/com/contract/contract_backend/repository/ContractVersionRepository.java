package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractVersionRepository extends JpaRepository<ContractVersion, Long> {
    List<ContractVersion> findByContractIdOrderByVersionNoDesc(Long contractId);

    Optional<ContractVersion> findTopByContractIdOrderByVersionNoDesc(Long contractId);
}