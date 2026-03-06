package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Template;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndTemplateIdNot(String name, Long templateId);

    long countByStatus(String status);

    Page<Template> findByNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Template> findByContractType(String contractType, Pageable pageable);

    Page<Template> findByStatus(String status, Pageable pageable);

    Page<Template> findByNameContainingIgnoreCaseAndContractType(String name, String contractType, Pageable pageable);

    Page<Template> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    Page<Template> findByContractTypeAndStatus(String contractType, String status, Pageable pageable);

    Page<Template> findByNameContainingIgnoreCaseAndContractTypeAndStatus(
            String name, String contractType, String status, Pageable pageable
    );
}