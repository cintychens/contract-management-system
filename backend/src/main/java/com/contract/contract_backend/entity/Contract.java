package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    private Long contractId;

    @Column(name = "contract_no", nullable = false, unique = true, length = 64)
    private String contractNo;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "contract_type", nullable = false, length = 64)
    private String contractType;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "current_version_id")
    private Long currentVersionId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}