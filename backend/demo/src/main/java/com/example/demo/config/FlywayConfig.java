package com.example.demo.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Flyway configuration for multiple databases
 */
@Configuration
public class FlywayConfig {

    @Bean(initMethod = "migrate")
    @DependsOn("userDataSource")
    public Flyway userFlyway(@Qualifier("userDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/user")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean(initMethod = "migrate")
    @DependsOn("walletDataSource")
    public Flyway walletFlyway(@Qualifier("walletDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/wallet")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean(initMethod = "migrate")
    @DependsOn("paymentDataSource")
    public Flyway paymentFlyway(@Qualifier("paymentDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/payment")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean(initMethod = "migrate")
    @DependsOn("qrDataSource")
    public Flyway qrFlyway(@Qualifier("qrDataSource") DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration/qr")
                .baselineOnMigrate(true)
                .load();
    }
}
