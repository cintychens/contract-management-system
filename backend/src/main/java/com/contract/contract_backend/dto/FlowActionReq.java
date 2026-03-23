package com.contract.contract_backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlowActionReq {
    private Long operatorId;
    private String comment;
}