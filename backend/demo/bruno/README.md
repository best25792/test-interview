# Bruno API Collection - Payment System

This is a Bruno API collection for testing the Payment System API endpoints.

## Setup

1. Install [Bruno](https://www.usebruno.com/)
2. Open Bruno and click "Open Collection"
3. Select the `bruno` folder from this project

## Configuration

### Environment Variables

All requests use the `base_url` variable which defaults to `http://localhost:8080`.

You can override this by:
- Creating a `.env` file in the bruno folder
- Or modifying the `vars:pre-request` section in each `.bru` file

Example `.env`:
```
base_url=http://localhost:8080
```

## API Endpoints

### Payments
1. **Initiate Payment** - User initiates payment with only userId
2. **Get Payment by ID** - Get payment details
3. **Get All Payments** - List all payments (with optional status filter)
4. **Process Payment** - Merchant processes payment with QR code and payment details
5. **Confirm Payment** - Merchant confirms payment (triggers wallet deduction)
6. **Cancel Payment** - Cancel payment (releases hold)
7. **Refund Payment** - Refund a completed payment

### QR Codes
1. **Get QR Code by ID** - Get QR code details
2. **Validate QR Code** - Validate QR code

### Transactions
1. **Get Transaction by ID** - Get transaction details
2. **Get All Transactions** - List transactions (with optional filters)

### Users & Wallet
1. **Get User by ID** - Get user details
2. **Get Wallet Balance** - Get total wallet balance
3. **Get Available Balance** - Get available balance (considering holds)
4. **Get Wallet Details** - Get full wallet information
5. **Top Up Wallet** - Add money to wallet

## Complete Payment Flow

The typical payment flow:

1. **Initiate Payment**
   - User clicks "Pay with QR"
   - POST `/api/v1/payments/initiate` with `{ "userId": 1 }`
   - Returns `transactionId` and `qrCode`

2. **User Shows QR to Merchant**
   - No API call needed

3. **Merchant Processes Payment**
   - Merchant scans QR code
   - POST `/api/v1/payments/{id}/process` with:
     ```json
     {
       "qrCode": "...",
       "amount": 100.00,
       "currency": "USD",
       "merchantId": "MERCHANT_001",
       "description": "Payment for goods"
     }
     ```
   - Creates hold on user's wallet
   - Status: PROCESSING

4. **Merchant Confirms Payment**
   - POST `/api/v1/payments/{id}/confirm`
   - Captures hold and deducts from wallet
   - Status: COMPLETED

## Testing Tips

1. **Variables**: Update variables like `paymentId`, `userId`, `qrCode` in the `vars:pre-request` section of each request
2. **Chaining**: After creating a payment, copy the `transactionId` from response and use it in subsequent requests
3. **Status Filters**: Use query parameters like `?status=PENDING` to filter payments
4. **Tests**: Each request includes basic tests that verify the response structure

## Notes

- Base URL: `http://localhost:8080` (update if your server runs on different port)
- All endpoints use JSON format
- No authentication required in this version
- Payment amounts are in BigDecimal format
- Currency codes should be 3 characters (e.g., "USD", "EUR")
