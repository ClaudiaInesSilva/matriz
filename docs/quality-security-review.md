# Matriz Quality and Security Review

## 1. Review scope

The read-only review inspected the Spring Security configuration, project, task, assignment and comment controllers and services, relevant repositories, frontend API behavior, existing unit/controller/integration/E2E tests, Git history and frontend dependency metadata.

The reviewed implementation and tests cover Matriz project decisions rather than additional requirements independently confirmed by the original client brief.

## 2. Code review

Project updates and deletions enforce project ownership in `ProjectService.requireOwner`. Project IDs are changed by the caller, but the service resolves the project and compares its `ownerId` with the authenticated principal.

Task updates do not impose creator or owner restrictions; task viewing and updating are available to authenticated users. Task deletion requires the caller to be the task creator or the project owner in `TaskService.delete`.

Task creation requires an existing project and derives the creator from the authenticated principal. Assignment and comment operations resolve the task or project before persisting changes, and comment author and timestamp data are derived server-side rather than accepted from client author fields.

No confirmed IDOR or Broken Access Control vulnerability was identified against the current Matriz authorization rules.

## 3. Authorization review

- **Projects:** all authenticated users may list, view and create projects. Only the project owner may edit or delete that project.
- **Tasks:** all authenticated users may list, view and update tasks. Any authenticated user may create a task in an existing project.
- **Task deletion:** only the task creator or the project owner may delete a task.
- **Assignments:** any authenticated user may add or remove assignments under the current implementation policy.
- **Comments:** any authenticated user may view or add comments; the authenticated principal supplies the author and the server supplies the creation timestamp.

These restrictions are project decisions and should not be changed without an explicit requirements decision.

## 4. Security findings

### Finding 1 - ReDoS in the frontend development/build dependency chain

**Severity:** High (as reported by npm audit; development/build dependency).

**Affected files and evidence**

- `frontend/package-lock.json:1562-1566` contains `picomatch@4.0.7`.
- `frontend/package-lock.json:1710-1713` connects `tinyglobby`/`fdir` to `picomatch`.
- `frontend/package-lock.json:1772-1783` shows Vite `8.3.8` depending on `picomatch@4.0.7`.
- `npm ls picomatch --all` confirmed the chain: Vite → picomatch and Vite → tinyglobby → fdir → picomatch.

**Observed evidence**

The full npm audit reported a high-severity ReDoS advisory for `picomatch@4.0.7`, involving single-branch repeated extglobs. The finding is in the development/build dependency chain. The production-only npm audit reported 0 vulnerabilities.

**Impact**

This may expose build or development tooling to denial-of-service conditions if it processes attacker-controlled glob patterns. The repository evidence does not confirm exploitable production runtime exposure.

**Confidence and limitation**

Confirmed facts are the locked version and dependency chain, plus the audit advisory and production-only result. No production deployment or direct exploit was tested.

**Proposed correction**

Keep the finding open for human-approved evaluation of a compatible Vite/plugin/picomatch upgrade. Do not apply forced major-version fixes without compatibility and regression validation.

**Regression test**

After an approved upgrade, run the frontend build and Playwright smoke test, plus dependency/audit verification. No application behavior change is expected.

No forced dependency upgrade was applied during this review.

### Finding 2 — Local provisioning database guard is not database-name-specific

**Severity:** Low / confidence 7/10.

**Affected file and evidence**

- `backend/src/main/java/com/matriz/users/LocalDatabaseUrlGuard.java:52-61` validates configured `DB_URL` and `SPRING_DATASOURCE_URL`.
- `LocalDatabaseUrlGuard.java:78-106` accepts loopback PostgreSQL JDBC URLs without requiring the database name to be `matriz_test`.
- `LocalDemoAccountProvisioner.java:25-36` looks up `demo.alex` and `demo.sam` and replaces their password hashes in the selected database.

**Observed evidence**

The guard restricts provisioning to loopback PostgreSQL and rejects remote or alternate datasource configurations. It does not pin or require `matriz_test`; another valid local database name could therefore be selected with an incorrect environment.

**Impact**

If provisioning is invoked with `matriz_db` or another local database URL, it could update demo-account password hashes in that database. This is an operational configuration risk. Provisioning against the wrong database was not tested and no such modification is claimed.

**Confidence and limitation**

The URL guard behavior is confirmed by source inspection. Database selection and account modification against `matriz_db` were not executed.

**Proposed correction**

Whether the provisioner is permitted to target development databases is a human decision. If provisioning is required to remain test-only, introduce an explicit allowed-database check that requires `matriz_test`; preserve loopback restrictions and ensure the resolved datasource URL is pinned. No implementation change was made.

**Regression test**

Add a provisioning configuration test that rejects `matriz_db` and accepts only the intended isolated database URL when test-only restriction is approved.

## 5. Test coverage and limitations

Existing automated coverage includes:

- Project owner/non-owner update and delete tests.
- Task creator, project-owner and unrelated-user deletion tests.
- Authenticated task retrieval and a cross-identity task update test.
- Assignment add/remove, duplicate and missing-resource tests.
- Comment authentication, author derivation and validation tests.
- Authentication, CSRF and session-security tests.
- A real-PostgreSQL integration class for assignments, comments and task persistence.
- A Playwright Chromium E2E test that logged in, opened the seeded project, created a task, verified it, opened it, deleted it and verified removal. The run passed 1 test with 0 failures.

The PostgreSQL integration class was executed successfully with 4 tests, 0 failures, 0 errors and 0 skipped against `matriz_test`.

Dedicated two-user, real-session validation of project/task/assignment/comment ID-changing scenarios was not executed. MockMvc and controller tests do not prove real database authorization behavior. Missing test coverage is not itself evidence of a vulnerability.

### Verification executed during the latest read-only review

- Git status, recent history and relevant commit scopes.
- Read-only source and test inspection.
- Full frontend npm audit.
- Production-only npm audit.
- `npm ls picomatch --all`.

### Checks not executed during the latest read-only review

- Dedicated two-user, real-session authorization tests against PostgreSQL.
- A compatible dependency upgrade and subsequent regression verification.
- Production deployment security testing.
- Backend unit/integration test execution.
- Playwright browser execution.
- Database operations or provisioning commands.

### Earlier recorded executions

The PostgreSQL integration class was previously executed successfully with 4 tests, 0 failures, 0 errors and 0 skipped against `matriz_test`. The Playwright Chromium scenario was previously executed successfully with 1 passed and 0 failed. These are earlier recorded results, not checks executed during the latest review.

## 6. Decisions and recommendations

No application security fix was justified by the review against the documented Matriz authorization rules.

Keep the `picomatch`/Vite dependency finding open until a compatible upgrade is approved and assessed.

Decide whether local demo-account provisioning is restricted to `matriz_test` or may legitimately target another loopback PostgreSQL database. Add the database restriction only if approved.

Future work may add two-user PostgreSQL or direct-API tests covering:

- a non-owner updating a task successfully;
- a non-owner failing to delete another user's task;
- a project owner deleting another user's task successfully;
- project owner-only update/deletion behavior;
- assignment and comment operations across two real identities.
