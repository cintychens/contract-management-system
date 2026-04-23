package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
public class ContractMilestoneLog {

    @Id
    @GeneratedValue
    private Long id;

    private Long milestoneId;

    private String oldStatus;
    private String newStatus;

    private LocalDate oldDate;
    private LocalDate newDate;

    private String operator;

    private LocalDateTime operateTime;
}