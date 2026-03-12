package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminDictDto;

import java.util.Map;

public interface SysDictItemService {

    /**
     * 分页查询字典项
     */
    Map<String, Object> pageDictItems(int page, int size, String dictType, String keyword, String status);

    /**
     * 查询单条详情
     */
    AdminDictDto.Row getDictItemDetail(Long id);

    /**
     * 新增字典项
     */
    AdminDictDto.Row createDictItem(AdminDictDto.SaveReq req);

    /**
     * 修改字典项
     */
    AdminDictDto.Row updateDictItem(Long id, AdminDictDto.SaveReq req);

    /**
     * 启用 / 禁用
     */
    void changeStatus(Long id, String status);
}