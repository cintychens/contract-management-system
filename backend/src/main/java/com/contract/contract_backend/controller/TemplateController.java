package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.AdminTemplateDto;
import com.contract.contract_backend.dto.PageResult;
import com.contract.contract_backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/templates")
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public PageResult<AdminTemplateDto.TemplateRow> pageTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String contractType,
            @RequestParam(required = false) String status
    ) {
        return templateService.pageTemplates(page, size, keyword, contractType, status);
    }

    @GetMapping("/stats")
    public AdminTemplateDto.Stats stats() {
        return templateService.stats();
    }

    @GetMapping("/{id}")
    public AdminTemplateDto.TemplateDetail getTemplate(@PathVariable("id") Long id) {
        return templateService.getTemplate(id);
    }

    @PostMapping
    public AdminTemplateDto.TemplateRow createTemplate(@RequestBody AdminTemplateDto.SaveReq req) {
        return templateService.createTemplate(req);
    }

    @PutMapping("/{id}")
    public AdminTemplateDto.TemplateRow updateTemplate(
            @PathVariable("id") Long id,
            @RequestBody AdminTemplateDto.SaveReq req
    ) {
        return templateService.updateTemplate(id, req);
    }

    @PutMapping("/{id}/status")
    public AdminTemplateDto.TemplateRow updateStatus(
            @PathVariable("id") Long id,
            @RequestBody AdminTemplateDto.StatusReq req
    ) {
        return templateService.updateStatus(id, req);
    }

    @DeleteMapping("/{id}")
    public String deleteTemplate(@PathVariable("id") Long id) {
        templateService.deleteTemplate(id);
        return "OK";
    }
}