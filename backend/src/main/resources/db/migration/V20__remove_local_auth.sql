-- Refuse to discard legacy identities. Point the app at the clean Keycloak DB.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE keycloak_user_id IS NULL OR BTRIM(keycloak_user_id) = '') THEN
        RAISE EXCEPTION 'Local accounts remain. Use the prepared Keycloak database before removing local auth.';
    END IF;
END $$;

ALTER TABLE users ALTER COLUMN keycloak_user_id SET NOT NULL;
DROP TABLE refresh_tokens;
DROP TABLE tokens;
ALTER TABLE users DROP COLUMN role_id, DROP COLUMN password, DROP COLUMN active;
DROP TABLE roles;
