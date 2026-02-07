-- Create databases for each microservice (order-service has its own init for merchant_db)
CREATE DATABASE payment_db;
CREATE DATABASE qr_db;
CREATE DATABASE wallet_db;  -- Contains wallets and holds
CREATE DATABASE user_db;    -- Contains only users

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE payment_db TO sa;
GRANT ALL PRIVILEGES ON DATABASE qr_db TO sa;
GRANT ALL PRIVILEGES ON DATABASE wallet_db TO sa;
GRANT ALL PRIVILEGES ON DATABASE user_db TO sa;
