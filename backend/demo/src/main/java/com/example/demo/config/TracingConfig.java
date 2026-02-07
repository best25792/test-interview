package com.example.demo.config;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Configuration for distributed tracing with payment_id correlation
 * Adds payment_id to both trace spans and MDC for log correlation
 */
@Component
@RequiredArgsConstructor
public class TracingConfig {

    /**
     * Add payment_id to current span and MDC for log correlation
     */
    public void addPaymentIdToContext(Long paymentId) {
        if (paymentId != null) {
            // Add to MDC for log correlation
            MDC.put("payment_id", paymentId.toString());
            
            // Add to current span as attribute
            Span currentSpan = Span.current();
            if (currentSpan.isRecording()) {
                currentSpan.setAttribute("payment.id", paymentId);
                currentSpan.setAttribute("payment_id", paymentId.toString()); // Also as string for Loki queries
            }
        }
    }

    /**
     * Remove payment_id from MDC
     */
    public void clearPaymentIdFromContext() {
        MDC.remove("payment_id");
    }

    /**
     * Get current payment_id from MDC
     */
    public String getCurrentPaymentId() {
        return MDC.get("payment_id");
    }
}
