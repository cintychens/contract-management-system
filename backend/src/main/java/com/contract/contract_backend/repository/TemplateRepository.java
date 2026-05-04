package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.Template;
import com.contract.contract_backend.entity.TemplateField; // 添加缺失的导入
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndTemplateIdNot(String name, Long templateId);

    long countByStatus(String status);

    Page<Template> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // ❌ 原来的（保留）
    Page<Template> findByContractType(String contractType, Pageable pageable);

    // ✅ 新增（核心！！！）
    Page<Template> findByContractTypeStartingWith(String contractType, Pageable pageable);

    Page<Template> findByStatus(String status, Pageable pageable);

    Page<Template> findByNameContainingIgnoreCaseAndContractType(String name, String contractType, Pageable pageable);

    // ✅ 新增（核心！！！）
    Page<Template> findByNameContainingIgnoreCaseAndContractTypeStartingWith(String name, String contractType, Pageable pageable);

    Page<Template> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

    Page<Template> findByContractTypeAndStatus(String contractType, String status, Pageable pageable);

    // ✅ 新增（核心！！！）
    Page<Template> findByContractTypeStartingWithAndStatus(String contractType, String status, Pageable pageable);

    // ✅ 新增（核心！！！）
    Page<Template> findByNameContainingIgnoreCaseAndContractTypeStartingWithAndStatus(
            String name, String contractType, String status, Pageable pageable
    );

    List<Template> findByStatusIgnoreCase(String status);
}