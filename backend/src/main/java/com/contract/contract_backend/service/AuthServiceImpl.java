package com.contract.contract_backend.service.impl;

import com.contract.contract_backend.common.utils.JwtUtil;
import com.contract.contract_backend.dto.LoginRequest;
import com.contract.contract_backend.dto.LoginResponse;
import com.contract.contract_backend.dto.RegisterRequest;
import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.UserRepository;
import com.contract.contract_backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public void register(RegisterRequest req) {

        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }

        // ⭐ 允许角色列表
        String role = req.getRoleCode();

        if (role == null || role.isBlank()) {
            role = "USER";
        } else {
            role = role.toUpperCase();

            // ⭐ 安全限制（防止乱传）
            if (!role.equals("BUSINESS") &&
                    !role.equals("LEGAL") &&
                    !role.equals("FINANCE") &&
                    !role.equals("APPROVER") &&
                    !role.equals("USER")) {

                role = "USER";
            }
        }

        User user = User.builder()
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .roleCode(role)   // ✅ 用前端传的
                .status("ENABLED")
                .build();

        userRepository.save(user);
    }

    @Override
    public LoginResponse login(LoginRequest req) {

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!"ENABLED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        boolean ok = passwordEncoder.matches(
                req.getPassword(),
                user.getPasswordHash()
        );

        if (!ok) throw new RuntimeException("密码错误");

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRoleCode()
        );

        return LoginResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .roleCode(user.getRoleCode())
                .status(user.getStatus())
                .build();
    }
}