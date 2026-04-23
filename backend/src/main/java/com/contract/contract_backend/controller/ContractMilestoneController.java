package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.ContractMilestone;
import com.contract.contract_backend.service.ContractMilestoneService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}