package com.example.demo.service;

import com.example.demo.entity.Transaction;
import com.example.demo.entity.TransactionStatus;
import com.example.demo.entity.TransactionType;
import com.example.demo.event.PaymentConfirmedEvent;
import com.example.demo.event.PaymentProcessedEvent;
import com.example.demo.event.PaymentRefundedEvent;
import com.example.demo.repository.payment.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Transaction Service Event Listener
 * Simulated as a separate microservice that listens to payment events
 * and creates transaction records
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionEventListener {

    private final TransactionRepository transactionRepository;

    /**
     * Listen to PaymentProcessedEvent and create transaction
     */
    @EventListener
    @Async
    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        log.info("TransactionService: Received PaymentProcessedEvent for paymentId: {}", event.getPaymentId());
        
        Transaction transaction = Transaction.builder()
                .paymentId(event.getPaymentId())
                .type(TransactionType.PAYMENT)
                .amount(event.getAmount())
                .status(TransactionStatus.SUCCESS)
                .reference(UUID.randomUUID().toString())
                .description("Payment processed successfully via QR code")
                .timestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("TransactionService: Transaction created for paymentId: {}", event.getPaymentId());
    }

    /**
     * Listen to PaymentConfirmedEvent and create transaction
     */
    @EventListener
    @Async
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("TransactionService: Received PaymentConfirmedEvent for paymentId: {}", event.getPaymentId());
        
        Transaction transaction = Transaction.builder()
                .paymentId(event.getPaymentId())
                .type(TransactionType.PAYMENT)
                .amount(event.getAmount())
                .status(TransactionStatus.SUCCESS)
                .reference(UUID.randomUUID().toString())
                .description("Payment confirmed")
                .timestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("TransactionService: Transaction created for confirmed paymentId: {}", event.getPaymentId());
    }

    /**
     * Listen to PaymentRefundedEvent and create refund transaction
     */
    @EventListener
    @Async
    @Transactional
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        log.info("TransactionService: Received PaymentRefundedEvent for paymentId: {}", event.getPaymentId());
        
        Transaction transaction = Transaction.builder()
                .paymentId(event.getPaymentId())
                .type(TransactionType.REFUND)
                .amount(event.getRefundAmount())
                .status(TransactionStatus.SUCCESS)
                .reference(UUID.randomUUID().toString())
                .description(event.getReason() != null ? event.getReason() : "Refund processed")
                .timestamp(LocalDateTime.now())
                .build();

        transactionRepository.save(transaction);
        log.info("TransactionService: Refund transaction created for paymentId: {}", event.getPaymentId());
    }
}
