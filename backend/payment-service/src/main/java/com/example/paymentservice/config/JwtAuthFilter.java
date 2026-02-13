package com.example.paymentservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Validates JWT Bearer token for protected paths and sets request attribute "userId".
 * Returns 401 when token is missing or invalid.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTR_USER_ID = "userId";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";

    private final JwtValidator jwtValidator;
    private final List<String> protectedPathPrefixes;

    public JwtAuthFilter(JwtValidator jwtValidator, List<String> protectedPathPrefixes) {
        this.jwtValidator = jwtValidator;
        this.protectedPathPrefixes = protectedPathPrefixes;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        if (!isProtectedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        String auth = request.getHeader(AUTHORIZATION);
        if (auth == null || !auth.startsWith(BEARER)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }
        String token = auth.substring(BEARER.length()).trim();
        try {
            Long userId = jwtValidator.validateAccessTokenAndGetUserId(token);
            request.setAttribute(REQUEST_ATTR_USER_ID, userId);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid or expired token\"}");
        }
    }

    private boolean isProtectedPath(String path) {
        return protectedPathPrefixes.stream().anyMatch(path::startsWith);
    }
}
