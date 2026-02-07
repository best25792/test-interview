package com.example.demo.aop;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;

/**
 * Aspect that automatically creates OpenTelemetry spans for service method calls
 * This provides automatic distributed tracing for all service layer methods
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TracingAspect {

    private final OpenTelemetry openTelemetry;
    private Tracer tracer;

    @PostConstruct
    public void init() {
        this.tracer = openTelemetry.getTracer("demo-service", "1.0.0");
    }

    /**
     * Intercepts all public methods in classes annotated with @Service
     */
    @Around("execution(public * com.example.demo.service.*.*(..))")
    public Object traceServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = method.getName();
        
        // Create span name: ServiceName.methodName
        String spanName = className + "." + methodName;
        
        // Get current context
        Context parentContext = Context.current();
        
        // Create a new span as a child of the current span
        Span span = tracer
                .spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parentContext)
                .startSpan();
        
        // Add method information as span attributes
        span.setAttribute("code.function", methodName);
        span.setAttribute("code.namespace", className);
        span.setAttribute("code.filepath", joinPoint.getTarget().getClass().getName());
        
        // Add method parameters if available (limit to avoid too much data)
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (int i = 0; i < Math.min(args.length, 5); i++) { // Limit to first 5 parameters
                Object arg = args[i];
                if (arg != null) {
                    String paramName = "method.param." + i;
                    String paramValue = arg.toString();
                    // Truncate long values
                    if (paramValue.length() > 200) {
                        paramValue = paramValue.substring(0, 200) + "...";
                    }
                    span.setAttribute(paramName, paramValue);
                    
                    // Special handling for payment_id if found in parameters
                    if (arg instanceof Long && i == 0 && methodName.toLowerCase().contains("payment")) {
                        span.setAttribute("payment.id", (Long) arg);
                        span.setAttribute("payment_id", arg.toString());
                    }
                }
            }
        }
        
        // Make the span the current span
        try (Scope scope = span.makeCurrent()) {
            try {
                // Execute the method
                Object result = joinPoint.proceed();
                
                // Mark span as successful
                span.setAttribute("method.success", true);
                
                return result;
            } catch (Throwable throwable) {
                // Mark span as failed
                span.setAttribute("method.success", false);
                span.setAttribute("error", true);
                span.setAttribute("error.type", throwable.getClass().getName());
                span.setAttribute("error.message", throwable.getMessage() != null ? 
                        throwable.getMessage() : "Unknown error");
                
                // Record exception
                span.recordException(throwable);
                
                throw throwable;
            } finally {
                span.end();
            }
        }
    }
}
