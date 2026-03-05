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

        User user = User.builder()
                .username(req.getUsername())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .roleCode("USER")
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

        String token = jwtUtil.generateToken(
                user.getUserId(),
                user.getUsername(),
                user.getRoleCode()
        );

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRoleCode())
                .build();
    }
}