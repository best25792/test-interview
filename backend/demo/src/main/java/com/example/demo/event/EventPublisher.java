package com.example.demo.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Central event publisher service for microservice-style event-based communication
 * In a real microservices architecture, this would publish to a message broker (Kafka, RabbitMQ, etc.)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publishPaymentCreated(PaymentCreatedEvent event) {
        log.info("Publishing PaymentCreatedEvent for paymentId: {}", event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishQRCodeGenerated(QRCodeGeneratedEvent event) {
        log.info("Publishing QRCodeGeneratedEvent for paymentId: {}", event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        log.info("Publishing PaymentProcessedEvent for paymentId: {}", event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Publishing PaymentConfirmedEvent for paymentId: {}", event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishPaymentRefunded(PaymentRefundedEvent event) {
        log.info("Publishing PaymentRefundedEvent for paymentId: {}", event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishQRCodeValidated(QRCodeValidatedEvent event) {
        log.info("Publishing QRCodeValidatedEvent for qrCodeId: {}", event.getQrCodeId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishQRCodeUsed(QRCodeUsedEvent event) {
        log.info("Publishing QRCodeUsedEvent for qrCodeId: {}", event.getQrCodeId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishWalletDeducted(WalletDeductedEvent event) {
        log.info("Publishing WalletDeductedEvent for userId: {}, paymentId: {}", 
                event.getUserId(), event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishHoldCreated(HoldCreatedEvent event) {
        log.info("Publishing HoldCreatedEvent for holdId: {}, paymentId: {}", 
                event.getHoldId(), event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishHoldCaptured(HoldCapturedEvent event) {
        log.info("Publishing HoldCapturedEvent for holdId: {}, paymentId: {}", 
                event.getHoldId(), event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }

    public void publishHoldReleased(HoldReleasedEvent event) {
        log.info("Publishing HoldReleasedEvent for holdId: {}, paymentId: {}", 
                event.getHoldId(), event.getPaymentId());
        applicationEventPublisher.publishEvent(event);
    }
}
