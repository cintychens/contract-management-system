package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.service.TransportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport")
public class TransportController {

    @Autowired
    private TransportService service;

    /**
     * 查询某合同的运单
     */
    @GetMapping("/{contractId}")
    public Result get(@PathVariable Long contractId) {
        return Result.success(service.getByContract(contractId));
    }

    @PostMapping("/reship/{contractId}")
    public Result reship(@PathVariable Long contractId) {
        service.createOnShip(contractId);
        return Result.success("重新发货成功");
    }
}