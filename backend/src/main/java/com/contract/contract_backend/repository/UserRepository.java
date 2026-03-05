package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ 登录用
    Optional<User> findByUsername(String username);

    // ✅ 初始化管理员用
    boolean existsByUsername(String username);

    // ✅ 分页搜索
    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    // ✅ 统计
    long countByStatus(String status);     // ENABLED / DISABLED
    long countByRoleCode(String roleCode); // USER / ADMIN
}