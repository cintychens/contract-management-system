package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.GenerateContractRequest;
import com.contract.contract_backend.service.TemplateGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateGenerateController {

    private final TemplateGenerateService templateGenerateService;

    /**
     * 获取模板变量
     * GET /api/templates/{id}/variables
     */
    @GetMapping("/{id}/variables")
    public Map<String, Object> getTemplateVariables(@PathVariable Long id) {
        return templateGenerateService.getTemplateVariables(id);
    }

    /**
     * 生成合同
     * POST /api/templates/generate
     */
    @PostMapping("/generate")
    public Map<String, Object> generateContract(@RequestBody GenerateContractRequest request) {
        return templateGenerateService.generateContract(request);
    }
}