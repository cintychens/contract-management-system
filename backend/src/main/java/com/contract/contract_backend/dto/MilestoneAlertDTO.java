package com.contract.contract_backend.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MilestoneAlertDTO {

    private Long milestoneId;
    private Long contractId;

    private String contractTitle;
    private String contractNo;

    private String milestoneName;
    private LocalDateTime expectedDate;

    private String responsibleRole;

    private String type;
    private Long diffDays;

    private String delayReason;
}