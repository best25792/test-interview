package com.example.paymentservice.client.api;

import com.example.paymentservice.client.dto.DeductRequest;
import com.example.paymentservice.client.dto.TopUpRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

public interface WalletApi {

    @PostExchange("/api/v1/wallets/users/{userId}/deduct")
    void deduct(@PathVariable("userId") Long userId, @RequestBody DeductRequest body);

    @PostExchange("/api/v1/wallets/users/{userId}/topup")
    void topUp(@PathVariable("userId") Long userId, @RequestBody TopUpRequest body);
}
