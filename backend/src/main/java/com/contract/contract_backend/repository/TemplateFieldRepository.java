package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.TemplateField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateFieldRepository extends JpaRepository<TemplateField, Long> {

    Optional<TemplateField> findByFieldKey(String fieldKey);

    boolean existsByFieldKey(String fieldKey);

    boolean existsByFieldKeyAndFieldIdNot(String fieldKey, Long fieldId);

    List<TemplateField> findByStatusOrderBySortOrderAsc(String status);

    List<TemplateField> findByBusinessTypeAndStatusOrderBySortOrderAsc(String businessType, String status);

    List<TemplateField> findAllByOrderBySortOrderAsc();
}