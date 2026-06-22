-- PgBouncer auth_query infrastructure
--
-- Allows PgBouncer to look up SCRAM-SHA-256 password hashes directly from
-- pg_shadow instead of relying on a manually-maintained userlist.txt file.
-- PgBouncer connects as the 'pgbouncer' role and calls get_auth($1) to retrieve
-- the hash for any connecting client. userlist.txt is then only needed for the
-- 'pgbouncer' admin user itself.
--
-- SECURITY DEFINER: runs as ziyarah_user (superuser) regardless of the caller,
-- so pgbouncer (a non-superuser) can retrieve hashes from pg_shadow.
-- search_path is pinned to pg_catalog to block search_path injection attacks.

CREATE SCHEMA IF NOT EXISTS pgbouncer;

DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'pgbouncer') THEN
        CREATE ROLE pgbouncer LOGIN;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA pgbouncer TO pgbouncer;

CREATE OR REPLACE FUNCTION pgbouncer.get_auth(p_usename TEXT)
    RETURNS TABLE(username TEXT, password TEXT)
    LANGUAGE SQL
    SECURITY DEFINER
    SET search_path = pg_catalog
AS $$
    SELECT usename::TEXT, passwd::TEXT
    FROM pg_shadow
    WHERE usename = p_usename
$$;

REVOKE ALL ON FUNCTION pgbouncer.get_auth(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION pgbouncer.get_auth(TEXT) TO pgbouncer;
