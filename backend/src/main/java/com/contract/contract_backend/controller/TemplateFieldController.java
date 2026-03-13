package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.TemplateField;
import com.contract.contract_backend.repository.TemplateFieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/template-fields")
@RequiredArgsConstructor
public class TemplateFieldController {

    private final TemplateFieldRepository templateFieldRepository;

    @GetMapping
    public List<TemplateField> listAll() {
        return templateFieldRepository.findAllByOrderBySortOrderAsc();
    }
}