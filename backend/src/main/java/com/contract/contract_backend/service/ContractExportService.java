package com.contract.contract_backend.service;

public interface ContractExportService {

    byte[] exportWord(Long contractId);

    byte[] exportPdf(Long contractId);
}