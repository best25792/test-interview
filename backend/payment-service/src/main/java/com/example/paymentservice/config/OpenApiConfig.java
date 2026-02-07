package com.example.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Payment Service API")
                        .version("1.0.0")
                        .description("""
                                Payment Service manages the full payment lifecycle:
                                
                                1. **Initiate** - Customer starts a payment, QR code generated async
                                2. **Process** - Merchant scans QR, wallet deducted atomically
                                3. **Cancel** - Cancel pending/ready payments
                                4. **Refund** - Refund completed payments (full or partial)
                                
                                ### Key Features
                                - **Idempotency Keys** - Prevent duplicate charges on client retry
                                - **Structured Error Codes** - Programmatic error handling (e.g. INSUFFICIENT_BALANCE)
                                - **Async QR Generation** - Poll status endpoint for QR readiness
                                """)
                        .contact(new Contact()
                                .name("Payment Team")))
                .servers(List.of(
                        new Server().url("http://localhost:8083").description("Local development"),
                        new Server().url("http://payment-service:8083").description("Docker network")
                ));
    }
}
