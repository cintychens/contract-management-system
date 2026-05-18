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
    public Map<String, Object> generate(@RequestBody Map<String, Object> data) {

        try {
            String templateType = (String) data.get("templateType");

            if (templateType == null || templateType.isBlank()) {
                return Map.of(
                        "success", false,
                        "message", "templateType不能为空"
                );
            }

            Map<String, Object> result =
                    deepSeekService.generateWithTemplate(data, templateType);

            if (result.containsKey("error")) {
                return Map.of(
                        "success", false,
                        "message", result.get("error")
                );
            }

            return Map.of(
                    "success", true,
                    "data", result
            );

        } catch (Exception e) {
            e.printStackTrace();
            return Map.of(
                    "success", false,
                    "message", "AI处理失败：" + e.getMessage()
            );
        }
    }

}