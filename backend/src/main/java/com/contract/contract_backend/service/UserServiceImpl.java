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
    public PageResult<AdminUserDto.UserRow> pageUsers(int page, int size, String keyword) {
        // 前端一般 page 从 1 开始，这里转换成 0 开始
        int pageIndex = Math.max(page, 1) - 1;
        int pageSize = Math.max(size, 1);

        Pageable pageable = PageRequest.of(
                pageIndex,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<User> p;
        if (keyword == null || keyword.trim().isEmpty()) {
            p = userRepository.findAll(pageable);
        } else {
            p = userRepository.findByUsernameContainingIgnoreCase(keyword.trim(), pageable);
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

        return AdminUserDto.Stats.builder()
                .total(total)
                .enabled(enabled)
                .disabled(disabled)
                .build();
    }

    @Override
    @Transactional
    public AdminUserDto.UserRow updateUser(Long userId, AdminUserDto.UpdateReq req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + userId));

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
                .roleCode(u.getRoleCode())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}