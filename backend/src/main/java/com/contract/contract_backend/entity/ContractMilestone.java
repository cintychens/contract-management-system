package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
public class ContractMilestone {

    @Id
    @GeneratedValue
    private Long id;

    private Long contractId;

    private String name;

    private Integer sortOrder;

    private LocalDateTime dueDate;
    private LocalDateTime actualDate;

    private String status;

    private String responsibleRole;

    private Boolean isLocked;
}