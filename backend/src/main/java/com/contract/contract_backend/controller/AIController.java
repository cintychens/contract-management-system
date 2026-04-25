package com.contract.contract_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.contract.contract_backend.service.DeepSeekService;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final DeepSeekService deepSeekService;

    @PostMapping("/generate")
    public String generate(@RequestBody Map<String, Object> data) {

        String templateType = (String) data.get("templateType");

        return deepSeekService.generateWithTemplate(data, templateType);
    }
}