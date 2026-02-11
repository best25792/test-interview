package com.example.paymentservice.client.dto;

import java.math.BigDecimal;

public record TopUpRequest(BigDecimal amount) {}
