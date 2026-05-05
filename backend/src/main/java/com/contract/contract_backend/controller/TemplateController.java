package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.AdminTemplateDto;
import com.contract.contract_backend.dto.PageResult;
import com.contract.contract_backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * 新增：查询模板字段
     */
    @GetMapping("/{id}/fields")
    public List<Map<String, Object>> listTemplateFields(@PathVariable("id") Long id) {

        AdminTemplateDto.TemplateDetail template = templateService.getTemplate(id);

        if (template == null || template.getContent() == null) {
            throw new IllegalArgumentException("模板不存在或内容为空");
        }

        String content = template.getContent();

        Map<String, String> fieldMap = new LinkedHashMap<>();

        // ✅ 第一步：提取所有 ${xxx}
        Pattern keyPattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");
        Matcher keyMatcher = keyPattern.matcher(content);

        while (keyMatcher.find()) {
            String key = keyMatcher.group(1);
            fieldMap.putIfAbsent(key, key);
        }

        // ✅ 第二步：提取中文标签（覆盖）
        Pattern labelPattern = Pattern.compile("([^\\n：:]{2,})[：:]\\s*\\$\\{([a-zA-Z0-9_]+)}");
        Matcher labelMatcher = labelPattern.matcher(content);

        while (labelMatcher.find()) {
            String label = labelMatcher.group(1).trim();
            String key = labelMatcher.group(2).trim();

            fieldMap.put(key, label);
        }

        // ✅ 返回
        return fieldMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("fieldKey", e.getKey());
                    m.put("fieldName", e.getValue());
                    return m;
                })
                .toList();
    }

    @PostMapping
    public AdminTemplateDto.TemplateRow createTemplate(@RequestBody AdminTemplateDto.SaveReq req) {
        return templateService.createTemplate(req);
    }

    @PostMapping("/upload")
    public Map<String, Object> uploadTemplateFile(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase()
                : "";

        if (!List.of("pdf", "doc", "docx").contains(ext)) {
            throw new IllegalArgumentException("仅支持 pdf/doc/docx 文件");
        }

        String uploadDir = System.getProperty("user.dir") + "/uploads/templates/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String newFileName = System.currentTimeMillis() + "_" + originalFilename;
        File dest = new File(uploadDir + newFileName);
        file.transferTo(dest);

        Map<String, Object> result = new HashMap<>();
        result.put("fileName", originalFilename);
        result.put("fileObjectKey", "uploads/templates/" + newFileName);
        return result;
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