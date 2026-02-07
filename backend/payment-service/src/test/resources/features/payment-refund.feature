Feature: Payment Refund
  As a customer service agent
  I want to refund a completed payment
  So that the customer gets their money back

  Background:
    Given the payment system is running

  Scenario: Full refund of completed payment
    Given a completed payment of 100.00 for customer with user ID 1
    When a refund of 100.00 is requested
    Then the payment status should be "REFUNDED"
    And 100.00 should be credited to the customer's wallet

  Scenario: Partial refund of completed payment
    Given a completed payment of 100.00 for customer with user ID 1
    When a refund of 30.00 is requested
    Then the payment status should be "REFUNDED"
    And 30.00 should be credited to the customer's wallet

  Scenario: Refund amount exceeds payment amount
    Given a completed payment of 100.00 for customer with user ID 1
    When a refund of 150.00 is requested
    Then the refund should be rejected with error code "REFUND_EXCEEDS_AMOUNT"
    And no money should be credited to the customer's wallet

  Scenario: Cannot refund a pending payment
    Given a customer with user ID 1 exists
    And the customer has a payment in "PENDING" status
    When a refund of 50.00 is requested
    Then the refund should be rejected with error code "INVALID_PAYMENT_STATE"

  Scenario: Cannot refund a cancelled payment
    Given a customer with user ID 1 exists
    And the customer has a payment in "CANCELLED" status
    When a refund of 50.00 is requested
    Then the refund should be rejected with error code "INVALID_PAYMENT_STATE"
