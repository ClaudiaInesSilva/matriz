# Matriz Test Plan

## 1. Purpose and Scope

This plan identifies existing test coverage and the minimal proposed coverage for task-title validation, task persistence, authorization and browser-level workflows.

The client-confirmed baseline remains organising projects, managing tasks and following work progress. Detailed validation rules, permissions and implementation choices are Matriz project decisions and are not additional client-confirmed requirements.

This document does not propose production changes or execute tests.

## 2. Test Environment and Isolation

### Backend

The backend test framework is JUnit 5 with Spring Boot Test, Spring MockMvc, AssertJ and Mockito. Run backend tests from `backend`:

```powershell
mvn test
```

Frontend verification currently includes lint and build checks:

```powershell
npm run lint
npm run build
```

Playwright is installed and configured for the browser E2E scenario described below. Other frontend test frameworks such as Cypress, Vitest, Jest and Testing Library are not configured.

### PostgreSQL isolation

PostgreSQL integration tests are opt-in and use only:

- URL: `jdbc:postgresql://127.0.0.1:5432/matriz_test`
- Role: `matriz_test_app`
- Environment variables: `MATRIZ_TEST_DB_USERNAME` and `MATRIZ_TEST_DB_PASSWORD`

`PostgresAssignmentCommentPersistenceTest` pins Spring's datasource to the dedicated URL and checks `SELECT current_database()` equals `matriz_test` before every test.

The test must never use, reset, truncate or modify `matriz_db`. No proposed scenario may use a database reset, schema reset or broad deletion.

## 3. CreateTaskRequest Unit Scenarios

The proposed unit test is a focused Jakarta Validator test of the actual `CreateTaskRequest` record.

Existing location:

- `backend/src/main/java/com/matriz/tasks/CreateTaskRequest.java`

The record has `@NotBlank` and `@Size(min = 3, max = 150)` constraints. Its compact constructor trims the title with `String.strip()` before Jakarta validation.

| Scenario | Requirement | Level | Test data | Expected result |
|---|---|---|---|---|
| Exact minimum title | FR03; US03 AC03.4 | Proposed Jakarta Validator unit test | Construct `CreateTaskRequest("abc", null, null, null)` | No title violations |
| Exact maximum title | FR03; US03 AC03.4 | Proposed Jakarta Validator unit test | Construct the DTO with exactly 150 characters | No title violations |
| Below-minimum title | FR03; US03 AC03.4 | Proposed Jakarta Validator unit test | Construct the DTO with `"ab"` | `@Size` violation |
| Above-maximum title | FR03; US03 AC03.4 | Proposed Jakarta Validator unit test | Construct the DTO with 151 characters | `@Size` violation |
| Empty and whitespace-only titles | FR03; US03 AC03.4 | Proposed Jakarta Validator unit test | `""`, `"   "` and `"\t"` | Compact constructor trims the value; `@NotBlank` rejects it |

The test should construct the actual DTO, obtain a Jakarta `Validator`, call `validate(request)`, and assert the resulting title violations.

### Existing automated coverage

`backend/src/test/java/com/matriz/tasks/CreateTaskRequestTest.java` provides focused Jakarta Validator coverage for exact boundary and trimming cases.

`backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` already has:

- `rejectsShortBlankAndWhitespaceOnlyTitles`
- `createsTaskUsingAuthenticatedCreatorAndDefaultsStatusAndPriority`

This controller coverage verifies behavior through MockMvc but does not isolate the exact Jakarta DTO boundary cases in a focused unit test.

## 4. PostgreSQL Integration Scenarios

### Existing create-and-retrieve coverage

`backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java` contains:

- `persistsTaskAcrossSeparateHttpRequests`
- `persistsAssignmentsAcrossSeparateHttpRequests`
- `persistsCommentAcrossSeparateHttpRequests`

The existing task test is the create-and-retrieve scenario to reuse:

- Requirement: FR03/FR04 and US03
- Preconditions: dedicated `matriz_test`, authenticated creator and existing project
- Test data: unique title, description, status and priority
- Expected result: POST returns `201`; a separate GET returns the same task fields and project association

### Negative integration scenario

The existing persistence test class contains:

- `rejectsTaskCreationForMissingProjectWithoutPersistingTask`

- Scenario: create a task for a nonexistent project
- Requirement: FR03/FR04 and US03 AC03.5
- Test data: authenticated user and a random UUID that does not identify a project
- Expected result: POST returns `404`; no task is created
- Level: opt-in PostgreSQL integration coverage

The mocked controller test `TaskControllerTest.createAndListReturnNotFoundForMissingProject` already verifies the missing-project behavior at the MockMvc level. The persistence test verifies the same response against real JPA persistence and checks `tasks.project_id` and `tasks.title` in `matriz_test`.

### Execution results

The PostgreSQL integration class was executed successfully with 4 tests, 0 failures, 0 errors and 0 skipped, against `matriz_test`.

### Data cleanup

The existing PostgreSQL class creates records in UUID-based relationships. Cleanup must target only IDs created by the test and follow foreign-key dependencies:

1. Delete task assignments.
2. Delete comments.
3. Delete tasks.
4. Delete projects.
5. Leave test users in place unless a proposed implementation can safely track and remove only those users after all dependent records are gone.

No proposed cleanup may use `TRUNCATE`, `DROP`, database reset, broad predicates or deletion of unrelated records.

The task test can track and delete its task, project and any created assignments/comments before finishing. The comment test can delete its comment, task and project. The assignment test can delete assignments, then the task and project. The owner and assignee users may remain in the dedicated test database to avoid broader cleanup dependencies. The negative test creates no task or project and requires no cleanup.

If cleanup of test users or all test records proves impractical, that limitation should remain explicit. Unique IDs are isolation but not cleanup.

## 5. Security and Authorization Scenarios

The authorization rules to preserve are:

- Any authenticated user may view and update tasks.
- Only the task creator or project owner may delete a task.
- Only the project owner may edit or delete a project.
- Project and task deletion restrictions do not restrict task viewing or task updating.

| Scenario | Requirement | Existing coverage | Proposed addition |
|---|---|---|---|
| Unrelated user cannot edit/delete project | FR03; US02 AC02.4, AC02.5, AC02.7 | `ProjectControllerTest.nonOwnerCannotUpdateOrDeleteProject` | None |
| Unrelated user cannot delete task | FR04; US03 AC03.6 | `TaskControllerTest.anotherAuthenticatedUserCannotDeleteTask` | None |
| Task creator can delete task | FR04; US03 AC03.6 | `TaskControllerTest.creatorCanDeleteTask` | None |
| Project owner can delete another user's task | FR04; US03 AC03.6 | `TaskControllerTest.projectOwnerCanDeleteTaskCreatedByAnotherUser` | None |
| Authenticated non-owner updates task | FR04; US03 AC03.2 | No explicit cross-user update test | Proposed MockMvc security test; assert that the update succeeds |
| Unrelated authenticated user views task or project | FR02/FR04 | Project view test and task retrieval test exist | Proposed explicit cross-user assertions only if existing coverage is not sufficient |

Security tests must not expect `403` when an authenticated non-owner updates or views a task.

Existing authentication coverage includes `SecurityConfigurationTest`, `AuthenticationControllerTest` and `MatrizUserDetailsServiceTest`.

## 6. Playwright E2E Scenario

### Actual executed coverage

On 2026-11-18, Chromium executed `frontend/e2e/matriz.spec.ts` with `npm run test:e2e`. The result was 1 passed and 0 failed in 4.1 seconds.

The executed scenario:

1. Authenticate.
2. Open one existing project.
3. Create a task.
4. Verify the task appears in the project task list.

The test:

- Logged in as `demo.alex`.
- Opened the seeded project.
- Created a uniquely titled task.
- Verified the task row.
- Opened the task detail route.
- Deleted the task through the UI and accepted the browser confirmation dialog.
- Verified that the task row was removed.

The backend was running against `matriz_test` during the executed run. Cleanup succeeded for the created task; no project or user cleanup was performed.

The test implementation attempts cleanup again from the task detail route if normal cleanup has not completed. If a cleanup failure occurs alongside an original test failure, it adds a `cleanup-failure` Playwright test annotation while preserving the original failure.

### Configuration

Required environment variables:

- `MATRIZ_E2E_USERNAME`
- `MATRIZ_E2E_PASSWORD`
- `MATRIZ_E2E_PROJECT_ID`
- Optionally `MATRIZ_E2E_TASK_TITLE_PREFIX`

The Playwright configuration supports the local Vite URL `http://127.0.0.1:5173` only.

Passwords must not be hardcoded.

The project must be identified deterministically by `MATRIZ_E2E_PROJECT_ID`. The browser should navigate directly to `#/projects/${MATRIZ_E2E_PROJECT_ID}` rather than infer a project from dashboard order or name.

Stable selectors:

- Username: `#username`
- Password: `#password`
- Sign-in: button with accessible name `Sign in`
- Open task form: panel button `Criar tarefa`
- Task title: `#new-task-title`
- Task description: `#new-task-description`
- Submit: button inside `form.task-create-form` named `Criar tarefa`
- Verification: `.task-row` containing the unique task title

The two `Criar tarefa` buttons must be distinguished by scope: the first opens the form, while the submit button is located inside `form.task-create-form`.

The test must use Playwright locator auto-waiting and must not use fixed sleeps.

## 7. Test Data and Cleanup Strategy

- Unit tests use inline DTO values; no database or external state is required.
- MockMvc controller tests use mock repositories and authenticated principals.
- PostgreSQL integration tests use UUID-based users, projects and titles.
- E2E test data must use one existing project selected by environment variable and a unique task title.
- The executed Playwright scenario deleted only the task it created through the UI. Its task row removal was verified. A retained test record is unnecessary unless future cleanup fails.

Only `matriz_test` may be used for PostgreSQL integration data. Never provision, reset, truncate or modify `matriz_db`.

## 8. Execution Commands

Run from `backend`:

```powershell
mvn test
mvn "-Dtest=TaskControllerTest" test
mvn "-Dtest=ProjectControllerTest" test
mvn "-Dtest=PostgresAssignmentCommentPersistenceTest" test
```

The following commands can be copied from the repository root. Configure test credentials in the current PowerShell session, then enter `backend` before running Maven:

```powershell
$env:MATRIZ_TEST_DB_USERNAME = 'matriz_test_app'
$secureTestPassword = Read-Host 'Enter the matriz_test_app password' -AsSecureString
$env:MATRIZ_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureTestPassword).Password
$secureTestPassword.Dispose()

Set-Location .\backend
mvn "-Dtest=PostgresAssignmentCommentPersistenceTest" test
Set-Location ..
```

Run frontend checks from `frontend`:

```powershell
npm run lint
npm run build
```

The Playwright command is:

```powershell
Set-Location .\frontend
npm run test:e2e
```

## 9. Known Limitations

### Existing limitations

- Frontend unit/component test infrastructure is not implemented; the implemented Playwright scenario provides browser-level coverage.
- PostgreSQL integration tests are skipped unless dedicated test credentials are configured.
- Controller tests largely use mocked repositories and do not prove real database persistence.
- PostgreSQL tests verify persistence across separate HTTP requests, not backend restart persistence.
- The PostgreSQL test class does not currently implement a cleanup framework for all created records.
- No Playwright browser run outside the 2026-11-18 result is recorded here.

### Tests not yet implemented

- Cross-user task update test confirming that an authenticated non-owner can successfully update a task
- Explicit cross-user task/project view assertions if they are not considered covered by existing tests
