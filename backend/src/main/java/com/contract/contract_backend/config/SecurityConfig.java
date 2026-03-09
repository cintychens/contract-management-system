package com.contract.contract_backend.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())

                // ✅ 未登录 / 无权限时返回 JSON（不返回 Whitelabel）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":401,\"message\":\"NOT_LOGIN\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":403,\"message\":\"Access Denied\"}");
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // 放行登录/注册
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()

                        // /me 必须登录
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // ✅ 临时放行：模板智能生成相关接口（方便你当前开发测试）
                        .requestMatchers(HttpMethod.GET, "/api/templates/*/variables").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/templates/generate").permitAll()

                        // H2
                        .requestMatchers("/h2-console/**").permitAll()

                        // 静态资源
                        .requestMatchers(
                                "/", "/index.html", "/error", "/favicon.ico",
                                "/auth/**", "/dashboard/**", "/admin/**", "/assets/**",
                                "/**/*.html", "/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg",
                                "/**/*.jpeg", "/**/*.svg", "/**/*.ico", "/**/*.woff", "/**/*.woff2", "/**/*.ttf"
                        ).permitAll()

                        // ✅ 管理员接口：精确匹配 authority=ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // 其他 API
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}