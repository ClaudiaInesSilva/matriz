# Changelog

All notable changes to the Matriz application are documented here.

## [0.1.0-rc.1] — Release Candidate

### Features

- Session-based authentication with CSRF protection (HttpOnly `JSESSIONID` cookie, SameSite=Lax).
- Project management: create, list, view, search by name, owner-only update and delete.
- Task management: create, list with pagination, view, update status and priority, filter by status and/or priority.
- Task assignments: multiple assignees per task, add and remove assignments, duplicate prevention.
- Task comments: add and list comments with author and timestamp.
- Dashboard view aggregating project and task data from existing APIs.
- Demo accounts `demo.alex` and `demo.sam` seeded by database migrations with interactive local password provisioning.
- Demo account auto-login endpoint (`POST /api/auth/demo-login`) for Render deployment, restricted to the `render` Spring profile.

### Infrastructure and Deployment

- PostgreSQL 18.7 persistence with Flyway versioned migrations (V1: schema, V2: seed data, V3: disable demo credentials).
- Docker Compose configuration with three services: PostgreSQL database, Spring Boot backend, and Nginx-served React frontend.
- Dockerfiles for backend (multi-stage Java 21/Maven) and frontend (multi-stage Node 24/Nginx).
- Safe demo-account provisioning via temporary loopback-only Compose override (`compose.provisioning.yaml`).
- GitHub Actions CI pipeline: backend tests with ephemeral PostgreSQL, frontend lint and build, Playwright E2E tests, and Docker image builds.
- Render deployment preparation: `render` Spring profile with demo auto-login and secure session cookie; Nginx configuration template with environment-variable-based backend URL.

### Testing

- 106 backend unit and integration tests (JUnit 5, Spring MockMvc, AssertJ, Mockito); the latest full Maven run reported 106 tests, 0 failures, 0 errors, 4 skipped (102 executed).
- 4 PostgreSQL persistence integration tests verifying task, assignment, and comment persistence across separate HTTP requests; these passed in a separate run with dedicated test database credentials configured.
- Playwright E2E tests covering authentication, project navigation, task creation, and task deletion.
- Frontend lint (Oxlint) and production build verification.

### Known Limitations

- Render deployment was attempted but is not functional: the frontend Nginx reverse proxy cannot reach the backend. A Render Static Site alternative is planned but has not yet been attempted.
- Frontend unit and component test infrastructure is not implemented; only Playwright E2E tests exist.
- Account lifecycle beyond demo accounts (registration, password recovery) is not implemented.
- Production hosting, monitoring, backup, and recovery strategy are not configured.
