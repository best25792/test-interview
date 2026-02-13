CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    is_used BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_otp_challenges_user_id ON otp_challenges(user_id);
CREATE INDEX idx_otp_challenges_expires_at ON otp_challenges(expires_at);
CREATE INDEX idx_otp_challenges_channel ON otp_challenges(channel);
