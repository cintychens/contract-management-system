package com.contract.contract_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "contract_version",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_contract_version", columnNames = {"contract_id", "version_no"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Lob
    @Column(name = "CONTENT_TEXT")
    private String contentText;

    @Column(name = "contract_id", nullable = false)
    private Long contractId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 64)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "file_object_key", nullable = false, length = 500)
    private String fileObjectKey;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @Column(name = "change_note", length = 255)
    private String changeNote;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}