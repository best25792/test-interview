package com.example.paymentservice.service.outbox;

import com.example.paymentservice.kafka.KafkaEventProducer;
import com.example.paymentservice.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Consumes PaymentCreatedEvent from the outbox: deserializes and publishes to Kafka.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentCreatedOutboxEventConsumer implements OutboxEventConsumer {

    public static final String EVENT_TYPE = "PaymentCreatedEvent";

    private final ObjectMapper objectMapper;
    private final KafkaEventProducer kafkaEventProducer;

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public void consume(String eventData, String traceparent) throws Exception {
        PaymentService.PaymentCreatedEvent payload = objectMapper.readValue(
                eventData, PaymentService.PaymentCreatedEvent.class);
        kafkaEventProducer.publishPaymentCreated(payload, traceparent);
    }
}
