package com.example.paymentservice;

import com.example.paymentservice.client.api.QRCodeApi;
import com.example.paymentservice.client.api.UserApi;
import com.example.paymentservice.client.api.WalletApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.service.registry.ImportHttpServices;

@SpringBootApplication
@EnableScheduling
@ImportHttpServices(group = "wallet", types = WalletApi.class)
@ImportHttpServices(group = "qrCode", types = QRCodeApi.class)
@ImportHttpServices(group = "user", types = UserApi.class)
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
