-- Create database for order-service (merchant orders and inventory)
-- Source of truth: backend/order-service/src/main/resources/db/init/01-create-merchant-db.sql
CREATE DATABASE merchant_db;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE merchant_db TO sa;
