package com.example.paymentservice.bdd;

import com.example.paymentservice.client.QRCodeClientService;
import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import com.example.paymentservice.kafka.KafkaEventProducer;
import com.example.paymentservice.service.OutboxProcessor;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("test")
public class CucumberSpringConfig {

    // Mock external service clients (no real HTTP calls in tests)
    @MockitoBean
    UserClientService userClientService;

    @MockitoBean
    QRCodeClientService qrCodeClientService;

    @MockitoBean
    WalletClientService walletClientService;

    // Mock Kafka-dependent beans (Kafka infra excluded via @Profile("!test"))
    @MockitoBean
    KafkaEventProducer kafkaEventProducer;

    @MockitoBean
    OutboxProcessor outboxProcessor;
}
