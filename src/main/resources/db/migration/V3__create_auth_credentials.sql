CREATE TABLE IF NOT EXISTS user_password_credentials (
  user_id       BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
  password_hash VARCHAR(255) NOT NULL,
  updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_identities (
  id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id          BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  provider         VARCHAR(32)  NOT NULL,
  provider_user_id VARCHAR(255) NOT NULL,
  email            VARCHAR(320),
  created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_identities_provider_subject
  ON user_identities (provider, provider_user_id);

CREATE INDEX IF NOT EXISTS ix_user_identities_user_id ON user_identities (user_id);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  token_hash CHAR(64)    NOT NULL,
  expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  version    BIGINT      NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS ix_refresh_tokens_user_id ON refresh_tokens (user_id);
