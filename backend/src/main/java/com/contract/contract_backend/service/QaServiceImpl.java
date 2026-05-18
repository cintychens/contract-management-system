package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.QaRequest;
import com.contract.contract_backend.dto.QaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QaServiceImpl implements QaService {

    private final ContractAiContextService contractAiContextService;
    private final DeepSeekService deepSeekService;

    @Override
    public QaResponse ask(QaRequest request) {
        String question = request.getQuestion().trim();
        String context = contractAiContextService.buildAllContractsContext();
        String answer = deepSeekService.answerQuestion(question, context);

        return QaResponse.builder()
                .contractId(request.getContractId())
                .question(question)
                .answer(answer)
                .source("api-ai-qa")
                .references(List.of("all contracts"))
                .build();
    }
}
