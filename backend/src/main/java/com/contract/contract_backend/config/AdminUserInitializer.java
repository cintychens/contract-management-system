package com.contract.contract_backend.config;

import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 可以在 application.yaml 配置（也可以先写死）
    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin123456}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 已存在就不重复创建
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        User admin = User.builder()
                .username(adminUsername)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .roleCode("ADMIN")
                .status("ENABLED")
                .build();

        userRepository.save(admin);

        System.out.println("✅ Default admin created: " + adminUsername);
    }
}