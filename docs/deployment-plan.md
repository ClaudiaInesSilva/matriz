# Matriz Delivery Setup Plan

## 1. Purpose and Scope

This document records the approved plan for adding a reproducible local containerized Matriz delivery setup.

The plan covers:

- Reproducible local execution with Docker and Docker Compose.
- Automated backend and frontend quality checks and builds through GitHub Actions.
- Docker image validation and database isolation.
- Playwright end-to-end verification subject to safe CI demo-account provisioning.
- Documentation, changelog and candidate release preparation.

This is a delivery plan, not evidence that delivery work is complete. Dockerfiles, Compose orchestration, CI workflows and release artifacts do not exist until their implementation phases are completed and verified.

The original client brief confirms project organisation, task management and progress visibility. Detailed behavior remains documented project scope rather than independently confirmed client requirements. Hosting, monitoring, backup, recovery and production infrastructure are outside this plan.

## 2. Current Architecture

Matriz is a modular monolith with two deployment units:

- A Spring Boot backend under `backend/`.
- A React, TypeScript and Vite frontend under `frontend/`.
- PostgreSQL as the persistence service.

The backend is the sole owner of application APIs, authentication, authorization and persistence. The frontend obtains project, task, assignment and comment data from the backend and computes dashboard status counts from existing APIs; there is no separate dashboard API.

The components communicate over HTTP. In development, Vite proxies `/api` requests to Spring Boot. The planned container setup will instead use Nginx to serve the frontend and proxy same-origin `/api` requests to Spring Boot.

Authentication uses an HttpOnly, server-side `JSESSIONID` session. State-changing requests obtain and send the configured XOR-protected CSRF token using `X-CSRF-TOKEN`. The frontend sends browser credentials and requires the same behavior after containerization.

The existing proposed API contract is `openapi.yaml`. It describes authentication, projects, tasks, assignments and comments.

Relevant evidence:

- `docs/architecture.md`
- `README.md`
- `backend/src/main/java/com/matriz/`
- `frontend/src/`
- `openapi.yaml`

## 3. Technologies and Dependencies

### Existing application technology

| Area | Technology |
|---|---|
| Backend language | Java 21 |
| Backend framework | Spring Boot 4.1.2; Spring Security 7.1.2 |
| Backend build | Maven; no Maven Wrapper is currently committed |
| Persistence | Spring Data JPA, Hibernate |
| Database migrations | Flyway 13.12.1 |
| Database | PostgreSQL 18.7 |
| Frontend | React 19.3.1 and TypeScript 7.0.2 |
| Frontend build | Vite 8.3.8 resolved from a `^8.3.6` declaration |
| Frontend package management | npm 11 with `frontend/package-lock.json` |

Evidence: `backend/pom.xml`, `frontend/package.json`, `frontend/package-lock.json`, `docs/architecture.md` and `README.md`.

### Planned delivery technology

The planned setup uses:

- Docker and Docker Compose for local containerized execution.
- A multi-stage backend Maven image and a multi-stage frontend Node/Nginx image.
- Nginx for static frontend serving and `/api` reverse proxying.
- GitHub Actions for Maven tests, frontend checks, isolated PostgreSQL integration tests and Playwright CI.

These tools are planned additions, not currently existing implementation.

### Existing automated test tooling

- JUnit 5 and Spring Boot Test.
- Spring MockMvc, AssertJ and Mockito.
- npm lint through Oxlint.
- TypeScript and Vite production build.
- Playwright 1.65.1 with Chromium.

Evidence: `backend/pom.xml`, `frontend/package.json`, `frontend/e2e/matriz.spec.ts`, `frontend/playwright.config.ts` and `docs/test-plan.md`.

## 4. Configuration and Database Isolation

### Docker Compose database

Compose will use the separate database name `matriz_compose`. The backend will receive a datasource URL using Compose service DNS, for example:

`jdbc:postgresql://database:5432/matriz_compose`

The Compose backend will receive separate username and password variables. It must not use `matriz_db`.

The existing local development database is `matriz_db`. Compose and GitHub Actions must never connect to, provision, reset or modify `matriz_db`.

### CI integration-test database

GitHub Actions will create an ephemeral PostgreSQL service containing `matriz_test`, the role `matriz_test_app` and an ephemeral password.

The existing integration test must remain unchanged with respect to:

- Fixed JDBC URL: `jdbc:postgresql://127.0.0.1:5432/matriz_test`
- Role environment variables:
  - `MATRIZ_TEST_DB_USERNAME`
  - `MATRIZ_TEST_DB_PASSWORD`
- Database-name assertion: `SELECT current_database()` must equal `matriz_test`

Evidence: `backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java`.

### Environment configuration

Spring runtime variables are:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `MATRIZ_SESSION_COOKIE_SECURE`

Spring consumes these variables during runtime. The existing datasource configuration is in `backend/src/main/resources/application.properties`.

The frontend build-time/default API configuration is:

- `VITE_API_BASE_URL`, which defaults to `/api` in `frontend/src/api/apiClient.ts`.

The development-only Vite proxy consumes `VITE_API_PROXY_TARGET`; the container build will use the same-origin `/api` path through Nginx.

Playwright variables are test-process variables:

- `MATRIZ_E2E_USERNAME`
- `MATRIZ_E2E_PASSWORD`
- `MATRIZ_E2E_PROJECT_ID`
- `MATRIZ_E2E_TASK_TITLE_PREFIX` is optional

`.env.example` will document fictional placeholders and variable purposes. Spring Boot does not load this file automatically. Compose `.env` is used for interpolation into `compose.yaml`, and explicit `environment:` or secret-backed values are passed into containers. Real credentials must not be committed.

Evidence: `.env.example`, `frontend/vite.config.ts`, `frontend/playwright.config.ts`, `frontend/e2e/matriz.spec.ts` and `README.md`.

## 5. Planned Files

### Docker

| File | Purpose |
|---|---|
| `backend/Dockerfile` | Multi-stage Java 21/Maven build and non-root runtime image for the Spring Boot JAR |
| `backend/.dockerignore` | Exclude Maven build output, IDE state and local environment files from backend build context |
| `frontend/Dockerfile` | Multi-stage Node 24 build and Nginx runtime for the Vite production output |
| `frontend/.dockerignore` | Exclude dependencies, build output, test artifacts and local environment files from frontend build context |
| `frontend/nginx.conf` | Serve static files and proxy `/api/` requests to the backend service |

The backend health check will use the existing `GET /api/auth/csrf` endpoint to confirm application availability; it will not independently verify PostgreSQL connectivity. Database health must be checked separately. The planned backend runtime requires only `curl` for this check. Spring Actuator is not planned because no Actuator dependency currently exists.

### Compose

| File | Purpose |
|---|---|
| `compose.yaml` | Define PostgreSQL, backend and frontend services, the `matriz_compose` database, service health checks and a persistent PostgreSQL volume |

### GitHub Actions

| File | Purpose |
|---|---|
| `.github/workflows/ci.yml` | Run backend tests, frontend lint/build, isolated PostgreSQL integration tests, image builds and Playwright only after safe provision of test data |

### Configuration

| File | Purpose |
|---|---|
| `.env.example` | Document fictional Compose and test variable names without real credentials |

The existing `.gitignore` already excludes real `.env` files while allowing `.env.example`.

### Documentation and release

| File | Purpose |
|---|---|
| `README.md` | Update with container startup and delivery check commands |
| `docs/architecture.md` | Add container boundaries and Nginx routing |
| `docs/test-plan.md` | Document container and CI test setup, database isolation and Playwright prerequisites |
| `docs/verification.md` | Record only newly obtained CI/container verification evidence |
| `frontend/README.md` | Clarify container same-origin proxy behavior when it differs from Vite development proxy behavior |
| `CHANGELOG.md` | Record the candidate delivery change set and any candidate `0.1.0` release notes |

No additional planning documents are necessary.

## 6. Implementation Sequence

1. **Docker implementation**
   - Create the backend Dockerfile and `.dockerignore`.
   - Create the frontend Dockerfile, `.dockerignore` and Nginx configuration.
   - Validate the exact Spring Boot JAR name and frontend build inputs.
   - Validate the backend health endpoint and non-root runtime.

2. **Docker Compose implementation**
   - Create `compose.yaml` with PostgreSQL, backend and frontend.
   - Configure `matriz_compose` separately from `matriz_db`.
   - Add PostgreSQL, backend and frontend health checks.
   - Add a named persistent PostgreSQL volume.
   - Verify service startup and same-origin `/api` routing.

3. **GitHub Actions implementation**
   - Run backend tests.
   - Run frontend lint and production build.
   - Run container build smoke checks.
   - Create the ephemeral `matriz_test` PostgreSQL service and credentials.
   - Run the PostgreSQL integration test without changing its fixed URL or safety assertions.

4. **Playwright implementation**
   - Investigate the smallest safe demo-account provisioning approach.
   - Provision isolated CI data without weakening database guards or hardcoding credentials.
   - Start the application against CI-only PostgreSQL data.
   - Install and run Chromium.
   - Use the existing Playwright cleanup behavior.
   - Change provisioning or test code only if isolated CI execution demonstrates it is required.

5. **Documentation and release preparation**
   - Update only the relevant existing documentation.
   - Record authoritative CI test counts; the historical 85-test reports are not fresh evidence.
   - Add a changelog entry in `CHANGELOG.md` with candidate `0.1.0` release notes.
   - Do not describe candidate release notes as an actual release.

6. **Final validation**
   - Confirm no Compose or CI path references `matriz_db`.
   - Confirm integration tests assert and reach `matriz_test`.
   - Confirm session and CSRF behavior through the Nginx proxy.
   - Confirm health checks and volume behavior.

## 7. Security and Operational Considerations

- The backend runtime container will use a non-root numeric user. It does not require application-level root privileges.
- Database passwords, demo passwords and test passwords must remain outside source control and container build arguments.
- The backend, frontend and test containers must not contain real credentials.
- Compose will use a separate `matriz_compose` database. CI will use a separate ephemeral `matriz_test` database.
- `matriz_db` must never be referenced or modified by delivery tooling.
- The backend will preserve HttpOnly `JSESSIONID`, SameSite and secure-cookie configuration.
- Nginx must preserve the `/api` prefix and browser credentials so CSRF token retrieval and state-changing requests continue to work.
- PostgreSQL data will use a named persistent volume only for local Compose execution.
- Backend readiness will use `GET /api/auth/csrf`, not a new Actuator dependency. This HTTP check confirms application availability but does not verify PostgreSQL connectivity, which must be checked separately.
- No Redis, message broker, monitoring platform, backup service or production hosting infrastructure is planned.

## 8. Open Decisions and Limitations

The following remain unresolved and must not be presented as confirmed decisions:

- Whether Playwright will run on every pull request or only for a selected workflow trigger.
- The exact CI-safe demo-account provisioning mechanism.
- Whether `frontend/playwright.config.ts` needs configurable base URL support for CI.
- Whether Maven should be pinned with a Maven Wrapper or remain Maven-installation dependent.
- The exact Compose port publication and local host exposure.
- The PostgreSQL Compose image tag.
- The final candidate release date and release authority.
- Candidate version `0.1.0` is proposed, not an actual release.

The existing Playwright test has one implementation-dependent constraint: it requires a running backend, usable demo credentials and an existing project. The migration seed creates demo users and two demo projects, but V3 disables seeded passwords until explicit provisioning. CI must provision credentials in an isolated database without changing or weakening `LocalDatabaseUrlGuard`.

The historical Surefire report set was available in `backend/target/surefire-reports/`, but the exact reported execution has to be reconciled against a clean CI run. The authoritative backend test counts must come from that CI execution.
