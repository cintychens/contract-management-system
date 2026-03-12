package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.TemplateFieldBind;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateFieldBindRepository extends JpaRepository<TemplateFieldBind, Long> {

    List<TemplateFieldBind> findByTemplateIdAndStatusOrderBySortOrderAscIdAsc(Long templateId, String status);
}