package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.ContractMilestoneLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractMilestoneLogRepository
        extends JpaRepository<ContractMilestoneLog, Long> {

    // ⭐ 根据多个里程碑ID查询日志（按时间倒序）
    List<ContractMilestoneLog> findByMilestoneIdInOrderByOperateTimeDesc(List<Long> milestoneIds);
}