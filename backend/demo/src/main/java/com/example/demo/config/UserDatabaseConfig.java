package com.example.demo.config;

import com.example.demo.entity.User;
import com.example.demo.entity.Wallet;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Configuration for User database (user_db)
 * Contains: User, Wallet entities
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.example.demo.repository.user",
    entityManagerFactoryRef = "userEntityManagerFactory",
    transactionManagerRef = "userTransactionManager"
)
@EntityScan(basePackageClasses = {User.class, Wallet.class})
public class UserDatabaseConfig {
    // Configuration handled by DatabaseConfig
}
