package com.contract.contract_backend.service;

import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.entity.ContractMilestoneLog;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.repository.ContractMilestoneLogRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ContractMilestoneService {

    @Autowired
    private ContractMilestoneRepository repository;

    @Autowired
    private ContractMilestoneLogRepository logRepository;

    public List<ContractMilestone> list(Long contractId) {
        return repository.findByContractIdOrderBySortOrder(contractId);
    }

    public void complete(Long id) {
        ContractMilestone m = repository.findById(id).get();

        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus(m.getStatus());
        log.setNewStatus("COMPLETED");
        log.setOperateTime(LocalDateTime.now());
        logRepository.save(log);

        m.setStatus("COMPLETED");
        m.setActualDate(LocalDate.now());
    }
}