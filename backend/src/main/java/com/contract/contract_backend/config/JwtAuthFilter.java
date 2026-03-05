package com.contract.contract_backend.config;

import com.contract.contract_backend.common.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        String method = request.getMethod();

        // 1) 预检请求直接跳过
        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        // 2) 只跳过登录/注册
        if ("/api/auth/login".equals(path)) return true;
        if ("/api/auth/register".equals(path)) return true;

        // 3) 静态页面路由跳过
        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/dashboard/")) return true;
        if (path.startsWith("/admin/")) return true;
        if ("/".equals(path) || "/index.html".equals(path)) return true;

        // 4) 静态资源跳过
        if (path.startsWith("/assets/")) return true;
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")) return true;

        if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".svg") || path.endsWith(".ico") || path.endsWith(".woff")
                || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".map")) return true;

        // 5) H2 console 跳过
        if (path.startsWith("/h2-console")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 没有 token → 直接放行，交给 Security 判断是否需要登录
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7);

        try {
            if (jwtUtil.isValid(token)) {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);

                // ✅ authority 统一：ADMIN / USER（不带 ROLE_）
                String authority = null;
                if (role != null && !role.isBlank()) {
                    authority = role.trim().toUpperCase();
                }

                // ✅ Debug（临时用：确定后可删除）
                System.out.println(">>> PATH=" + request.getServletPath());
                System.out.println(">>> username=" + username);
                System.out.println(">>> roleClaim=" + role);
                System.out.println(">>> authority=" + authority);

                var authorities = (authority == null)
                        ? List.<SimpleGrantedAuthority>of()
                        : List.of(new SimpleGrantedAuthority(authority));

                var authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 可选 Debug：token 不合法
                System.out.println(">>> JWT invalid, path=" + request.getServletPath());
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            System.out.println(">>> JWT parse error, path=" + request.getServletPath() + ", err=" + ex.getMessage());
        }

        chain.doFilter(request, response);
    }
}