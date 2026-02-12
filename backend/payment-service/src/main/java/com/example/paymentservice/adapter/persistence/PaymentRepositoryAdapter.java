package com.example.paymentservice.adapter.persistence;

import com.example.paymentservice.domain.model.Payment;
import com.example.paymentservice.entity.PaymentEntity;
import com.example.paymentservice.entity.PaymentStatus;
import com.example.paymentservice.mapper.PaymentMapper;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA adapter for payment persistence. Implements the port and delegates to Spring Data JPA.
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository  {

    private final SpringDataPaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentRepository.findById(id).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return paymentRepository.findByIdempotencyKey(idempotencyKey).map(paymentMapper::toDomain);
    }

    @Override
    public Optional<Payment> findByIdAndStatus(Long id, PaymentStatus status) {
        return paymentRepository.findByIdAndStatus(id, status).map(paymentMapper::toDomain);
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        return paymentRepository.findByStatus(status).stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findByMerchantId(String merchantId) {
        return List.of();
    }

    @Override
    public List<Payment> findByCustomerId(String customerId) {
        return paymentRepository.findByCustomerId(customerId).stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

    @Override
    public List<Payment> findAll() {
        return paymentRepository.findAll().stream()
                .map(paymentMapper::toDomain)
                .toList();
    }

    @Override
    public Payment save(Payment payment) {
        var entity = paymentMapper.toEntity(payment);
        var saved = paymentRepository.save(entity);
        return paymentMapper.toDomain(saved);
    }

    @Override
    public Boolean existsById(Long id) {
        return paymentRepository.existsById(id);
    }
}

interface SpringDataPaymentRepository extends JpaRepository<PaymentEntity, Long> {
    List<PaymentEntity> findByStatus(PaymentStatus status);

    List<PaymentEntity> findByCustomerId(String customerId);

    Optional<PaymentEntity> findByIdAndStatus(Long id, PaymentStatus status);

    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);
}
