package com.example.paymentservice.mapper;

import com.example.paymentservice.domain.model.Transaction;
import com.example.paymentservice.dto.response.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    /** Map JPA entity to domain using builder. */
    public Transaction toDomain(com.example.paymentservice.entity.Transaction entity) {
        if (entity == null) return null;
        return Transaction.builder()
                .id(entity.getId())
                .paymentId(entity.getPaymentId())
                .type(entity.getType())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .reference(entity.getReference())
                .externalReference(entity.getExternalReference())
                .timestamp(entity.getTimestamp())
                .description(entity.getDescription())
                .build();
    }

    /** Map domain to API response. */
    public TransactionResponse toResponse(Transaction domain) {
        if (domain == null) return null;
        return TransactionResponse.builder()
                .id(domain.getId())
                .paymentId(domain.getPaymentId())
                .type(domain.getType())
                .amount(domain.getAmount())
                .status(domain.getStatus())
                .reference(domain.getReference())
                .externalReference(domain.getExternalReference())
                .timestamp(domain.getTimestamp())
                .description(domain.getDescription())
                .build();
    }
}
