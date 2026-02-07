package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Filter to extract payment_id from request path/body and add to trace context
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PaymentIdFilter extends OncePerRequestFilter {

    private final TracingConfig tracingConfig;
    
    // Pattern to match payment ID in URL path: /api/v1/payments/{id} or /api/v1/payments/{id}/process, etc.
    private static final Pattern PAYMENT_ID_PATH_PATTERN = Pattern.compile("/api/v1/payments/(\\d+)");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Extract payment_id from URL path
        String path = request.getRequestURI();
        Long paymentId = extractPaymentIdFromPath(path);
        
        // If not found in path, try to extract from request body for POST requests
        if (paymentId == null && "POST".equalsIgnoreCase(request.getMethod())) {
            // For initiate payment, we'll extract it after the payment is created
            // For now, we'll handle it in the service layer
        }
        
        if (paymentId != null) {
            tracingConfig.addPaymentIdToContext(paymentId);
            log.debug("Added payment_id {} to trace context for request: {}", paymentId, path);
        }
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up MDC after request
            if (paymentId != null) {
                tracingConfig.clearPaymentIdFromContext();
            }
        }
    }

    private Long extractPaymentIdFromPath(String path) {
        Matcher matcher = PAYMENT_ID_PATH_PATTERN.matcher(path);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                log.warn("Failed to parse payment_id from path: {}", path);
            }
        }
        return null;
    }
}
