# Matriz — Deployment Configuration

## 1. Purpose and Scope

This document describes the current deployment configuration for the Matriz application, including local Docker Compose execution, GitHub Actions CI, and Render deployment preparation.

The original client brief confirms the need to organise projects, manage tasks and follow work progress. Detailed behavior remains documented project scope rather than independently confirmed client requirements. Hosting, monitoring, backup, recovery and production infrastructure are outside this document.

## 2. Application Architecture

Matriz is a modular monolith with two deployment units:

- A Spring Boot backend under `backend/`.
- A React, TypeScript and Vite frontend under `frontend/`.
- PostgreSQL as the persistence service.

The backend owns application APIs, authentication, authorization and persistence. The frontend obtains project, task, assignment and comment data from the backend and computes dashboard status counts from existing APIs; there is no separate dashboard API.

Authentication uses an HttpOnly, server-side `JSESSIONID` session. State-changing requests obtain and send the configured XOR-protected CSRF token using `X-CSRF-TOKEN`.

## 3. Technologies

| Area | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 4.1.2; Spring Security 7.1.2 |
| Backend build | Maven (local installation required) |
| Persistence | Spring Data JPA, Hibernate |
| Database migrations | Flyway 13.12.1 |
| Database | PostgreSQL 18.7 |
| Frontend | React 19.3.1 and TypeScript 7.0.2 |
| Frontend build | Vite 8.3.8 resolved from `^8.3.6` |
| Frontend package management | npm 11 with `frontend/package-lock.json` |
| Containerization | Docker and Docker Compose |
| CI | GitHub Actions |
| E2E testing | Playwright 1.65.1 with Chromium |

## 4. Docker Configuration

### Dockerfiles

| File | Purpose |
|---|---|
| `backend/Dockerfile` | Multi-stage Java 21/Maven build and non-root runtime image for the Spring Boot JAR |
| `backend/.dockerignore` | Exclude Maven build output, IDE state and local environment files |
| `frontend/Dockerfile` | Multi-stage Node 24 build and Nginx runtime for the Vite production output |
| `frontend/.dockerignore` | Exclude dependencies, build output, test artifacts and local environment files |
| `frontend/nginx.conf.template` | Nginx configuration template: serves static files and proxies `/api/` to the backend |

The Nginx configuration template uses environment variables `${PORT}` and `${BACKEND_URL}` for runtime flexibility. The official Nginx Docker image processes `/etc/nginx/templates/*.template` at container startup, performing environment substitution to generate the active `nginx.conf`.

### Docker Compose

`compose.yaml` defines three services:

- **database** — PostgreSQL 18.7 with the `matriz_compose` database, health check, and persistent volume (`matriz_compose_data`).
- **backend** — Spring Boot with Flyway migrations. No Compose-level health check is configured; the Dockerfile includes a health check via `GET /api/auth/csrf`.
- **frontend** — Nginx serving the compiled React application, with health check on the container's internal port 80 (published to host port 8081).

The Compose database uses `matriz_compose`, separate from the local development database `matriz_db`. Compose and CI must never connect to `matriz_db`.

Demo accounts `demo.alex` and `demo.sam` are seeded by migrations without usable passwords. Interactive provisioning sets local-only passwords. See `README.md` for the provisioning procedure using `compose.provisioning.yaml`.

### Health Checks

- **Database:** `pg_isready` inside the PostgreSQL container (Compose and CI).
- **Backend:** The Dockerfile defines a health check via `curl` against `GET /api/auth/csrf` on port 8080. This confirms application availability but does not verify PostgreSQL connectivity. No Compose-level health check is configured for the backend service.
- **Frontend:** The Compose health check runs the following command inside the container on the internal Nginx port 80. The published host port is 8081.

  ```
  wget -q --spider http://127.0.0.1/
  ```

## 5. Environment Configuration

### Backend runtime variables

| Variable | Source | Default | Description |
|---|---|---|---|
| `DB_URL` | `application.properties` | `jdbc:postgresql://localhost:5432/matriz_db` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `application.properties` | `matriz_app` | Database role name |
| `DB_PASSWORD` | `application.properties` | (none) | Database role password (required) |
| `MATRIZ_SESSION_COOKIE_SECURE` | `application.properties` | `true` | Session cookie Secure attribute; `local` profile sets `false` |
| `SPRING_PROFILES_ACTIVE` | Compose / CI | (none) | Activates Spring profiles (e.g., `local`, `compose`, `render`) |
| `PORT` | `application.properties` | `8080` | Backend HTTP listen port |

### Frontend build-time variables

| Variable | Source | Default | Description |
|---|---|---|---|
| `VITE_DEMO_LOGIN_ENABLED` | `frontend/Dockerfile` ARG | `false` | Build-time flag exposed to the React app via `VITE_DEMO_LOGIN_ENABLED` env var during `npm run build` |

### Frontend runtime variables (Nginx / Docker)

| Variable | Source | Default | Description |
|---|---|---|---|
| `PORT` | `frontend/Dockerfile` ENV | `80` | Nginx listen port inside the container |
| `BACKEND_URL` | `frontend/Dockerfile` ENV | `http://backend:8080` | Backend URL for Nginx `/api/` reverse proxy |

### Frontend development variables (Vite)

| Variable | Source | Default | Description |
|---|---|---|---|
| `VITE_API_PROXY_TARGET` | `vite.config.ts` | `http://localhost:8080` | Backend target for Vite's `/api` development proxy |

### Playwright test variables

| Variable | Description |
|---|---|
| `MATRIZ_E2E_USERNAME` | Demo account username for E2E tests |
| `MATRIZ_E2E_PASSWORD` | Demo account password for E2E tests |
| `MATRIZ_E2E_PROJECT_ID` | Known project ID for E2E tests |
| `MATRIZ_E2E_TASK_TITLE_PREFIX` | Optional prefix for E2E task titles |

`.env.example` documents fictional placeholders. Spring Boot does not load `.env` automatically. Real `.env` files are excluded by `.gitignore`.

## 6. GitHub Actions CI Pipeline

`.github/workflows/ci.yml` defines three jobs:

### Backend Tests

Runs `mvn test` with an ephemeral PostgreSQL 18.7 service (`matriz_test` database, `matriz_test_app` role). The PostgreSQL integration test (`PostgresAssignmentCommentPersistenceTest`) connects to this ephemeral database, asserting `current_database()` equals `matriz_test`. Test credentials are supplied via `MATRIZ_TEST_DB_USERNAME` and `MATRIZ_TEST_DB_PASSWORD` environment variables.

### Frontend Checks

Runs `npm ci`, `npm run lint`, and `npm run build`. Then executes Playwright E2E tests by:

1. Starting a disposable PostgreSQL container (`matriz_test` database).
2. Starting the Spring Boot backend with the `local` profile against the test database.
3. Updating the demo account password hash via direct SQL in the ephemeral container.
4. Running `npx playwright test` against the running application.

### Docker Build

After backend tests and frontend checks pass, builds the backend and frontend Docker images tagged `0.1.0`.

## 7. Render Deployment Preparation

Render deployment is prepared but has not yet been completed. The following artifacts support Render deployment:

- **Backend:** `backend/src/main/resources/application-render.properties` activates the `render` Spring profile, enabling demo-account auto-login (`matriz.demo-accounts.auto-login-enabled=true`) and setting `server.servlet.session.cookie.secure=true`.
- **Frontend:** `frontend/nginx.conf.template` supports environment-variable-based backend URL configuration via `${BACKEND_URL}`, allowing the Nginx proxy to reach the Render backend service.
- **Demo login endpoint:** `POST /api/auth/demo-login` is available only when the `render` profile is active and `matriz.demo-accounts.auto-login-enabled=true`. It accepts `demo.alex` or `demo.sam` usernames without a password.

Render deployment requires creating the Render services, configuring environment variables, and provisioning the demo accounts in the Render database. These steps have not been performed.

## 8. Database Isolation

| Environment | Database | Role | Purpose |
|---|---|---|---|
| Local development | `matriz_db` | `matriz_app` | Interactive local development |
| Docker Compose | `matriz_compose` | `matriz_compose_app` | Local containerized execution |
| CI (backend tests) | `matriz_test` | `matriz_test_app` | Ephemeral CI PostgreSQL integration tests |
| CI (Playwright E2E) | `matriz_test` | `matriz_test_app` | Ephemeral CI E2E test database |

`matriz_db` must never be referenced, provisioned, reset or modified by Compose or CI tooling.

## 9. Security Considerations

- The backend runtime container uses a non-root numeric user.
- Database passwords and demo passwords remain outside source control and container build arguments.
- Compose uses `matriz_compose`; CI uses ephemeral `matriz_test`. Neither touches `matriz_db`.
- The backend preserves HttpOnly `JSESSIONID`, SameSite=Lax, and secure-cookie configuration.
- Nginx preserves the `/api` prefix and browser credentials for CSRF token retrieval and state-changing requests.
- PostgreSQL data uses a named persistent volume only for local Compose execution.
- No Redis, message broker, monitoring platform, backup service, or production hosting infrastructure is configured.

## 10. Open Decisions and Limitations

The following remain unresolved:

- Whether Maven should be pinned with a Maven Wrapper or remain Maven-installation dependent.
- The final release date and release authority.
- Production hosting, monitoring, backup, and recovery strategy.
- Render deployment has been prepared but not yet executed.
