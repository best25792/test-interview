package com.example.paymentservice.client.api;

import com.example.paymentservice.client.dto.UserResponse;
import com.example.paymentservice.client.dto.WalletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

public interface UserApi {

    @GetExchange("/api/v1/users/{id}")
    UserResponse getUser(@PathVariable("id") Long id);

    @GetExchange("/api/v1/users/{id}/wallet")
    WalletResponse getWallet(@PathVariable("id") Long id);
}
