# Matriz 0.1.0-rc.1 — Release Notes

**Status:** Release candidate. This is not a stable production release.

## Overview

Matriz is a project and task management application for work teams. The original client brief confirms the need to organise projects, manage tasks and follow work progress. Detailed behaviors documented in the application are project implementation decisions, not additional client-confirmed requirements.

This release candidate provides a fully functional local deployment via Docker Compose, a complete CI pipeline via GitHub Actions, and prepared (but not yet executed) Render deployment configuration.

## Capabilities

- **Authentication:** Session-based login with CSRF protection. Demo accounts `demo.alex` and `demo.sam` are provisioned with interactive local passwords.
- **Projects:** Create, list, view, and search projects. Project owners can update and delete their projects; deletion is blocked while tasks remain.
- **Tasks:** Create, list (with pagination), view, and update tasks. Tasks support status (`TODO`, `IN_PROGRESS`, `DONE`) and priority (`LOW`, `MEDIUM`, `HIGH`). Any authenticated user can create and update tasks; only the project owner or task creator can delete a task.
- **Assignments:** Assign and unassign users to tasks. Multiple assignees per task are supported; duplicate assignments are prevented.
- **Comments:** Add and view comments on tasks. Comments record the author and creation timestamp.
- **Dashboard:** Aggregated view of project and task data, including status counts across all projects.

## Technologies

- **Backend:** Java 21, Spring Boot 4.1.2, Spring Security 7.1.2, Spring Data JPA, Hibernate, Flyway 13.12.1.
- **Database:** PostgreSQL 18.7.
- **Frontend:** React 19.3.1, TypeScript 7.0.2, Vite 8.3.8, Node.js 24.
- **Containerization:** Docker and Docker Compose.
- **CI:** GitHub Actions with backend tests, frontend checks, Playwright E2E, and Docker image builds.

## Local Setup

### Docker Compose

The recommended local setup uses Docker Compose with three services (database, backend, frontend). After building the Docker images and starting the database, provision the demo accounts interactively, then start all services. The frontend is accessible at `http://localhost:8081`.

See `README.md` for the complete step-by-step procedure, including safe demo-account provisioning via `compose.provisioning.yaml`.

### Development servers

Alternatively, run the backend and frontend as local development servers using Maven and Vite. See `README.md` for instructions.

## Demo Account Provisioning

Database migrations seed `demo.alex` and `demo.sam` without usable passwords. An interactive provisioning command sets local-only passwords. No default demo passwords exist. Passwords are stored as BCrypt hashes. The provisioning process uses a strict guard to prevent accidental use against the wrong database.

## Testing

- **Backend:** 106 unit and integration tests (JUnit 5, Spring MockMvc); the latest full Maven run reported 102 executed and 4 skipped. The 4 PostgreSQL persistence integration tests verify real database persistence across separate HTTP requests and passed in a separate run with dedicated test database credentials configured.
- **Frontend:** Playwright E2E tests covering authentication, project navigation, task creation, and task deletion. These run locally and in the CI pipeline.
- **CI:** GitHub Actions runs backend tests with an ephemeral PostgreSQL database, frontend lint and build checks, Playwright E2E tests, and Docker image builds.

## Known Limitations

- **Render deployment:** Deployment artifacts (Spring profile, Nginx template, demo-login endpoint) are prepared but Render deployment has not yet been executed.
- **Frontend tests:** Only Playwright E2E tests exist; frontend unit and component test infrastructure is not implemented.
- **Account lifecycle:** Public registration, password recovery, and account management beyond demo accounts are not implemented.
- **Production infrastructure:** Monitoring, backup, recovery, and production hosting are not configured.
- **Assignment eligibility:** Only two demo users are available for testing; assignment eligibility with a third user has not been verified.

## Security Considerations

- Session cookies are HttpOnly and SameSite=Lax; the `Secure` attribute defaults to `true` and is disabled only in the local development profile.
- CSRF protection requires the `X-CSRF-TOKEN` header for all state-changing requests. Tokens are obtained via `GET /api/auth/csrf` and refreshed after login and logout.
- Passwords are stored as BCrypt hashes. No plaintext passwords or credentials are committed to the repository.
- Database isolation: local development uses `matriz_db`, Docker Compose uses `matriz_compose`, and CI uses an ephemeral `matriz_test`. No tooling connects to `matriz_db` from Compose or CI.
- The backend Docker runtime uses a non-root user.

## Important Notes

- This is a release candidate (`0.1.0-rc.1`), not a stable production release.
- Render deployment is prepared but not yet completed.
- Detailed application behaviors are project implementation decisions, not client-confirmed requirements.
- See `docs/requirements.md`, `docs/user-stories.md`, `docs/constraints.md`, and `docs/deployment-plan.md` for full scope, assumptions, and deployment details.
