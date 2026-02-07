Feature: Payment Idempotency
  As a payment system
  I want to prevent duplicate charges when clients retry
  So that customers are never charged twice

  Background:
    Given the payment system is running

  Scenario: Duplicate initiation with same idempotency key returns same payment
    Given a customer with user ID 1 exists
    And the customer meets business conditions
    When the customer initiates a payment with idempotency key "KEY_001"
    And the customer initiates a payment again with idempotency key "KEY_001"
    Then both responses should return the same payment ID

  Scenario: Different idempotency keys create separate payments
    Given a customer with user ID 1 exists
    And the customer meets business conditions
    When the customer initiates a payment with idempotency key "KEY_A"
    And the customer initiates a payment with idempotency key "KEY_B"
    Then two separate payments should exist
