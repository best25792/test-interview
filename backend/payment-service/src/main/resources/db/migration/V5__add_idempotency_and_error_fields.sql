ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE payments ADD COLUMN error_code VARCHAR(50);
ALTER TABLE payments ADD COLUMN error_message TEXT;

CREATE UNIQUE INDEX idx_payments_idempotency_key ON payments(idempotency_key);
