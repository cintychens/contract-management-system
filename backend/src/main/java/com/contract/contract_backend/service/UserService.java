package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminUserDto;
import com.contract.contract_backend.dto.PageResult;

public interface UserService {

    PageResult<AdminUserDto.UserRow> pageUsers(int page, int size, String keyword, String role);

    AdminUserDto.Stats stats();

    AdminUserDto.UserRow updateUser(Long userId, AdminUserDto.UpdateReq req);
}