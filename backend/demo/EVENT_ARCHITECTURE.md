# Event-Based Microservices Architecture

## Overview

This project uses an **event-based communication pattern** to simulate a microservices architecture while keeping everything in the same codebase for easy understanding. Services communicate through events instead of direct method calls.

## Architecture Pattern

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│ Payment Service │         │  QRCode Service │         │Transaction      │
│                 │         │                 │         │Service          │
│  - create()     │────────▶│  - generate()   │         │                 │
│  - process()    │  Event  │  - validate()   │         │  - create()     │
│  - confirm()    │         │  - markUsed()   │         │                 │
│  - refund()     │         │                 │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
       │                            │                            │
       │                            │                            │
       └────────────────────────────┴────────────────────────────┘
                          │
                    EventPublisher
                    (Spring Events)
```

## Event Flow

### 1. Payment Creation Flow

```
PaymentService.createPayment()
    ↓
Publishes: PaymentCreatedEvent
    ↓
QRCodeService.handlePaymentCreated() (Event Listener)
    ↓
Generates QR Code
    ↓
Publishes: QRCodeGeneratedEvent
```

### 2. Payment Processing Flow

```
PaymentService.processPayment()
    ↓
Validates QR Code (synchronous for API response)
    ↓
Updates Payment Status
    ↓
Publishes: PaymentProcessedEvent
    ↓
TransactionEventListener.handlePaymentProcessed()
    ↓
Creates Transaction Record
```

### 3. Payment Confirmation Flow

```
PaymentService.confirmPayment()
    ↓
Updates Payment Status
    ↓
Publishes: PaymentConfirmedEvent
    ↓
TransactionEventListener.handlePaymentConfirmed()
    ↓
Creates Transaction Record
```

### 4. Payment Refund Flow

```
PaymentService.refundPayment()
    ↓
Updates Payment Status
    ↓
Publishes: PaymentRefundedEvent
    ↓
TransactionEventListener.handlePaymentRefunded()
    ↓
Creates Refund Transaction Record
```

## Event Types

### Payment Events

- **PaymentCreatedEvent**: Published when a new payment is created
  - Triggers QR code generation
  - Contains: paymentId, amount, currency, merchantId, customerId

- **PaymentProcessedEvent**: Published when payment is processed via QR
  - Triggers transaction creation
  - Contains: paymentId, qrCode, amount, currency

- **PaymentConfirmedEvent**: Published when payment is confirmed
  - Triggers transaction creation
  - Contains: paymentId, amount, currency

- **PaymentRefundedEvent**: Published when payment is refunded
  - Triggers refund transaction creation
  - Contains: paymentId, refundAmount, reason

### QR Code Events

- **QRCodeGeneratedEvent**: Published when QR code is generated
  - Contains: qrCodeId, qrCode, paymentId, expiresAt

- **QRCodeValidatedEvent**: Published when QR code is validated
  - Contains: qrCodeId, qrCode, paymentId, isValid, reason

- **QRCodeUsedEvent**: Published when QR code is marked as used
  - Contains: qrCodeId, qrCode, paymentId

## Service Boundaries

### Payment Service
- **Responsibility**: Payment lifecycle management
- **Publishes**: PaymentCreatedEvent, PaymentProcessedEvent, PaymentConfirmedEvent, PaymentRefundedEvent
- **Listens**: None (acts as source of truth for payments)

### QR Code Service
- **Responsibility**: QR code generation and validation
- **Publishes**: QRCodeGeneratedEvent, QRCodeValidatedEvent, QRCodeUsedEvent
- **Listens**: PaymentCreatedEvent

### Transaction Service
- **Responsibility**: Transaction record creation
- **Publishes**: None
- **Listens**: PaymentProcessedEvent, PaymentConfirmedEvent, PaymentRefundedEvent

## Implementation Details

### Event Publisher
- Central service (`EventPublisher`) that wraps Spring's `ApplicationEventPublisher`
- In production, this would publish to a message broker (Kafka, RabbitMQ, etc.)

### Event Listeners
- Use `@EventListener` annotation
- Process events asynchronously with `@Async`
- Each service has its own event listener class

### Async Configuration
- `AsyncConfig` configures thread pool for event processing
- Events are processed in separate threads to simulate distributed processing

## Benefits of This Architecture

1. **Loose Coupling**: Services don't directly depend on each other
2. **Scalability**: Each service can be scaled independently
3. **Resilience**: If one service fails, others continue working
4. **Event Sourcing Ready**: Easy to add event sourcing for audit trails
5. **Easy Migration**: Can easily split into separate microservices later

## Real-World Migration Path

To convert this to real microservices:

1. **Replace Spring Events with Message Broker**:
   - Replace `EventPublisher` with Kafka/RabbitMQ producer
   - Replace `@EventListener` with message consumers

2. **Split Services**:
   - Move each service to its own Spring Boot application
   - Each service has its own database
   - Use API Gateway for external communication

3. **Add Service Discovery**:
   - Use Eureka, Consul, or Kubernetes service discovery

4. **Add Distributed Tracing**:
   - Use OpenTelemetry or Zipkin for tracing across services

## Current Limitations (For Simplicity)

- Events are processed synchronously in some cases (for API responses)
- All services share the same database (in real microservices, each has its own)
- No event replay mechanism
- No event ordering guarantees (would need Kafka partitions)

## Testing Event-Based Architecture

When testing:
- Mock `EventPublisher` to verify events are published
- Use `@SpringBootTest` to test event listeners
- Verify event flow in integration tests
