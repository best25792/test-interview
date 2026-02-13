CREATE TABLE IF NOT EXISTS device_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_id VARCHAR(255) NOT NULL,
    refresh_token_hash VARCHAR(255) NOT NULL,
    refresh_token_expires_at TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX idx_device_sessions_user_id ON device_sessions(user_id);
CREATE INDEX idx_device_sessions_refresh_token_hash ON device_sessions(refresh_token_hash);
CREATE INDEX idx_device_sessions_is_revoked ON device_sessions(is_revoked);
