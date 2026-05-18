package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.QaRequest;
import com.contract.contract_backend.dto.QaResponse;

public interface QaService {

    QaResponse ask(QaRequest request);
}
