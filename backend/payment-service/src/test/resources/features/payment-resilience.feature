Feature: Payment Service Resilience
  As the payment system
  I want to gracefully handle downstream service failures
  So that persistent failures return structured SERVICE_UNAVAILABLE errors

  Background:
    Given the payment system is running
    And a customer with user ID 1 exists

  Scenario: Payment fails when wallet service is unavailable (circuit open)
    Given the customer meets business conditions
    And the customer has a payment in "READY" status with QR code "VALID_QR_002"
    And the wallet service circuit breaker is open
    When the merchant scans QR code "VALID_QR_002" with amount 50.00
    Then the payment should be rejected with error code "SERVICE_UNAVAILABLE"

  Scenario: Payment initiation fails when user service is unavailable (circuit open)
    Given the user service circuit breaker is open
    When the customer initiates a payment
    Then the payment should be rejected with error code "SERVICE_UNAVAILABLE"
    And no payment record should be created
