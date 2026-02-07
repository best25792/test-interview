# E-Wallet QR Payment Flow

## Overview

This system implements an e-wallet provider that allows users to pay merchants using QR codes. The flow follows a specific sequence of events with event-based microservices architecture.

## Payment Flow

### Step 1: User Initiates Payment
**Endpoint**: `POST /api/v1/payments/initiate`

**Request**:
```json
{
  "userId": 1,
  "amount": 100.00,
  "currency": "USD",
  "merchantId": "MERCHANT_001",
  "description": "Payment for goods"
}
```

**What happens**:
1. User clicks "Pay with QR" in the app
2. Frontend calls `/api/v1/payments/initiate` with user ID and payment details

### Step 2: Business Condition Validation
**Service**: `PaymentService.initiatePayment()`

**Validations**:
- ✅ User exists and is active
- ✅ User has sufficient wallet balance
- ✅ User meets business conditions (active status, verified status, etc.)

**If validation fails**: Returns error
**If validation succeeds**: Creates payment record and generates QR code

**Response**:
```json
{
  "transactionId": 123,
  "qrCode": "PAYMENT_123_ABC123DEF456",
  "message": "Payment initiated successfully. QR code generated.",
  "success": true
}
```

### Step 3: User Shows QR to Merchant
- Frontend displays the QR code to the user
- User shows QR code to merchant's scanner

### Step 4: Merchant Processes Payment
**Endpoint**: `POST /api/v1/payments/{id}/process`

**Request**:
```json
{
  "qrCode": "PAYMENT_123_ABC123DEF456"
}
```

**What happens**:
1. Merchant scans QR code
2. Merchant calls `/api/v1/payments/{id}/process` with QR code
3. System validates QR code matches the payment
4. QR code is marked as USED
5. Payment status changes to PROCESSING (waiting for merchant confirmation)
6. `PaymentProcessedEvent` is published

**Response**: Payment with status PROCESSING

### Step 5: Merchant Confirms Payment
**Endpoint**: `POST /api/v1/payments/{id}/confirm`

**What happens**:
1. Merchant confirms the payment (callback/webhook)
2. System validates payment is in PROCESSING status
3. **Wallet deduction happens here**:
   - User's wallet balance is deducted
   - `WalletDeductedEvent` is published
4. Payment status changes to COMPLETED
5. `PaymentConfirmedEvent` is published
6. Transaction record is created (via event listener)

**Response**: Payment with status COMPLETED

### Step 6: Wallet Deduction
**Service**: `WalletService.deductFromWallet()`

**What happens**:
- User's wallet balance is reduced by payment amount
- Old balance and new balance are tracked
- `WalletDeductedEvent` is published
- Event listeners can handle notifications, audit logs, etc.

## Event Flow Diagram

```
User Initiates Payment
    ↓
PaymentService.initiatePayment()
    ├─→ Validates user conditions
    ├─→ Checks wallet balance
    ├─→ Creates payment (PENDING)
    └─→ Publishes PaymentCreatedEvent
            ↓
        QRCodeService.handlePaymentCreated()
            ├─→ Generates QR code
            └─→ Publishes QRCodeGeneratedEvent
                    ↓
                Returns transactionId + QR code to frontend

User Shows QR to Merchant
    ↓
Merchant Scans QR
    ↓
PaymentService.processPayment()
    ├─→ Validates QR code
    ├─→ Marks QR as USED
    ├─→ Updates payment to PROCESSING
    └─→ Publishes PaymentProcessedEvent

Merchant Confirms Payment
    ↓
PaymentService.confirmPayment()
    ├─→ Validates payment status
    ├─→ WalletService.deductFromWallet()
    │   ├─→ Deducts from user wallet
    │   └─→ Publishes WalletDeductedEvent
    ├─→ Updates payment to COMPLETED
    └─→ Publishes PaymentConfirmedEvent
            ↓
        TransactionEventListener.handlePaymentConfirmed()
            └─→ Creates transaction record
```

## Database Schema

### Users Table
- `id` - User ID
- `username` - Unique username
- `email` - User email
- `phone_number` - User phone
- `wallet_balance` - Current wallet balance
- `is_active` - User active status
- `is_verified` - User verification status

### Payments Table
- `id` - Payment ID (also used as transaction_id)
- `amount` - Payment amount
- `currency` - Currency code
- `status` - Payment status (PENDING, PROCESSING, COMPLETED, etc.)
- `merchant_id` - Merchant identifier
- `customer_id` - User ID (as string)
- `description` - Payment description

### QR Codes Table
- `id` - QR code ID
- `code` - QR code string
- `payment_id` - Associated payment
- `status` - QR code status (ACTIVE, USED, EXPIRED)
- `expires_at` - Expiration timestamp

### Transactions Table
- `id` - Transaction ID
- `payment_id` - Associated payment
- `type` - Transaction type (PAYMENT, REFUND)
- `amount` - Transaction amount
- `status` - Transaction status
- `reference` - Transaction reference

## API Endpoints

### Payment Endpoints
- `POST /api/v1/payments/initiate` - User initiates payment (Step 1-2)
- `POST /api/v1/payments/{id}/process` - Merchant processes payment (Step 4)
- `POST /api/v1/payments/{id}/confirm` - Merchant confirms payment (Step 5-6)
- `GET /api/v1/payments/{id}` - Get payment details
- `GET /api/v1/payments` - List payments
- `POST /api/v1/payments/{id}/refund` - Refund payment

### User/Wallet Endpoints
- `GET /api/v1/users/{id}/wallet/balance` - Get wallet balance
- `POST /api/v1/users/{id}/wallet/topup` - Top up wallet
- `GET /api/v1/users/{id}` - Get user details

## Business Rules

1. **User Validation**:
   - User must be active
   - User must have sufficient balance
   - Additional business conditions can be added

2. **QR Code**:
   - QR code expires after 15 minutes
   - QR code can only be used once
   - QR code must match the payment

3. **Payment States**:
   - PENDING → Initial state after creation
   - PROCESSING → After merchant processes QR
   - COMPLETED → After merchant confirms and wallet deduction
   - REFUNDED → After refund is processed
   - FAILED → If payment fails
   - CANCELLED → If payment is cancelled

4. **Wallet Operations**:
   - Deduction happens only on payment confirmation
   - Refunds credit back to wallet
   - Balance is locked during deduction (pessimistic locking)

## Error Handling

- **Insufficient Balance**: Returns error before creating payment
- **Invalid QR Code**: Returns error during processing
- **User Not Active**: Returns error during initiation
- **Payment Already Processed**: Returns error if trying to process again

## Event-Based Communication

All services communicate via events:
- `PaymentCreatedEvent` → Triggers QR code generation
- `PaymentProcessedEvent` → Notifies other services
- `PaymentConfirmedEvent` → Triggers transaction creation
- `WalletDeductedEvent` → Triggers notifications/audit logs

## Testing the Flow

1. **Create a user with wallet balance**:
   ```bash
   POST /api/v1/users
   # Then top up: POST /api/v1/users/{id}/wallet/topup
   ```

2. **User initiates payment**:
   ```bash
   POST /api/v1/payments/initiate
   {
     "userId": 1,
     "amount": 100.00,
     "currency": "USD",
     "merchantId": "MERCHANT_001"
   }
   ```

3. **Merchant processes payment**:
   ```bash
   POST /api/v1/payments/{transactionId}/process
   {
     "qrCode": "PAYMENT_123_ABC123DEF456"
   }
   ```

4. **Merchant confirms payment**:
   ```bash
   POST /api/v1/payments/{transactionId}/confirm
   ```

5. **Verify wallet deduction**:
   ```bash
   GET /api/v1/users/{userId}/wallet/balance
   ```
