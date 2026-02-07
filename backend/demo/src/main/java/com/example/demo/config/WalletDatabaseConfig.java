package com.example.demo.config;

import com.example.demo.entity.Hold;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for Wallet database (wallet_db)
 * Contains: Hold entity
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.wallet",
    entityManagerFactoryRef = "walletEntityManagerFactory",
    transactionManagerRef = "walletTransactionManager"
)
@EntityScan(basePackageClasses = {Hold.class})
public class WalletDatabaseConfig {
    // Configuration handled by DatabaseConfig
}
