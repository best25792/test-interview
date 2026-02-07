CREATE TABLE IF NOT EXISTS qr_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(500) NOT NULL UNIQUE,
    payment_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
    -- Note: Foreign key to payments removed as it references a different database
);

CREATE INDEX idx_qr_codes_payment_id ON qr_codes(payment_id);
CREATE INDEX idx_qr_codes_code ON qr_codes(code);
CREATE INDEX idx_qr_codes_status ON qr_codes(status);
