package com.example.paymentservice.client.api;

import com.example.paymentservice.dto.request.CreateQRCodeRequest;
import com.example.paymentservice.dto.request.ValidateQRCodeRequest;
import com.example.paymentservice.dto.response.QRCodeResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

public interface QRCodeApi {

    @PostExchange("/api/v1/qrcodes")
    QRCodeResponse create(@RequestBody CreateQRCodeRequest body);

    @GetExchange("/api/v1/qrcodes/{id}")
    QRCodeResponse getById(@PathVariable("id") Long id);

    @PostExchange("/api/v1/qrcodes/validate")
    QRCodeResponse validate(@RequestBody ValidateQRCodeRequest body);
}
