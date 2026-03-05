package com.contract.contract_backend.service;

import com.contract.contract_backend.dto.LoginRequest;
import com.contract.contract_backend.dto.LoginResponse;
import com.contract.contract_backend.dto.RegisterRequest;

public interface AuthService {

    void register(RegisterRequest req);

    LoginResponse login(LoginRequest req);
}