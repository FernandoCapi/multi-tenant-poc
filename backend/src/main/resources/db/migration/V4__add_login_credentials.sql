-- Login credentials and lookup (ADR-0001).
-- The login happens before any tenant is known, so row-level security hides every user.
-- Instead of exposing a table without RLS, a single SECURITY DEFINER function owned by app_auth
-- (NOLOGIN, BYPASSRLS) returns the credentials of one e-mail. app_user may only execute it.

ALTER TABLE users ADD COLUMN password_hash varchar(100) NOT NULL;

-- The application reads users without the hash; hashes are only reachable through the function
REVOKE SELECT ON users FROM app_user;
GRANT SELECT (id, family_id, email, display_name, created_at) ON users TO app_user;

CREATE FUNCTION find_login_credentials(p_email text)
    RETURNS TABLE (user_id uuid, family_id uuid, password_hash varchar)
    LANGUAGE sql
    STABLE
    SECURITY DEFINER
    -- Pinned search_path: a definer function must not resolve objects from the caller's path
    SET search_path = public, pg_temp
AS $$
    SELECT u.id, u.family_id, u.password_hash
      FROM users u
     WHERE lower(u.email) = lower(p_email)
$$;

REVOKE ALL ON FUNCTION find_login_credentials(text) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION find_login_credentials(text) TO app_user;

GRANT USAGE ON SCHEMA public TO app_auth;
GRANT SELECT (id, family_id, email, password_hash) ON users TO app_auth;

-- The new owner needs CREATE on the schema only for the ownership transfer
GRANT CREATE ON SCHEMA public TO app_auth;
ALTER FUNCTION find_login_credentials(text) OWNER TO app_auth;
REVOKE CREATE ON SCHEMA public FROM app_auth;
