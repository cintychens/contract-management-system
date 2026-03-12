package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.AdminTemplateFieldBindDto;
import com.contract.contract_backend.service.TemplateFieldBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/template-field-bind")
@RequiredArgsConstructor
public class TemplateFieldBindController {

    private final TemplateFieldBindService templateFieldBindService;

    @GetMapping("/template/{templateId}")
    public Result<List<AdminTemplateFieldBindDto>> listByTemplateId(@PathVariable Long templateId) {
        return Result.success(templateFieldBindService.listByTemplateId(templateId));
    }
}