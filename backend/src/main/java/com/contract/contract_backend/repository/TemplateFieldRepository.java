package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.TemplateField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateFieldRepository extends JpaRepository<TemplateField, Long> {

    List<TemplateField> findByTemplateIdOrderBySortOrderAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}