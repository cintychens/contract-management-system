package com.contract.contract_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度需在3~32之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6~64之间")
    private String password;

    // ⭐⭐⭐ 新增这一行
    private String roleCode;

    // ===== getter =====
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRoleCode() { return roleCode; }   // ⭐ 新增

    // ===== setter =====
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; } // ⭐ 新增
}