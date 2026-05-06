package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.TransportOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long> {

    List<TransportOrder> findByContractId(Long contractId);
}