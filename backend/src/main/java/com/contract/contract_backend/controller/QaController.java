package com.contract.contract_backend.controller;

import com.contract.contract_backend.common.Result;
import com.contract.contract_backend.dto.QaRequest;
import com.contract.contract_backend.dto.QaResponse;
import com.contract.contract_backend.service.QaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final QaService qaService;

    @PostMapping("/ask")
    public Result<QaResponse> ask(@Valid @RequestBody QaRequest request) {
        return Result.success(qaService.ask(request));
    }
}
