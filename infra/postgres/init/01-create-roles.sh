#!/bin/sh
# Creates the database roles described in ADR-0001.
# Runs once, as the bootstrap superuser, through the official image entrypoint
# (docker-entrypoint-initdb.d). Also used by the Testcontainers setup.
#   app_owner: owns the schema; used only by Flyway migrations.
#   app_user:  runtime role; never superuser, never BYPASSRLS, never owns tables.
set -eu

: "${APP_OWNER_PASSWORD:?APP_OWNER_PASSWORD is required}"
: "${APP_USER_PASSWORD:?APP_USER_PASSWORD is required}"

psql -v ON_ERROR_STOP=1 \
    --username "$POSTGRES_USER" \
    --dbname "$POSTGRES_DB" \
    -v db="$POSTGRES_DB" \
    -v owner_password="$APP_OWNER_PASSWORD" \
    -v user_password="$APP_USER_PASSWORD" <<'EOSQL'
CREATE ROLE app_owner LOGIN PASSWORD :'owner_password'
    NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;

CREATE ROLE app_user LOGIN PASSWORD :'user_password'
    NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE NOINHERIT;

REVOKE ALL ON DATABASE :"db" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db" TO app_owner, app_user;

ALTER SCHEMA public OWNER TO app_owner;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO app_user;
EOSQL
