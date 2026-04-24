package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.entity.ContractMilestoneLog;
import com.contract.contract_backend.repository.ContractMilestoneLogRepository;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.service.ContractMilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/milestones")
public class ContractMilestoneController {

    @Autowired
    private ContractMilestoneService service;

    @Autowired
    private ContractMilestoneRepository repository;

    @Autowired
    private ContractMilestoneLogRepository logRepository;

    // =========================
    // 查询履约节点
    // =========================
    @GetMapping("/{contractId}")
    public List<ContractMilestone> list(@PathVariable Long contractId) {
        return service.list(contractId);
    }

    // =========================
    // 完成节点
    // =========================
    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id) {

        String role = getCurrentUserRole();
        service.complete(id, role);
    }

    // =========================
    // 设置预计时间
    // =========================
    @PostMapping("/{id}/expected")
    public void setExpected(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String expected = body.get("expectedDate");

        if (expected == null || expected.isBlank()) {
            throw new RuntimeException("预计时间不能为空");
        }

        String role = getCurrentUserRole();

        service.setExpected(
                id,
                LocalDateTime.parse(expected),
                role
        );
    }

    // =========================
    // 上报延期
    // =========================
    @PostMapping("/{id}/delay")
    public void reportDelay(@PathVariable Long id,
                            @RequestBody Map<String,String> body) {

        String reason = body.get("delayReason");

        String role = getCurrentUserRole();

        service.reportDelay(id, reason, role);
    }

    // =========================
    // ⭐ 新增：履约日志接口（前端用）
    // =========================
    @GetMapping("/{contractId}/logs")
    public List<ContractMilestoneLog> logs(@PathVariable Long contractId) {

        List<ContractMilestone> list =
                repository.findByContractIdOrderBySortOrder(contractId);

        List<Long> ids = list.stream()
                .map(ContractMilestone::getId)
                .collect(Collectors.toList());

        if (ids.isEmpty()) {
            return List.of();
        }

        return logRepository.findByMilestoneIdInOrderByOperateTimeDesc(ids);
    }

    // =========================
    // 获取当前用户角色
    // =========================
    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities().isEmpty()) {
            throw new RuntimeException("未获取到用户角色");
        }

        return auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");
    }
}