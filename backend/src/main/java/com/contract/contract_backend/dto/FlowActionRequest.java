package com.contract.contract_backend.dto.contractflow;

import lombok.Data;

@Data
public class FlowActionRequest {

    /**
     * 当前操作人ID
     */
    private Long operatorId;

    /**
     * 审批意见 / 退回说明
     */
    private String comment;
}