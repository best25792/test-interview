ALTER TABLE users ADD COLUMN IF NOT EXISTS is_2fa_enabled BOOLEAN NOT NULL DEFAULT true;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_review_account BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_users_is_2fa_enabled ON users(is_2fa_enabled);
CREATE INDEX IF NOT EXISTS idx_users_is_review_account ON users(is_review_account);
