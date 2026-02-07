# Microservices Separation Guide

This document outlines the separation of the monolithic application into 4 independent microservices.

## Service Overview

### 1. User Service (Port 8081)
- **Database**: `user_db`
- **Entities**: User, Wallet
- **Responsibilities**:
  - User management (create, get)
  - Wallet management (balance, top-up)
  - Wallet balance queries

### 2. Wallet Service (Port 8082)
- **Database**: `wallet_db`
- **Entities**: Hold
- **Responsibilities**:
  - Hold management (create, capture, release)
  - Available balance calculation (requires communication with User Service)
  - Wallet operations that require holds

### 3. Payment Service (Port 8083)
- **Database**: `payment_db`
- **Entities**: Payment, Transaction, EventOutbox
- **Responsibilities**:
  - Payment lifecycle management
  - Transaction recording
  - Outbox pattern for event publishing
  - Communication with Wallet Service (for holds)
  - Communication with QR Service (for QR code generation)

### 4. QR Service (Port 8084)
- **Database**: `qr_db`
- **Entities**: QRCode
- **Responsibilities**:
  - QR code generation
  - QR code validation
  - QR code expiration management

## Inter-Service Communication

### Payment Service → QR Service
- **Method**: REST Client (RestClient)
- **Endpoint**: `POST /api/v1/qrcodes` - Create QR code
- **Configuration**: `qr.code.service.url=http://localhost:8084`

### Payment Service → Wallet Service
- **Method**: REST Client (RestClient)
- **Endpoints**:
  - `POST /api/v1/holds` - Create hold
  - `POST /api/v1/holds/{id}/capture` - Capture hold
  - `POST /api/v1/holds/{id}/release` - Release hold
- **Configuration**: `wallet.service.url=http://localhost:8082`

### Payment Service → User Service
- **Method**: REST Client (RestClient)
- **Endpoints**:
  - `GET /api/v1/users/{id}` - Get user
  - `GET /api/v1/users/{id}/wallet` - Get wallet
- **Configuration**: `user.service.url=http://localhost:8081`

## Migration Steps

### Files to Copy for Each Service

#### User Service
- Entities: `User.java`, `Wallet.java`
- Repositories: `UserRepository.java`, `WalletRepository.java`
- Services: `UserService.java`, `WalletService.java` (wallet operations only)
- Controllers: `UserController.java`
- DTOs: `CreateUserRequest.java`
- Exceptions: `PaymentException.java`, `GlobalExceptionHandler.java`
- Config: `CorsConfig.java`, `TracingConfig.java`, `PaymentIdFilter.java`
- Migrations: `V1__create_users_table.sql`, `V2__create_wallets_table.sql`

#### Wallet Service
- Entities: `Hold.java`, `HoldStatus.java`
- Repositories: `HoldRepository.java`
- Services: `WalletService.java` (hold operations only)
- Controllers: `WalletController.java` (new - for hold operations)
- DTOs: Hold-related DTOs
- Exceptions: `PaymentException.java`, `GlobalExceptionHandler.java`
- Config: `CorsConfig.java`, `TracingConfig.java`
- Migrations: `V1__create_holds_table.sql`

#### Payment Service
- Entities: `Payment.java`, `Transaction.java`, `EventOutbox.java`, `PaymentStatus.java`, `TransactionType.java`, `TransactionStatus.java`, `OutboxStatus.java`
- Repositories: `PaymentRepository.java`, `TransactionRepository.java`, `EventOutboxRepository.java`
- Services: `PaymentService.java`, `TransactionService.java`, `OutboxService.java`, `OutboxProcessor.java`
- Controllers: `PaymentController.java`, `TransactionController.java`
- DTOs: Payment and Transaction DTOs
- Exceptions: `PaymentException.java`, `PaymentNotFoundException.java`, `GlobalExceptionHandler.java`
- Config: `CorsConfig.java`, `TracingConfig.java`, `PaymentIdFilter.java`, `RestClientConfiguration.java`
- Clients: `QRCodeClientService.java`, `WalletClientService.java` (new), `UserClientService.java` (new)
- Migrations: `V1__create_payments_table.sql`, `V2__create_transactions_table.sql`, `V3__create_outbox_table.sql`

#### QR Service
- Entities: `QRCode.java`, `QRCodeStatus.java`
- Repositories: `QRCodeRepository.java`
- Services: `QRCodeService.java`
- Controllers: `QRCodeController.java`
- DTOs: QR Code DTOs
- Exceptions: `QRCodeNotFoundException.java`, `GlobalExceptionHandler.java`
- Config: `CorsConfig.java`, `TracingConfig.java`
- Migrations: `V1__create_qr_codes_table.sql`

## Package Name Updates

All package names should be updated from `com.example.demo.*` to:
- User Service: `com.example.userservice.*`
- Wallet Service: `com.example.walletservice.*`
- Payment Service: `com.example.paymentservice.*`
- QR Service: `com.example.qrservice.*`

## Docker Compose Updates

Update `docker-compose.yml` to include service definitions for each microservice with appropriate ports and environment variables.

## Next Steps

1. Copy entity classes to each service
2. Copy repository interfaces
3. Copy service classes (split WalletService appropriately)
4. Copy controller classes
5. Copy DTO classes
6. Copy exception classes
7. Copy configuration classes
8. Update all package names
9. Update all imports
10. Create REST client services for inter-service communication
11. Update application.properties for each service
12. Test each service independently
