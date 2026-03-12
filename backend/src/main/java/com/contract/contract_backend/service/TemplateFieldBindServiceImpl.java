package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.dto.AdminTemplateFieldBindDto;
import com.contract.contract_backend.entity.SysDictItem;
import com.contract.contract_backend.entity.TemplateFieldBind;
import com.contract.contract_backend.repository.SysDictItemRepository;
import com.contract.contract_backend.repository.TemplateFieldBindRepository;
import com.contract.contract_backend.service.TemplateFieldBindService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateFieldBindServiceImpl implements TemplateFieldBindService {

    private final TemplateFieldBindRepository templateFieldBindRepository;
    private final SysDictItemRepository sysDictItemRepository;

    @Override
    public List<AdminTemplateFieldBindDto> listByTemplateId(Long templateId) {
        List<TemplateFieldBind> binds =
                templateFieldBindRepository.findByTemplateIdAndStatusOrderBySortOrderAscIdAsc(templateId, "ENABLED");

        if (binds.isEmpty()) {
            return List.of();
        }

        List<String> fieldKeys = binds.stream()
                .map(TemplateFieldBind::getFieldKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<SysDictItem> dictItems = sysDictItemRepository.findByDictTypeIgnoreCase("CONTRACT_FIELD")
                .stream()
                .filter(item -> fieldKeys.contains(item.getItemKey()))
                .collect(Collectors.toList());

        Map<String, SysDictItem> dictMap = new HashMap<>();
        for (SysDictItem item : dictItems) {
            dictMap.put(item.getItemKey(), item);
        }

        List<AdminTemplateFieldBindDto> result = new ArrayList<>();
        for (TemplateFieldBind bind : binds) {
            SysDictItem dict = dictMap.get(bind.getFieldKey());

            result.add(AdminTemplateFieldBindDto.builder()
                    .bindId(bind.getId())
                    .templateId(bind.getTemplateId())
                    .fieldKey(bind.getFieldKey())
                    .itemKey(bind.getFieldKey())
                    .itemName(dict == null ? bind.getFieldKey() : dict.getItemName())
                    .valueType(dict == null ? "" : dict.getValueType())
                    .moduleName(dict == null ? "" : dict.getModuleName())
                    .requiredFlag(Boolean.TRUE.equals(bind.getRequiredFlag()))
                    .itemValue(dict == null ? "" : dict.getItemValue())
                    .status(bind.getStatus())
                    .sortOrder(bind.getSortOrder())
                    .build());
        }

        return result;
    }
}