package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.UserRepository;
import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.entity.ContractMilestoneLog;
import com.contract.contract_backend.repository.ContractMilestoneLogRepository;
import com.contract.contract_backend.repository.ContractMilestoneRepository;
import com.contract.contract_backend.service.ContractMilestoneService;
import com.contract.contract_backend.common.Result;

import com.contract.contract_backend.dto.MilestoneAlertDTO;
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

    @Autowired
    private UserRepository userRepository;

    // =========================
    // 查询履约节点
    // =========================
    @GetMapping("/{contractId}")
    public List<ContractMilestone> list(@PathVariable Long contractId) {
        return service.list(contractId);
    }

    @PostMapping("/{id}/accept")
    public Result accept(@PathVariable Long id, @RequestBody Map<String, String> body) {

        String status = body.get("status");   // PASSED / FAILED
        String reason = body.get("reason");

        String role = getCurrentUserRole();
        String username = getCurrentUsername();

        service.accept(id, status, reason, role, username);

        return Result.success("验收处理成功");
    }

    @PostMapping("/{id}/legal")
    public Result legal(@PathVariable Long id, @RequestBody Map<String, String> body) {

        String result = body.get("result");
        String reason = body.get("reason");             // ⭐新增
        String responsibility = body.get("responsibility"); // ⭐新增
        String action = body.get("action");             // ⭐新增

        String role = getCurrentUserRole();

        String username = getCurrentUsername();  // ⭐你已有的话

        service.legalProcess(id, result, role, reason, responsibility, action, username);

        return Result.success("法务处理完成");
    }

    @PostMapping("/contracts/{id}/terminate")
    public Result terminate(@PathVariable Long id) {

        service.terminateContract(id);

        return Result.success("合同已终止");
    }

    // =========================
    // 完成节点
    // =========================
    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id) {

        String role = getCurrentUserRole();
        String username = getCurrentUsername();

        service.complete(id, role, username);
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
        String username = getCurrentUsername();

        service.reportDelay(id, reason, role, username);
    }

    @GetMapping("/alerts")
    public List<MilestoneAlertDTO> getAlerts() {
        return service.getMilestoneAlerts(); // ⭐ 用你已有的 service
    }

    @PostMapping("/{id}/reject")
    public Result reject(@PathVariable Long id) {

        String role = getCurrentUserRole();

        service.reject(id, role);

        return Result.success("验收失败");
    }

    @PostMapping("/restart/{contractId}")
    public Result restart(@PathVariable Long contractId) {

        service.restartTransport(contractId);

        return Result.success("重新发货成功");
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

    @GetMapping("/milestones/summary")
    public Result getSummary() {
        return Result.success(service.getSummary());
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

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return "未知用户";
        }

        String username = auth.getName();

        return userRepository.findByUsername(username)
                .map(user -> user.getFullName() != null && !user.getFullName().isBlank()
                        ? user.getFullName()
                        : user.getUsername())
                .orElse(username);
    }
}