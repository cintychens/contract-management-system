package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.AdminUserDto;
import com.contract.contract_backend.dto.PageResult;
import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public PageResult<AdminUserDto.UserRow> pageUsers(int page, int size, String keyword, String role) {
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String kw = (keyword == null) ? "" : keyword.trim();
        String r = (role == null) ? "" : role.trim().toUpperCase(); // USER / ADMIN / ALL / ""

        boolean hasKw = !kw.isEmpty();
        boolean hasRole = !r.isEmpty() && !"ALL".equals(r);

        Page<User> p;

        if (hasKw && hasRole) {
            // 同时按用户名 + 角色筛选
            p = userRepository.findByUsernameContainingIgnoreCaseAndRoleCode(kw, r, pageable);
        } else if (hasKw) {
            p = userRepository.findByUsernameContainingIgnoreCase(kw, pageable);
        } else if (hasRole) {
            p = userRepository.findByRoleCode(r, pageable);
        } else {
            p = userRepository.findAll(pageable);
        }

        List<AdminUserDto.UserRow> rows = p.getContent().stream()
                .map(this::toRow)
                .toList();

        return PageResult.of(rows, p.getTotalElements(), page, pageSize);
    }

    @Override
    public AdminUserDto.Stats stats() {
        long total = userRepository.count();
        long enabled = userRepository.countByStatus("ENABLED");
        long disabled = userRepository.countByStatus("DISABLED");

        long adminCount = userRepository.countByRoleCode("ADMIN");
        long userCount = userRepository.countByRoleCode("USER");

        return AdminUserDto.Stats.builder()
                .total(total)
                .enabled(enabled)
                .disabled(disabled)
                .adminCount(adminCount)
                .userCount(userCount)
                .build();
    }

    @Override
    @Transactional
    public AdminUserDto.UserRow updateUser(Long userId, AdminUserDto.UpdateReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

        if (req.getFullName() != null) {
            user.setFullName(req.getFullName().trim());
        }

        if (req.getRemark() != null) {
            user.setRemark(req.getRemark().trim());
        }

        if (req.getRoleCode() != null && !req.getRoleCode().isBlank()) {
            user.setRoleCode(req.getRoleCode().trim().toUpperCase()); // USER / ADMIN
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            user.setStatus(req.getStatus().trim().toUpperCase()); // ENABLED / DISABLED
        }

        // updatedAt 由 @PreUpdate 自动更新
        User saved = userRepository.save(user);
        return toRow(saved);
    }

    private AdminUserDto.UserRow toRow(User u) {
        return AdminUserDto.UserRow.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .fullName(u.getFullName())
                .remark(u.getRemark())
                .roleCode(u.getRoleCode())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }
}