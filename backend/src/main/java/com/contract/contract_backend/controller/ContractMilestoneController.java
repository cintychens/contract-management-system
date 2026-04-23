package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.service.ContractMilestoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/milestones")
public class ContractMilestoneController {

    @Autowired
    private ContractMilestoneService service;

    @GetMapping("/{contractId}")
    public List<ContractMilestone> list(@PathVariable Long contractId) {
        return service.list(contractId);
    }

    @PostMapping("/{id}/complete")
    public void complete(@PathVariable Long id) {
        service.complete(id);
    }

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

    private String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getAuthorities().isEmpty()) {
            throw new RuntimeException("未获取到用户角色");
        }

        // 如果返回是 ROLE_BUSINESS，可去掉前缀
        return auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");
    }
}