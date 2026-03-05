package com.contract.contract_backend.controller;

import com.contract.contract_backend.dto.LoginRequest;
import com.contract.contract_backend.dto.LoginResponse;
import com.contract.contract_backend.dto.RegisterRequest;
import com.contract.contract_backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return "OK";
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @GetMapping("/me")
    public Object me(Authentication authentication) {
        return authentication == null
                ? "NOT_LOGIN"
                : authentication.getName();
    }
}