package com.contract.contract_backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "contract.upload")
public class ContractUploadProperties {
    private List<String> allowedExtensions;
    private Long maxSize;
}