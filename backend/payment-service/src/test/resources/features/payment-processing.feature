Feature: Payment Processing
  As a merchant
  I want to process a customer's payment by scanning their QR code
  So that I can receive payment for goods

  Background:
    Given the payment system is running
    And a customer with user ID 1 exists

  Scenario: Merchant successfully processes payment via QR scan
    Given the customer has a payment in "READY" status with QR code "QR_001"
    And the customer has wallet balance of 500.00
    When the merchant scans QR code "QR_001" with amount 100.00
    Then the payment status should be "COMPLETED"
    And 100.00 should be deducted from the customer's wallet
    And the merchant ID should be recorded on the payment

  Scenario: Payment rejected due to insufficient wallet balance
    Given the customer has a payment in "READY" status with QR code "QR_002"
    And the customer has insufficient wallet balance for 100.00
    When the merchant scans QR code "QR_002" with amount 100.00
    Then the payment should be rejected with error code "INSUFFICIENT_BALANCE"

  Scenario: Payment rejected because QR code is expired
    Given the customer has a payment in "READY" status
    And the QR code is expired
    When the merchant scans the expired QR code with amount 100.00
    Then the payment should be rejected with error code "QR_CODE_EXPIRED"

  Scenario: Payment rejected when QR code does not match payment
    Given the customer has a payment in "READY" status
    And the QR code belongs to a different payment
    When the merchant scans the mismatched QR code with amount 100.00
    Then the payment should be rejected with error code "QR_CODE_MISMATCH"

  Scenario Outline: Only READY payments can be processed
    Given the customer has a payment in "<status>" status
    When the merchant tries to process the payment with amount 100.00
    Then the payment should be rejected with error code "INVALID_PAYMENT_STATE"

    Examples:
      | status    |
      | PENDING   |
      | COMPLETED |
      | FAILED    |
      | CANCELLED |
      | REFUNDED  |
