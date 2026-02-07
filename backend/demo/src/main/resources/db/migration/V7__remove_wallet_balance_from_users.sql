-- Remove wallet_balance column from users table
-- Wallet information is now in separate wallets table
ALTER TABLE users DROP COLUMN IF EXISTS wallet_balance;
