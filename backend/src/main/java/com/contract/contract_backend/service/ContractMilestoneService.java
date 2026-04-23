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

    // 查询
    public List<ContractMilestone> list(Long contractId) {
        return repository.findByContractIdOrderBySortOrder(contractId);
    }

    // 完成节点（带顺序控制）
    public void complete(Long id) {

        // 1. 查当前节点
        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // 2. 获取同合同所有节点（按顺序）
        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(m.getContractId());

        // 3. ⭐ 顺序校验（核心）
        for (int i = 0; i < list.size(); i++) {

            if (list.get(i).getId().equals(id)) {

                // 不是第一个节点
                if (i > 0) {
                    ContractMilestone prev = list.get(i - 1);

                    if (!"COMPLETED".equals(prev.getStatus())) {
                        throw new RuntimeException("必须按顺序完成节点");
                    }
                }
            }
        }

        // 4. 防止重复点击
        if ("COMPLETED".equals(m.getStatus())) {
            return;
        }

        // 5. 写日志
        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus(m.getStatus());
        log.setNewStatus("COMPLETED");
        log.setOperateTime(LocalDateTime.now());
        logRepository.save(log);

        // 6. 更新状态
        m.setStatus("COMPLETED");
        m.setActualDate(LocalDateTime.now());

        // 7. 保存
        repository.save(m);
    }
}