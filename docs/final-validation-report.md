# Matriz — Final Validation Report

This report consolidates the final project validation state for instructor review.

## 1. Docker Compose Validation

All Docker Compose services started and ran successfully:

- **PostgreSQL container** started and became healthy.
- **Backend container** started successfully and became healthy.
- **Frontend container** started successfully and became healthy.
- The application was accessible at `http://localhost:8081`.
- Manual application feature checks were successful.

## 2. Backend Test Results

The latest full Maven test run from `backend` reported:

- **106 tests, 0 failures, 0 errors, 4 skipped (102 executed).**
- BUILD SUCCESS.

The 4 skipped tests are opt-in PostgreSQL persistence integration tests that require dedicated test database credentials (`matriz_test_app` role, `matriz_test` database). A prior targeted run with credentials configured confirmed these tests pass.

## 3. Frontend Validation

- The frontend Docker image built successfully.
- The frontend production build completed without errors.

## 4. GitHub Actions CI Status

The latest GitHub Actions CI workflow completed successfully (green). The pipeline includes:

- Backend tests with an ephemeral PostgreSQL database.
- Frontend lint and build checks.
- Playwright E2E tests.
- Docker image builds for both backend and frontend.

## 5. Docker Log Review

Docker logs were reviewed after the Compose run. No unexpected errors or warnings were identified.

## 6. Environment File Verification

- `.env` is ignored by Git and is not tracked in the repository.
- `.env.example` is tracked and contains fictional placeholders.
- Spring Boot does not load `.env` automatically; environment variables are set explicitly in PowerShell sessions.

## 7. Flyway Migration Status

Three versioned migrations are present in `backend/src/main/resources/db/migration`:

- **V1:** Creates the schema (`users`, `projects`, `tasks`, `task_assignments`, `comments`).
- **V2:** Inserts deterministic demonstration records (demo accounts, projects, tasks, assignments, comments).
- **V3:** Disables demo account credentials with the `!DISABLED!` marker before enforcing `NOT NULL` on `password_hash`.

Flyway validated all three migrations. The database schema was at version 3 after application.

## 8. Render Deployment Status

Both frontend and backend services were deployed to Render. However, the deployment is not functional:

- The frontend Nginx reverse proxy cannot reach the backend.
- A direct `GET /api/auth/csrf` request to the backend URL returns HTTP 200.
- The equivalent request through the frontend proxy returns HTTP 401.
- The backend logs show no corresponding request for the failing proxied request.

Investigation included Nginx `BACKEND_HOST` configuration changes and a Spring Security `DispatcherType.ERROR` permission adjustment; neither resolved the issue. Cloudflare CDN loop detection is suspected but unconfirmed. A Render Static Site alternative for the frontend, with browser-to-backend API communication, is planned but has not yet been attempted. Render free-plan constraints prevent pursuing private networking.

## 9. Known Limitations

- **Render deployment:** Not functional due to Nginx reverse proxy failure. Static Site alternative is planned but not yet attempted.
- **Frontend tests:** Only Playwright E2E tests exist; frontend unit and component test infrastructure is not implemented.
- **Account lifecycle:** Public registration, password recovery, and account management beyond demo accounts are not implemented.
- **Production infrastructure:** Monitoring, backup, recovery, and production hosting are not configured.
- **Assignment eligibility:** Only two demo users are available for testing; assignment eligibility with a third user has not been verified.

## 10. Pending Items

- Complete the Render Static Site deployment attempt for the frontend.
- Confirm the root cause of the Nginx reverse proxy failure on Render.
- Resolve open decisions documented in `docs/deployment-plan.md` and `docs/constraints.md`.
