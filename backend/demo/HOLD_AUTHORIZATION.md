# Hold and Authorization System

## Overview

The wallet system implements a **hold and authorization** pattern to ensure funds are reserved when a payment is initiated, preventing double-spending and ensuring funds are available when the payment is confirmed.

## Concepts

### Hold (Authorization)
A **hold** is a reservation of funds from a user's wallet. When a hold is created:
- The amount is **reserved** but **not deducted** from the wallet
- The user's available balance decreases by the hold amount
- The wallet balance remains unchanged
- The hold expires after 30 minutes if not captured

### Capture
**Capture** is the process of converting a hold into an actual deduction:
- The held amount is **deducted** from the wallet balance
- The hold status changes to CAPTURED
- This happens when the merchant confirms the payment

### Release
**Release** cancels a hold without deducting:
- The hold status changes to RELEASED
- The reserved amount becomes available again
- This happens when payment is cancelled, fails, or expires

## Balance Types

### Total Balance (`wallet_balance`)
- The actual amount of money in the user's wallet
- Stored in the `users` table
- Only changes when:
  - Money is added (top-up, refund)
  - Hold is captured (payment confirmed)

### Available Balance
- Calculated as: `wallet_balance - sum(active_holds)`
- The amount the user can actually spend
- Decreases when holds are created
- Increases when holds are released

### Held Balance
- Sum of all active holds for a user
- Amount that is reserved but not yet deducted

## Payment Flow with Holds

```
1. User Initiates Payment
   ↓
   - Validates user conditions
   - Checks available balance (not total balance)
   - Creates payment (PENDING)
   - Creates HOLD (ACTIVE) ← Amount reserved
   - Generates QR code
   ↓
   Available Balance: wallet_balance - hold_amount
   Total Balance: wallet_balance (unchanged)

2. Merchant Processes Payment
   ↓
   - Validates QR code
   - Updates payment to PROCESSING
   - Hold remains ACTIVE
   ↓
   Available Balance: still reduced
   Total Balance: still unchanged

3. Merchant Confirms Payment
   ↓
   - Captures HOLD ← Amount deducted
   - Updates payment to COMPLETED
   ↓
   Available Balance: increases (hold removed)
   Total Balance: decreases (amount deducted)

OR

3. Payment Cancelled/Failed
   ↓
   - Releases HOLD ← Amount freed
   - Updates payment to CANCELLED/FAILED
   ↓
   Available Balance: increases (hold removed)
   Total Balance: unchanged
```

## Hold States

### ACTIVE
- Hold is active and amount is reserved
- Available balance is reduced
- Can be captured or released

### CAPTURED
- Hold was successfully captured
- Amount was deducted from wallet
- Final state for successful payments

### RELEASED
- Hold was released (cancelled)
- Amount is available again
- Final state for cancelled/failed payments

### EXPIRED
- Hold expired without being captured
- Automatically released
- Amount is available again

## API Methods

### WalletService

#### `createHold(userId, paymentId, amount, reason)`
Creates a hold (authorization) on user's wallet.

**What happens**:
- Checks available balance (considering existing holds)
- Creates hold record with status ACTIVE
- Publishes `HoldCreatedEvent`
- Available balance decreases, total balance unchanged

#### `captureHold(holdId, reason)`
Captures a hold (deducts from wallet).

**What happens**:
- Validates hold is ACTIVE
- Deducts amount from wallet balance
- Updates hold status to CAPTURED
- Publishes `HoldCapturedEvent` and `WalletDeductedEvent`
- Available balance increases (hold removed), total balance decreases

#### `releaseHold(holdId, reason)`
Releases a hold (cancels authorization).

**What happens**:
- Validates hold is ACTIVE
- Updates hold status to RELEASED
- Publishes `HoldReleasedEvent`
- Available balance increases (hold removed), total balance unchanged

#### `getAvailableBalance(userId)`
Returns available balance (wallet balance minus active holds).

#### `hasSufficientBalance(userId, amount)`
Checks if user has sufficient available balance.

## Database Schema

### Holds Table
```sql
CREATE TABLE holds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- ACTIVE, CAPTURED, RELEASED, EXPIRED
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    captured_at TIMESTAMP,
    reason TEXT
);
```

## Events

### HoldCreatedEvent
Published when a hold is created.
- `holdId`, `userId`, `paymentId`, `amount`, `expiresAt`

### HoldCapturedEvent
Published when a hold is captured.
- `holdId`, `userId`, `paymentId`, `amount`, `oldBalance`, `newBalance`

### HoldReleasedEvent
Published when a hold is released.
- `holdId`, `userId`, `paymentId`, `amount`, `reason`

## Benefits

1. **Prevents Double-Spending**
   - Funds are reserved when payment is initiated
   - User can't spend the same money twice

2. **Ensures Funds Availability**
   - Money is guaranteed to be available when payment is confirmed
   - No race conditions between payment initiation and confirmation

3. **Better User Experience**
   - User sees available balance immediately
   - Clear distinction between reserved and available funds

4. **Flexible Cancellation**
   - Payments can be cancelled without actual deduction
   - Funds are immediately available after cancellation

5. **Audit Trail**
   - Complete history of holds, captures, and releases
   - Easy to track authorization lifecycle

## Example Scenarios

### Scenario 1: Successful Payment
```
User Balance: $100
User initiates payment: $50
  → Hold created: $50 (ACTIVE)
  → Available Balance: $50
  → Total Balance: $100

Merchant confirms payment
  → Hold captured: $50
  → Available Balance: $50
  → Total Balance: $50
```

### Scenario 2: Cancelled Payment
```
User Balance: $100
User initiates payment: $50
  → Hold created: $50 (ACTIVE)
  → Available Balance: $50
  → Total Balance: $100

User cancels payment
  → Hold released: $50
  → Available Balance: $100
  → Total Balance: $100
```

### Scenario 3: Multiple Payments
```
User Balance: $100
User initiates payment 1: $40
  → Hold 1: $40 (ACTIVE)
  → Available Balance: $60

User initiates payment 2: $30
  → Hold 2: $30 (ACTIVE)
  → Available Balance: $30

User initiates payment 3: $50
  → ❌ Fails: Insufficient available balance ($30 < $50)

Payment 1 confirmed
  → Hold 1 captured: $40
  → Available Balance: $60 (30 + 40 - 40)
  → Total Balance: $60
```

## API Endpoints

### Get Available Balance
```
GET /api/v1/users/{id}/wallet/available-balance
```
Returns:
```json
{
  "userId": 1,
  "totalBalance": 100.00,
  "availableBalance": 50.00
}
```

### Get Total Balance
```
GET /api/v1/users/{id}/wallet/balance
```
Returns:
```json
{
  "userId": 1,
  "balance": 100.00
}
```

## Implementation Details

### Pessimistic Locking
- Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` for concurrent access
- Prevents race conditions when creating/capturing holds
- Ensures data consistency

### Hold Expiration
- Default expiration: 30 minutes
- Can be configured per payment type
- Expired holds should be automatically released (requires scheduled job)

### Transaction Safety
- All hold operations are transactional
- If payment creation fails, hold is not created
- If capture fails, hold remains ACTIVE

## Future Enhancements

1. **Automatic Hold Expiration**
   - Scheduled job to release expired holds
   - Notification to user when hold expires

2. **Partial Capture**
   - Support for capturing less than the held amount
   - Useful for tips, adjustments, etc.

3. **Hold Extensions**
   - Allow extending hold expiration time
   - Useful for long-running transactions

4. **Hold History**
   - API to view all holds for a user
   - Filter by status, date range, etc.
