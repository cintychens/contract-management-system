package com.contract.contract_backend.controller;

import com.contract.contract_backend.entity.User;
import com.contract.contract_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;

    /**
     * 用户管理（简版）- 分页查询用户
     * GET /api/admin/users-simple?page=1&size=10
     */
    @GetMapping("/users-simple")
    public Map<String, Object> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int p = Math.max(page - 1, 0);
        int s = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "userId"));
        Page<User> result = userRepository.findAll(pageable);

        return Map.of(
                "items", result.getContent(),
                "page", page,
                "size", s,
                "total", result.getTotalElements()
        );
    }
}