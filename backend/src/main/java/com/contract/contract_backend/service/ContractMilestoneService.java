package com.contract.contract_backend.service;

import com.contract.contract_backend.common.constant.ContractStatus;
import com.contract.contract_backend.entity.Contract;
import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.entity.ContractMilestoneLog;
import com.contract.contract_backend.repository.ContractMilestoneLogRepository;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.repository.ContractRepository;
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

    @Autowired
    private ContractRepository contractRepository;

    // 查询
    public List<ContractMilestone> list(Long contractId) {
        return repository.findByContractIdOrderBySortOrder(contractId);
    }

    // ⭐ 完成节点（最终版）
    public void complete(Long id) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        Contract contract = contractRepository.findById(m.getContractId())
                .orElseThrow(() -> new RuntimeException("合同不存在"));

        // ⭐ 必须生效
        if (!ContractStatus.ACTIVE.equals(contract.getStatus())) {
            throw new RuntimeException("合同未生效，不能执行履约");
        }

        // ⭐ 防重复
        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能重复操作");
        }

        // ⭐ 必须先填写预计时间（新加逻辑）
        if (m.getExpectedDate() == null) {
            throw new RuntimeException("请先填写预计完成时间");
        }

        // ⭐ 顺序校验
        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(m.getContractId());

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) {

                if (i > 0) {
                    ContractMilestone prev = list.get(i - 1);

                    if (!"COMPLETED".equals(prev.getStatus())) {
                        throw new RuntimeException("必须按顺序完成节点");
                    }
                }

                break;
            }
        }

        // ⭐ 写日志
        ContractMilestoneLog log = new ContractMilestoneLog();
        log.setMilestoneId(id);
        log.setOldStatus(m.getStatus());
        log.setNewStatus("COMPLETED");
        log.setOperateTime(LocalDateTime.now());
        logRepository.save(log);

        // ⭐ 更新
        m.setStatus("COMPLETED");
        m.setActualDate(LocalDateTime.now());
        repository.save(m);

        // ⭐ 全部完成 → 合同完成
        boolean allDone = repository
                .findByContractIdOrderBySortOrder(m.getContractId())
                .stream()
                .allMatch(x -> "COMPLETED".equals(x.getStatus()));

        if (allDone) {
            contract.setStatus(ContractStatus.COMPLETED);
            contractRepository.save(contract);
        }
    }

    // ⭐ 初始化履约节点（已删除 dueDate）
    public void initMilestones(Long contractId, LocalDate startDate) {

        ContractMilestone m1 = new ContractMilestone();
        m1.setContractId(contractId);
        m1.setName("发货");
        m1.setResponsibleRole("BUSINESS");
        m1.setSortOrder(1);
        m1.setStatus("PENDING");

        ContractMilestone m2 = new ContractMilestone();
        m2.setContractId(contractId);
        m2.setName("到货");
        m2.setResponsibleRole("BUSINESS");
        m2.setSortOrder(2);
        m2.setStatus("PENDING");

        ContractMilestone m3 = new ContractMilestone();
        m3.setContractId(contractId);
        m3.setName("验收");
        m3.setResponsibleRole("LEGAL");
        m3.setSortOrder(3);
        m3.setStatus("PENDING");

        ContractMilestone m4 = new ContractMilestone();
        m4.setContractId(contractId);
        m4.setName("付款");
        m4.setResponsibleRole("FINANCE");
        m4.setSortOrder(4);
        m4.setStatus("PENDING");

        repository.saveAll(List.of(m1, m2, m3, m4));
    }

    // ⭐ 设置预计时间
    public void setExpected(Long id, LocalDateTime expectedDate, String role) {

        ContractMilestone m = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("节点不存在"));

        // 权限控制
        if (!role.equals(m.getResponsibleRole()) && !"ADMIN".equals(role)) {
            throw new RuntimeException("无权限设置预计时间");
        }

        // 已完成不可修改
        if ("COMPLETED".equals(m.getStatus())) {
            throw new RuntimeException("节点已完成，不能修改预计时间");
        }

        m.setExpectedDate(expectedDate);
        repository.save(m);
    }
}