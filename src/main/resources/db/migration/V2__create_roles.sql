CREATE TABLE IF NOT EXISTS roles (
  id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  code VARCHAR(32)  NOT NULL,
  name VARCHAR(100) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_code ON roles (code);

INSERT INTO roles (code, name)
SELECT 'USER', 'User'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'USER');

INSERT INTO roles (code, name)
SELECT 'MODERATOR', 'Moderator'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'MODERATOR');

INSERT INTO roles (code, name)
SELECT 'ADMIN', 'Administrator'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE code = 'ADMIN');

CREATE TABLE IF NOT EXISTS user_roles (
  user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS ix_user_roles_role_id ON user_roles (role_id);
