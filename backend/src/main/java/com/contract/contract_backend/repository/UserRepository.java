package com.contract.contract_backend.repository;

import com.contract.contract_backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 登录用
    Optional<User> findByUsername(String username);

    // 初始化管理员用
    boolean existsByUsername(String username);

    // 关键词搜索
    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    // ⭐ 按角色筛选
    Page<User> findByRoleCode(String roleCode, Pageable pageable);

    // ⭐ 关键词 + 角色
    Page<User> findByUsernameContainingIgnoreCaseAndRoleCode(String keyword, String roleCode, Pageable pageable);

    // 统计
    long countByStatus(String status);
    long countByRoleCode(String roleCode);
}