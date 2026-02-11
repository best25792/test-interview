package com.example.paymentservice.mapper;

import com.example.paymentservice.client.dto.WalletResponse;
import com.example.paymentservice.domain.model.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public Wallet toDomain(WalletResponse response) {
        if (response == null) return null;
        return Wallet.builder()
                .id(response.id())
                .userId(response.userId())
                .balance(response.balance())
                .currency(response.currency())
                .active(Boolean.TRUE.equals(response.isActive()))
                .createdAt(response.createdAt())
                .updatedAt(response.updatedAt())
                .build();
    }
}
