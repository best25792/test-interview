# Tracing and Logging with Payment ID Correlation

This document explains how tracing and logging are configured to group all processes related to the same `payment_id`.

## Overview

All payment-related operations are automatically correlated by `payment_id` in both:
- **Distributed Traces** (Tempo) - All spans for a payment share the same `payment_id` attribute
- **Logs** (Loki) - All logs include `payment_id` in the MDC (Mapped Diagnostic Context)

## How It Works

### 1. Automatic Payment ID Extraction

The `PaymentIdFilter` automatically extracts `payment_id` from:
- URL paths: `/api/v1/payments/{id}`, `/api/v1/payments/{id}/process`, etc.
- Request processing in service methods

### 2. Trace Context Propagation

When a `payment_id` is detected:
- It's added as a span attribute: `payment.id` and `payment_id`
- It's added to MDC (Mapped Diagnostic Context) for log correlation
- All child spans inherit the `payment_id` attribute

### 3. Log Correlation

All log statements automatically include `payment_id` when available:
```
2026-01-20 13:54:32.123 [http-nio-8080-exec-1] INFO [123] com.example.demo.service.PaymentService - Payment initiated. Payment id: 123
```

The `[123]` in brackets is the `payment_id` from MDC.

## Viewing in Grafana

### Query Traces by Payment ID

In Grafana Tempo Explore:

1. **Search by payment_id attribute:**
   ```
   { payment_id = "123" }
   ```

2. **Search by payment.id attribute:**
   ```
   { payment.id = 123 }
   ```

### Query Logs by Payment ID

In Grafana Loki Explore:

1. **Search logs with payment_id:**
   ```logql
   {job="demo"} |= "payment_id" | json | payment_id="123"
   ```

2. **Or using MDC field:**
   ```logql
   {job="demo"} | json | payment_id="123"
   ```

### Correlate Traces and Logs

1. **From Trace to Logs:**
   - Open a trace in Tempo
   - Click on a span
   - Use "Logs" button to see related logs
   - Grafana will automatically filter logs by `payment_id`

2. **From Logs to Traces:**
   - Open a log entry in Loki
   - Click "Tempo" button to see related traces
   - Grafana will automatically filter traces by `payment_id`

## Payment Flow Example

When processing a payment with `payment_id=123`, you'll see:

### Traces (Tempo)
- `POST /api/v1/payments/123/process` (span with `payment_id=123`)
  - `PaymentService.processPayment` (child span with `payment_id=123`)
    - `WalletService.createHold` (child span with `payment_id=123`)
    - `QRCodeClientService.validateQRCode` (child span with `payment_id=123`)

### Logs (Loki)
```
[123] PaymentService - Payment processed by merchant. Payment id: 123
[123] WalletService - Hold created. HoldId: 456, PaymentId: 123
[123] QRCodeClientService - QR code validated for payment: 123
```

All these logs and traces are automatically grouped by `payment_id=123`.

## Configuration Files

- **TracingConfig**: Adds `payment_id` to trace spans and MDC
- **PaymentIdFilter**: Extracts `payment_id` from HTTP requests
- **TracingAspect**: Automatically creates spans for all service method calls
- **logback-spring.xml**: Includes `payment_id` in log output pattern

## Automatic Service Method Tracing

The `TracingAspect` automatically creates OpenTelemetry spans for all public methods in service classes:

- **Automatic span creation**: Every service method call gets its own span
- **Span attributes**: Method name, class name, parameters, and results
- **Error tracking**: Exceptions are automatically recorded in spans
- **Payment ID detection**: Automatically detects `payment_id` in method parameters and adds it to span attributes

### Example Trace Structure

When calling `PaymentService.processPayment()`:

```
POST /api/v1/payments/123/process (HTTP span)
  └── PaymentService.processPayment (service span)
      ├── WalletService.createHold (service span)
      ├── QRCodeClientService.validateQRCode (HTTP client span)
      │   └── POST /api/v1/qrcodes/validate (HTTP span)
      └── OutboxService.saveEvent (service span)
```

All spans automatically include:
- `code.function`: Method name
- `code.namespace`: Class name
- `payment.id` / `payment_id`: If detected in parameters
- `method.success`: true/false
- Error details if exception occurs

## Testing

1. **Initiate a payment:**
   ```bash
   curl -X POST http://localhost:8080/api/v1/payments/initiate \
     -H "Content-Type: application/json" \
     -d '{"userId": "1"}'
   ```

2. **Process the payment** (use the payment_id from step 1):
   ```bash
   curl -X POST http://localhost:8080/api/v1/payments/123/process \
     -H "Content-Type: application/json" \
     -d '{"qrCode": "...", "amount": 100, "currency": "USD", "merchantId": "merchant1"}'
   ```

3. **View in Grafana:**
   - Open http://localhost:3001
   - Go to Explore → Tempo
   - Search: `{ payment_id = "123" }`
   - Go to Explore → Loki
   - Search: `{job="demo"} | json | payment_id="123"`
