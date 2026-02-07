Feature: Payment Initiation
  As a customer
  I want to initiate a payment
  So that I can pay for goods via QR code

  Background:
    Given the payment system is running

  Scenario: Customer successfully initiates a payment
    Given a customer with user ID 1 exists
    And the customer meets business conditions
    When the customer initiates a payment
    Then the payment should be created with status "PENDING"

  Scenario: Customer does not meet business conditions
    Given a customer with user ID 1 exists
    And the customer does NOT meet business conditions
    When the customer initiates a payment
    Then the payment should be rejected with error code "USER_VALIDATION_FAILED"
    And no payment record should be created
