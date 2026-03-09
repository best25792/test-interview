-- Role-based access: PAYMENT_USER (payment tab), MERCHANT (store tab), ADMIN (transactions tab)
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) NOT NULL DEFAULT 'PAYMENT_USER';
