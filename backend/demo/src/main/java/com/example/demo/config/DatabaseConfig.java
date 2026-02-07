package com.example.demo.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * Configuration for multiple datasources - one for each microservice database
 * 
 * Database mapping:
 * - user_db: User, Wallet entities
 * - wallet_db: Hold entity
 * - payment_db: Payment, Transaction, EventOutbox entities (PRIMARY)
 * - qr_db: QRCode entity
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    // ========== USER DATASOURCE ==========
    @Bean(name = "userDataSource")
    @ConfigurationProperties("spring.datasource.user")
    public DataSource userDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "userEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean userEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("userDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.demo.entity.User", "com.example.demo.entity.Wallet")
                .persistenceUnit("user")
                .build();
    }

    @Bean(name = "userTransactionManager")
    public PlatformTransactionManager userTransactionManager(
            @Qualifier("userEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ========== WALLET DATASOURCE ==========
    @Bean(name = "walletDataSource")
    @ConfigurationProperties("spring.datasource.wallet")
    public DataSource walletDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "walletEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean walletEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("walletDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.demo.entity.Hold")
                .persistenceUnit("wallet")
                .build();
    }

    @Bean(name = "walletTransactionManager")
    public PlatformTransactionManager walletTransactionManager(
            @Qualifier("walletEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ========== PAYMENT DATASOURCE (PRIMARY) ==========
    @Primary
    @Bean(name = "paymentDataSource")
    @ConfigurationProperties("spring.datasource.payment")
    public DataSource paymentDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Primary
    @Bean(name = "paymentEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean paymentEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("paymentDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.demo.entity.Payment", 
                         "com.example.demo.entity.Transaction",
                         "com.example.demo.entity.EventOutbox")
                .persistenceUnit("payment")
                .build();
    }

    @Primary
    @Bean(name = "paymentTransactionManager")
    public PlatformTransactionManager paymentTransactionManager(
            @Qualifier("paymentEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ========== QR DATASOURCE ==========
    @Bean(name = "qrDataSource")
    @ConfigurationProperties("spring.datasource.qr")
    public DataSource qrDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .build();
    }

    @Bean(name = "qrEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean qrEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("qrDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.example.demo.entity.QRCode")
                .persistenceUnit("qr")
                .build();
    }

    @Bean(name = "qrTransactionManager")
    public PlatformTransactionManager qrTransactionManager(
            @Qualifier("qrEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
