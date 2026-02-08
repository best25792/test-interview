package com.example.paymentservice.bdd.steps;

import com.example.paymentservice.client.ServiceUnavailableException;
import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import io.cucumber.java.en.Given;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/**
 * Step definitions for circuit breaker and retry resilience scenarios.
 *
 * Since client services are mocked via @MockitoBean, these steps simulate
 * what happens when the circuit breaker is OPEN: the mocked client throws
 * ServiceUnavailableException, and we verify that PaymentService returns
 * structured SERVICE_UNAVAILABLE error codes.
 */
public class ResilienceStepDefinitions {

    @Autowired
    private WalletClientService walletClientService;

    @Autowired
    private UserClientService userClientService;

    @Given("the wallet service circuit breaker is open")
    public void walletServiceCircuitBreakerIsOpen() {
        doThrow(new ServiceUnavailableException(
                "Service walletService is currently unavailable (circuit open)",
                new RuntimeException("CB open")))
                .when(walletClientService).deductFromWallet(any(Long.class), any(BigDecimal.class));
    }

    @Given("the user service circuit breaker is open")
    public void userServiceCircuitBreakerIsOpen() {
        doThrow(new ServiceUnavailableException(
                "Service userService is currently unavailable (circuit open)",
                new RuntimeException("CB open")))
                .when(userClientService).validateUserConditions(any(Long.class));
    }
}
