package com.example.demo.config;

import com.example.demo.entity.EventOutbox;
import com.example.demo.entity.Payment;
import com.example.demo.entity.Transaction;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for Payment database (payment_db) - PRIMARY
 * Contains: Payment, Transaction, EventOutbox entities
 */
@Configuration
@Primary
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.payment",
    entityManagerFactoryRef = "paymentEntityManagerFactory",
    transactionManagerRef = "paymentTransactionManager"
)
@EntityScan(basePackageClasses = {Payment.class, Transaction.class, EventOutbox.class})
public class PaymentDatabaseConfig {
    // Configuration handled by DatabaseConfig
}
