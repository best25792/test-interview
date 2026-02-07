package com.example.demo.config;

import com.example.demo.entity.QRCode;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for QR database (qr_db)
 * Contains: QRCode entity
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.qr",
    entityManagerFactoryRef = "qrEntityManagerFactory",
    transactionManagerRef = "qrTransactionManager"
)
@EntityScan(basePackageClasses = {QRCode.class})
public class QRDatabaseConfig {
    // Configuration handled by DatabaseConfig
}
