package com.example.paymentservice.bdd.steps;

import com.example.paymentservice.client.UserClientService;
import com.example.paymentservice.client.WalletClientService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
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
 * CallNotPermittedException, and we verify that PaymentService returns
 * structured SERVICE_UNAVAILABLE error codes.
 */
public class ResilienceStepDefinitions {

    @Autowired
    private WalletClientService walletClientService;

    @Autowired
    private UserClientService userClientService;

    @Given("the wallet service circuit breaker is open")
    public void walletServiceCircuitBreakerIsOpen() {
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.ofDefaults("walletService")))
                .when(walletClientService).deductFromWallet(any(Long.class), any(BigDecimal.class));
    }

    @Given("the user service circuit breaker is open")
    public void userServiceCircuitBreakerIsOpen() {
        doThrow(CallNotPermittedException.createCallNotPermittedException(
                CircuitBreaker.ofDefaults("userService")))
                .when(userClientService).validateUserConditions(any(Long.class));
    }
}
