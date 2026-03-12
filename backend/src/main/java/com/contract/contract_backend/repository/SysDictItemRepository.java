package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.SysDictItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SysDictItemRepository extends JpaRepository<SysDictItem, Long> {

    boolean existsByDictTypeAndItemKeyIgnoreCase(String dictType, String itemKey);

    boolean existsByDictTypeAndItemKeyIgnoreCaseAndIdNot(String dictType, String itemKey, Long id);

    Page<SysDictItem> findByDictTypeIgnoreCase(String dictType, Pageable pageable);

    List<SysDictItem> findByDictTypeIgnoreCase(String dictType);

    Page<SysDictItem> findByDictTypeIgnoreCaseAndItemNameContainingIgnoreCase(
            String dictType,
            String itemName,
            Pageable pageable
    );

    Page<SysDictItem> findByDictTypeIgnoreCaseAndItemKeyContainingIgnoreCase(
            String dictType,
            String itemKey,
            Pageable pageable
    );

    Page<SysDictItem> findByDictTypeIgnoreCaseAndStatusIgnoreCase(
            String dictType,
            String status,
            Pageable pageable
    );

    Page<SysDictItem> findByDictTypeIgnoreCaseAndItemNameContainingIgnoreCaseAndStatusIgnoreCase(
            String dictType,
            String itemName,
            String status,
            Pageable pageable
    );

    Page<SysDictItem> findByDictTypeIgnoreCaseAndItemKeyContainingIgnoreCaseAndStatusIgnoreCase(
            String dictType,
            String itemKey,
            String status,
            Pageable pageable
    );
}