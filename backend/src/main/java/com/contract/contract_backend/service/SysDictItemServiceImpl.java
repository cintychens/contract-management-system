package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.dto.AdminDictDto;
import com.contract.contract_backend.entity.SysDictItem;
import com.contract.contract_backend.repository.SysDictItemRepository;
import com.contract.contract_backend.service.SysDictItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SysDictItemServiceImpl implements SysDictItemService {

    private final SysDictItemRepository repository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Map<String, Object> pageDictItems(int page, int size, String dictType, String keyword, String status) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("sortOrder").ascending().and(Sort.by("id")));

        Page<SysDictItem> result;

        if (keyword != null && !keyword.isBlank() && status != null && !status.isBlank()) {
            result = repository.findByDictTypeIgnoreCaseAndItemNameContainingIgnoreCaseAndStatusIgnoreCase(
                    dictType, keyword, status, pageable
            );
        } else if (keyword != null && !keyword.isBlank()) {
            result = repository.findByDictTypeIgnoreCaseAndItemNameContainingIgnoreCase(
                    dictType, keyword, pageable
            );
        } else if (status != null && !status.isBlank()) {
            result = repository.findByDictTypeIgnoreCaseAndStatusIgnoreCase(
                    dictType, status, pageable
            );
        } else {
            result = repository.findByDictTypeIgnoreCase(dictType, pageable);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("records", result.getContent().stream().map(this::toRow).toList());
        map.put("total", result.getTotalElements());
        return map;
    }

    @Override
    public AdminDictDto.Row getDictItemDetail(Long id) {
        SysDictItem item = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("字典项不存在"));
        return toRow(item);
    }

    @Override
    public AdminDictDto.Row createDictItem(AdminDictDto.SaveReq req) {

        if (repository.existsByDictTypeAndItemKeyIgnoreCase(req.getDictType(), req.getItemKey())) {
            throw new RuntimeException("字段编码已存在");
        }

        SysDictItem item = SysDictItem.builder()
                .dictType(req.getDictType())
                .itemKey(req.getItemKey())
                .itemName(req.getItemName())
                .valueType(req.getValueType())
                .moduleName(req.getModuleName())
                .requiredFlag(req.getRequiredFlag())
                .itemValue(req.getItemValue())
                .status(req.getStatus())
                .sortOrder(req.getSortOrder())
                .remark(req.getRemark())
                .build();

        repository.save(item);
        return toRow(item);
    }

    @Override
    public AdminDictDto.Row updateDictItem(Long id, AdminDictDto.SaveReq req) {

        SysDictItem item = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("字典项不存在"));

        if (repository.existsByDictTypeAndItemKeyIgnoreCaseAndIdNot(req.getDictType(), req.getItemKey(), id)) {
            throw new RuntimeException("字段编码已存在");
        }

        item.setDictType(req.getDictType());
        item.setItemKey(req.getItemKey());
        item.setItemName(req.getItemName());
        item.setValueType(req.getValueType());
        item.setModuleName(req.getModuleName());
        item.setRequiredFlag(req.getRequiredFlag());
        item.setItemValue(req.getItemValue());
        item.setStatus(req.getStatus());
        item.setSortOrder(req.getSortOrder());
        item.setRemark(req.getRemark());

        repository.save(item);
        return toRow(item);
    }

    @Override
    public void changeStatus(Long id, String status) {

        SysDictItem item = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("字典项不存在"));

        item.setStatus(status);
        repository.save(item);
    }

    private AdminDictDto.Row toRow(SysDictItem item) {
        return AdminDictDto.Row.builder()
                .id(item.getId())
                .dictType(item.getDictType())
                .itemKey(item.getItemKey())
                .itemName(item.getItemName())
                .valueType(item.getValueType())
                .moduleName(item.getModuleName())
                .requiredFlag(item.getRequiredFlag())
                .itemValue(item.getItemValue())
                .status(item.getStatus())
                .sortOrder(item.getSortOrder())
                .remark(item.getRemark())
                .createdBy(item.getCreatedBy())
                .updatedBy(item.getUpdatedBy())
                .createdAt(item.getCreatedAt() == null ? null : item.getCreatedAt().format(FMT))
                .updatedAt(item.getUpdatedAt() == null ? null : item.getUpdatedAt().format(FMT))
                .build();
    }
}