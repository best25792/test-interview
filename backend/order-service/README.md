# Order Service (Merchant Orders & Inventory)

Spring Boot service for merchant store: products (inventory) and orders.

- **Port:** 8085
- **Database:** PostgreSQL `merchant_db` (create via `backend/demo/src/main/resources/db/init/01-create-databases.sql`)

## API

- **Products:** `GET/POST /api/v1/products`, `GET/PATCH /api/v1/products/{id}`, `PATCH /api/v1/products/{id}/stock`
- **Orders:** `GET/POST /api/v1/orders`, `GET/PATCH /api/v1/orders/{id}`, `PATCH /api/v1/orders/{id}/status`

## Run

1. Ensure PostgreSQL is running with `merchant_db` created.
2. `./mvnw spring-boot:run` (or `mvnw.cmd` on Windows)

Frontend (merchant store) uses `NEXT_PUBLIC_ORDER_SERVICE_URL=http://localhost:8085/api/v1` when set; otherwise it falls back to local cart/orders.
