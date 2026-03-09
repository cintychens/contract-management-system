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

        if ("OPTIONS".equalsIgnoreCase(method)) return true;

        if ("/api/auth/login".equals(path)) return true;
        if ("/api/auth/register".equals(path)) return true;

        if (path.startsWith("/auth/")) return true;
        if (path.startsWith("/dashboard/")) return true;
        if (path.startsWith("/admin/")) return true;
        if ("/".equals(path) || "/index.html".equals(path)) return true;

        if (path.startsWith("/assets/")) return true;
        if (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/")) return true;

        if (path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".js")
                || path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg")
                || path.endsWith(".svg") || path.endsWith(".ico") || path.endsWith(".woff")
                || path.endsWith(".woff2") || path.endsWith(".ttf") || path.endsWith(".map")) return true;

        if (path.startsWith("/h2-console")) return true;

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        String path = request.getServletPath();
        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        System.out.println("========== JWT FILTER ==========");
        System.out.println("PATH = " + path);
        System.out.println("AUTH HEADER = " + auth);

        if (auth == null || !auth.startsWith("Bearer ")) {
            System.out.println("NO VALID BEARER HEADER");
            chain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7).trim();
        System.out.println("TOKEN = " + token);

        try {
            boolean valid = jwtUtil.isValid(token);
            System.out.println("JWT VALID = " + valid);

            if (valid) {
                String username = jwtUtil.getUsername(token);
                String role = jwtUtil.getRole(token);

                System.out.println("USERNAME = " + username);
                System.out.println("ROLE = " + role);

                String authority = null;
                if (role != null && !role.isBlank()) {
                    authority = role.trim().toUpperCase();
                }

                var authorities = (authority == null)
                        ? List.<SimpleGrantedAuthority>of()
                        : List.of(new SimpleGrantedAuthority(authority));

                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("AUTHENTICATION SET SUCCESS");
            } else {
                System.out.println("JWT INVALID");
            }
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            System.out.println("JWT PARSE ERROR = " + ex.getMessage());
            ex.printStackTrace();
        }

        chain.doFilter(request, response);
    }
}