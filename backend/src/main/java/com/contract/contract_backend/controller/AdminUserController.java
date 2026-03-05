package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.AdminUserDto;
import com.contract.contract_backend.dto.PageResult;
import com.contract.contract_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    // ✅ 用户分页列表
    // GET /api/admin/users?page=1&size=10&keyword=
    @GetMapping
    public PageResult<AdminUserDto.UserRow> pageUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        return userService.pageUsers(page, size, keyword);
    }

    // ✅ 统计卡片
    // GET /api/admin/users/stats
    @GetMapping("/stats")
    public AdminUserDto.Stats stats() {
        return userService.stats();
    }

    // ✅ 更新用户（角色/状态）
    // PUT /api/admin/users/{id}
    @PutMapping("/{id}")
    public AdminUserDto.UserRow updateUser(@PathVariable("id") Long id,
                                           @RequestBody AdminUserDto.UpdateReq req) {
        return userService.updateUser(id, req);
    }
}