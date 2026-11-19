\getenv app_password MATRIZ_COMPOSE_DB_PASSWORD

CREATE ROLE matriz_compose_app
    LOGIN
    PASSWORD :'app_password'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION
    NOBYPASSRLS;

GRANT CONNECT ON DATABASE matriz_compose TO matriz_compose_app;
GRANT USAGE, CREATE ON SCHEMA public TO matriz_compose_app;
