-- Transitional schema: keep legacy users until the clean environment is ready.
ALTER TABLE users ADD COLUMN keycloak_user_id VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT uq_users_keycloak_user_id UNIQUE (keycloak_user_id);
ALTER TABLE users ADD CONSTRAINT ck_users_keycloak_user_id
    CHECK (keycloak_user_id IS NULL
        OR (keycloak_user_id <> '' AND keycloak_user_id = BTRIM(keycloak_user_id)));

-- Keycloak profiles must not need dummy local credentials or local roles.
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
ALTER TABLE users ALTER COLUMN role_id DROP NOT NULL;
