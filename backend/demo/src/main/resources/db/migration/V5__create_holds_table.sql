CREATE TABLE IF NOT EXISTS holds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    payment_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    released_at TIMESTAMP,
    captured_at TIMESTAMP,
    reason TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (payment_id) REFERENCES payments(id)
);

CREATE INDEX idx_holds_user_id ON holds(user_id);
CREATE INDEX idx_holds_payment_id ON holds(payment_id);
CREATE INDEX idx_holds_status ON holds(status);
CREATE INDEX idx_holds_expires_at ON holds(expires_at);
