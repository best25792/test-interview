# Hotel Booking Backend API Spec

## Overview

The **hotel-service** is a new microservice for the hotel booking domain. It integrates with the existing user-service (auth) and payment-service (payments). The Angular hotel booking frontend consumes this API.

---

## 1. Database

### 1.1 New Database

Add to `backend/db-init/01-create-databases.sql`:

```sql
CREATE DATABASE hotel_db;
GRANT ALL PRIVILEGES ON DATABASE hotel_db TO sa;
```

### 1.2 Tables

#### hotels

| Column      | Type         | Constraints                |
|-------------|--------------|----------------------------|
| id          | BIGSERIAL    | PRIMARY KEY                |
| name        | VARCHAR(255) | NOT NULL                   |
| location    | VARCHAR(255) | NOT NULL                   |
| description | TEXT         |                            |
| amenities   | TEXT[]       |                            |
| created_at  | TIMESTAMP    | DEFAULT NOW()              |
| updated_at  | TIMESTAMP    | DEFAULT NOW()              |

#### rooms

| Column        | Type          | Constraints                |
|---------------|---------------|----------------------------|
| id            | BIGSERIAL     | PRIMARY KEY                |
| hotel_id      | BIGINT        | NOT NULL, FK → hotels(id)  |
| name          | VARCHAR(255)  | NOT NULL                   |
| type          | VARCHAR(100)  | NOT NULL                   |
| price_per_night | DECIMAL(10,2) | NOT NULL                 |
| max_guests    | INT           | NOT NULL                   |
| created_at    | TIMESTAMP     | DEFAULT NOW()              |
| updated_at    | TIMESTAMP     | DEFAULT NOW()              |

#### bookings

| Column      | Type          | Constraints                  |
|-------------|---------------|------------------------------|
| id          | BIGSERIAL     | PRIMARY KEY                  |
| hotel_id    | BIGINT        | NOT NULL, FK → hotels(id)    |
| room_id     | BIGINT        | NOT NULL, FK → rooms(id)     |
| user_id     | BIGINT        | NOT NULL                     |
| check_in    | DATE          | NOT NULL                     |
| check_out   | DATE          | NOT NULL                     |
| status      | VARCHAR(50)   | NOT NULL (PENDING, COMPLETE, CANCELLED) |
| total_amount| DECIMAL(10,2) | NOT NULL                     |
| payment_id  | BIGINT        | Nullable (set when payment completes) |
| created_at  | TIMESTAMP     | DEFAULT NOW()                |
| updated_at  | TIMESTAMP     | DEFAULT NOW()                |

---

## 2. API Endpoints

Base path: `/api/v1`

### 2.1 Hotels

#### GET /hotels/search

Search hotels with optional filters.

**Query parameters:**

| Param    | Type   | Required | Description                    |
|----------|--------|----------|--------------------------------|
| name     | string | No       | Partial match on hotel name    |
| location | string | No       | Partial match on location      |
| minPrice | number | No       | Minimum price per night        |
| maxPrice | number | No       | Maximum price per night        |
| checkIn  | string | No       | ISO date (YYYY-MM-DD)          |
| checkOut | string | No       | ISO date (YYYY-MM-DD)          |

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "name": "Grand Hotel",
    "location": "Bangkok",
    "description": "Luxury hotel in city center",
    "amenities": ["WiFi", "Pool", "Spa"],
    "minPrice": 150.00
  }
]
```

#### GET /hotels/{id}

Get hotel by ID with rooms.

**Response:** `200 OK`

```json
{
  "id": 1,
  "name": "Grand Hotel",
  "location": "Bangkok",
  "description": "Luxury hotel in city center",
  "amenities": ["WiFi", "Pool", "Spa"],
  "rooms": [
    {
      "id": 1,
      "hotelId": 1,
      "name": "Deluxe Room",
      "type": "DELUXE",
      "pricePerNight": 199.99,
      "maxGuests": 2
    }
  ]
}
```

#### GET /hotels/{id}/rooms

Get rooms for a hotel, optionally filter by availability.

**Query parameters:**

| Param   | Type   | Required | Description           |
|---------|--------|----------|-----------------------|
| checkIn | string | No       | ISO date (YYYY-MM-DD) |
| checkOut| string | No       | ISO date (YYYY-MM-DD) |

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "hotelId": 1,
    "name": "Deluxe Room",
    "type": "DELUXE",
    "pricePerNight": 199.99,
    "maxGuests": 2,
    "available": true
  }
]
```

### 2.2 Bookings

#### POST /bookings

Create a new booking.

**Request body:**

```json
{
  "hotelId": 1,
  "roomId": 1,
  "userId": 1,
  "checkIn": "2025-04-01",
  "checkOut": "2025-04-03"
}
```

**Response:** `201 Created`

```json
{
  "id": 1,
  "hotelId": 1,
  "roomId": 1,
  "userId": 1,
  "checkIn": "2025-04-01",
  "checkOut": "2025-04-03",
  "status": "PENDING",
  "totalAmount": 399.98,
  "paymentId": null,
  "hotelName": "Grand Hotel",
  "roomName": "Deluxe Room"
}
```

**Validation:**
- checkOut must be after checkIn
- Room must exist and belong to hotel
- Room must be available for the date range
- totalAmount = pricePerNight × nights

#### GET /bookings

List bookings. Requires JWT.

**Query parameters:**

| Param  | Type   | Required | Description         |
|--------|--------|----------|---------------------|
| userId | number | No       | Filter by user      |
| status | string | No       | PENDING, COMPLETE, CANCELLED |

**Response:** `200 OK`

```json
[
  {
    "id": 1,
    "hotelId": 1,
    "roomId": 1,
    "userId": 1,
    "checkIn": "2025-04-01",
    "checkOut": "2025-04-03",
    "status": "PENDING",
    "totalAmount": 399.98,
    "paymentId": null,
    "hotelName": "Grand Hotel",
    "roomName": "Deluxe Room"
  }
]
```

#### GET /bookings/{id}

Get booking by ID. Requires JWT.

**Response:** `200 OK` (same schema as single booking in POST response)

#### PATCH /bookings/{id}/complete

Mark booking as COMPLETE (called by frontend after successful payment). Requires JWT.

**Response:** `200 OK`

```json
{
  "id": 1,
  "status": "COMPLETE",
  ...
}
```

**Validation:** Booking must be in PENDING status.

#### PATCH /bookings/{id}/cancel

Cancel booking. No refund. Requires JWT.

**Response:** `200 OK`

```json
{
  "id": 1,
  "status": "CANCELLED",
  ...
}
```

**Validation:** Booking must be in PENDING status.

---

## 3. Payment Integration Flow

1. **Create booking** – `POST /api/v1/bookings` returns booking with `id`, `totalAmount`, `status: PENDING`
2. **Initiate payment** – User calls `POST /api/v1/payments/initiate` with `{ userId }` on payment-service
3. **Receive QR** – Frontend polls `GET /api/v1/payments/{id}/status` until QR ready, displays QR
4. **Process payment** – Frontend calls `POST /api/v1/payments/{id}/process` with `amount: booking.totalAmount`, `qrCode`, `currency: "USD"`, `merchantId: "HOTEL_MERCHANT"`
5. **On success** – Frontend calls `PATCH /api/v1/bookings/{id}/complete`
6. **On failure** – Frontend calls `PATCH /api/v1/bookings/{id}/cancel`

---

## 4. Tech Stack

- Java 21
- Spring Boot 4.x
- PostgreSQL (hotel_db)
- JWT validation (reuse user-service issuer/algorithm)
- Port: **8086**
- Flyway for migrations (optional)

---

## 5. Error Handling

| Code | Condition |
|------|-----------|
| 400 | Invalid dates, room not available, validation errors |
| 401 | Unauthorized (missing/invalid JWT) |
| 404 | Hotel, room, or booking not found |
| 409 | Booking conflict (room already booked) |

Error response format:

```json
{
  "message": "Room is not available for the selected dates",
  "code": "ROOM_NOT_AVAILABLE"
}
```

---

## 6. Security

- Booking endpoints (list, get, complete, cancel) require `Authorization: Bearer <jwt>`
- JWT validation via user-service issuer
- userId in create booking can be derived from JWT or passed (validate user owns the request)
