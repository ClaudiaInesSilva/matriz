# Matriz Verification Record

## Purpose and scope

This document records the verification evidence available for the current Matriz implementation. Detailed application behaviors in `docs/requirements.md` are project implementation decisions and scope; they should not be read as additional requirements confirmed by the client.

Verification combines user-reported manual checks with automated backend tests. These evidence types are identified separately below. Manual results were confirmed by the user; no screenshots, detailed browser logs, or raw PowerShell request/response transcripts were retained.

## Test environment and prerequisites

- Backend: Java 21 and Maven, run from the `backend` directory.
- PostgreSQL integration tests: local PostgreSQL test database `matriz_test`, with `MATRIZ_TEST_DB_USERNAME` and `MATRIZ_TEST_DB_PASSWORD` configured in the test process.
- The integration-test class pins its JDBC URL to `jdbc:postgresql://127.0.0.1:5432/matriz_test`. Its `verifyDatabaseTarget` setup checks `SELECT current_database()` equals `matriz_test` before each test. The tests do not target `matriz_db`.
- The environment and date of the user-reported manual browser/API checks were not recorded.

## Verification approach

- **Manual browser/API checks:** Observations reported by the user from the application UI and direct API calls. They are not automated-test results.
- **Automated backend tests:** JUnit/Spring MockMvc and unit/configuration tests. Controller tests generally use mocked repositories and establish API behavior, not database persistence.
- **PostgreSQL integration tests:** `@SpringBootTest`/MockMvc tests using the real application and the guarded `matriz_test` database. These verify persistence across separate HTTP requests. Backend-restart persistence was checked manually, not by these integration tests.

## Scenario results

| Scenario | Expected result | Observed result | Verification method and evidence | Result |
|---|---|---|---|---|
| 01 — Application access | Valid login works, invalid credentials are rejected, and protected pages require authentication. | Valid login succeeded; invalid credentials were rejected; unauthenticated access to protected pages redirected to login. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/security/AuthenticationControllerTest.java` — `validLoginRotatesSessionPersistsAcrossRequestsAndInvalidatesOldCsrfToken`, `invalidCredentialsAndUnauthenticatedProfileAreRejected`, `profileIdentifiesEachAuthenticatedUserFromTheSecurityContext`, `logoutFilterInvalidatesSessionAndSupportsFreshCsrfTokenAfterLogout`; `backend/src/test/java/com/matriz/security/SecurityConfigurationTest.java` — `requiresAuthenticationForProtectedApiRequestsByDefault`. | Pass |
| 02 — Project management | Projects can be created, listed, opened, searched and edited; non-owners cannot edit/delete; owners can delete empty projects. | These operations behaved as expected. A second user could view a project but could not edit or delete it; its owner could delete an empty project. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/projects/ProjectControllerTest.java` — `createsProjectWithAuthenticatedPrincipalAsOwnerIgnoringClientOwnerId`, `listsAllProjectsAndAppliesTrimmedCaseInsensitivePartialSearch`, `anyAuthenticatedUserCanViewAProject`, `ownerCanUpdateProjectWithoutChangingOwnership`, `nonOwnerCannotUpdateOrDeleteProject`, `ownerCanDeleteProjectWithoutTasks`, `rejectsEmptyAndWhitespaceOnlyProjectNamesOnCreate`, `rejectsEmptyAndWhitespaceOnlyProjectNamesOnUpdateWithoutChangingProject`. Controller tests use mocked repositories. | Pass |
| 03 — Task persistence | Created tasks remain available after refreshing/re-authenticating and after a backend restart while PostgreSQL remains running. | A newly created task appeared without refresh and remained available with its original details after the backend was restarted and the user refreshed/re-authenticated. | Manual browser check (user-reported). Automated reference: `backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java` — `persistsTaskAcrossSeparateHttpRequests` creates through the API and retrieves through another HTTP request; it does not restart the backend. | Pass |
| 04 — Data validation | Invalid project names, task titles, descriptions and comments are rejected, with errors shown and invalid data not accepted. | Empty/whitespace project names; empty, whitespace-only and too-short task titles; descriptions over 2,000 characters; and whitespace-only comments were rejected. Validation errors appeared and invalid data was not accepted. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `rejectsShortBlankAndWhitespaceOnlyTitles`, `rejectsDescriptionLongerThanTwoThousandCharacters`; `backend/src/test/java/com/matriz/comments/CommentControllerTest.java` — `rejectsEmptyWhitespaceOnlyAndOverLimitCommentContent`; `backend/src/test/java/com/matriz/projects/ProjectControllerTest.java` — `rejectsProjectNameOutsideDocumentedLength`, `rejectsEmptyAndWhitespaceOnlyProjectNamesOnCreate`, `rejectsEmptyAndWhitespaceOnlyProjectNamesOnUpdateWithoutChangingProject`. | Pass |
| 05 — Status and priority | Initial values are correct; changes, individual/combined filters and dashboard counts behave consistently. | Initial values were correct; status and priority changes appeared immediately; status, priority and combined filters worked; dashboard counts updated after status changes. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `createsTaskUsingAuthenticatedCreatorAndDefaultsStatusAndPriority`, `listsProjectTasksAndAppliesEitherOrBothFilters`, `updatesTaskFieldsAndStatusAndPriorityWithoutChangingCreatorOrProject`; `persistsTaskAcrossSeparateHttpRequests` checks persisted status and priority. | Pass |
| 06 — Assignments | Users can be assigned/unassigned; assigned users are not eligible for another assignment; duplicate assignments are rejected. | Assignment and removal worked; assigned users disappeared from the eligible list and could not be selected again. Only two demo users were available, so eligibility with a third user was not checked. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/assignments/AssignmentControllerTest.java` — `anyAuthenticatedUserCanAssignAnExistingUser`, `duplicateAssignmentReturnsConflict`, `duplicateAssignmentRejectedByDatabaseReturnsConflict`, `removalReturnsNoContentForExistingAssignment`; PostgreSQL reference: `PostgresAssignmentCommentPersistenceTest.persistsAssignmentsAcrossSeparateHttpRequests`. | Partial |
| 07 — Comments | Users can add comments; other users can see them with the correct author and date/time. | One user added a comment visible to another, with author and date/time displayed correctly. The second user also added a comment with the correct author. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/comments/CommentControllerTest.java` — `createsCommentWithServerDerivedAuthorAndTimestampAndCanListItAgain`, `listsCommentsForExistingTaskWithAuthorAndTimestamp`; PostgreSQL reference: `PostgresAssignmentCommentPersistenceTest.persistsCommentAcrossSeparateHttpRequests`. | Pass |
| 08 — API authorization | Project ownership and task deletion permissions are enforced for direct API requests. | Alex and Sam used separate authenticated PowerShell web sessions and could both see Alex's project. Sam's project-update request received HTTP 403; Alex confirmed the name was unchanged. Alex created `API Authorization Probe`; Sam's task-delete request received HTTP 403; Alex confirmed the task still existed with its original title, description, status and priority. The temporary task `API Authorization Probe` was created for this authorization check. Its cleanup was not confirmed and should be treated as pending. | Direct API check (user-reported; raw requests/responses were not retained). Automated references: `ProjectControllerTest.nonOwnerCannotUpdateOrDeleteProject`; `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `anotherAuthenticatedUserCannotDeleteTask`, `creatorCanDeleteTask`. | Pass |
| 09 — Deletion rules and error handling | A project containing tasks cannot be deleted; failed actions show errors rather than success; invalid task input is rejected without losing form data. | Deleting a project with tasks was rejected, the project remained available, and an error rather than a success message was shown. An invalid task title produced an error; no success message appeared and the form retained the entered information. | Manual browser check (user-reported). Automated references: `backend/src/test/java/com/matriz/projects/ProjectControllerTest.java` — `ownerCannotDeleteProjectWhileTasksRemain`; `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `rejectsShortBlankAndWhitespaceOnlyTitles`, `deletingMissingTaskReturnsNotFound`. | Pass |

## Automated backend test execution

The commands below are run from the `backend` directory. Each result is labeled as a prior report or a current run for this correction.

### PostgreSQL integration tests

```powershell
mvn "-Dtest=PostgresAssignmentCommentPersistenceTest" test
```

**Previously reported result:** 3 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. This was a prior run with test credentials configured; it was not repeated during the current correction because those credentials are unavailable here.

The class is `backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java`. It contains:

- `persistsAssignmentsAcrossSeparateHttpRequests`
- `persistsCommentAcrossSeparateHttpRequests`
- `persistsTaskAcrossSeparateHttpRequests`

The tests use the real Spring application and PostgreSQL database `matriz_test`, with unique test records, and verify persisted results through separate HTTP requests. The task test checks ID, title, description, status, priority and project association from the GET response, and verifies the persisted creator ID against the authenticated user. The task API response does not itself expose `creatorId`.

### Full backend suite

```powershell
mvn test
```

**Current result:** 80 tests run, 0 failures, 0 errors, 3 skipped; `BUILD SUCCESS`. The three skipped tests are the opt-in PostgreSQL persistence tests because `MATRIZ_TEST_DB_USERNAME` and `MATRIZ_TEST_DB_PASSWORD` were not configured for this run.

### Project API validation tests

```powershell
mvn "-Dtest=ProjectControllerTest" test
```

**Current result:** 12 tests run, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`. This includes the new API tests for empty and whitespace-only project names on creation and update.

Representative automated coverage:

| Coverage | Test references | What is verified |
|---|---|---|
| Validation | `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `rejectsShortBlankAndWhitespaceOnlyTitles`; `backend/src/test/java/com/matriz/projects/ProjectControllerTest.java` — `rejectsProjectNameOutsideDocumentedLength`; `backend/src/test/java/com/matriz/comments/CommentControllerTest.java` — `rejectsEmptyWhitespaceOnlyAndOverLimitCommentContent` | Invalid API input receives validation errors; relevant tests assert repository interactions or response status. |
| Authorization | `backend/src/test/java/com/matriz/projects/ProjectControllerTest.java` — `nonOwnerCannotUpdateOrDeleteProject`; `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` — `anotherAuthenticatedUserCannotDeleteTask` | MockMvc requests by non-owners are forbidden; repository-backed side effects are not performed. These controller tests use mocked repositories. |
| Create followed by persistence query | `backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java` — `persistsTaskAcrossSeparateHttpRequests` | Creates a task through HTTP, retrieves it with a separate HTTP request, and verifies persisted values against request and authenticated identity. Uses real PostgreSQL persistence. |
| Authentication/session/CSRF | `backend/src/test/java/com/matriz/security/AuthenticationControllerTest.java`; `backend/src/test/java/com/matriz/security/SecurityConfigurationTest.java`; `backend/src/test/java/com/matriz/security/MatrizUserDetailsServiceTest.java` | Login/profile/logout, protected routes, CSRF/session behavior, BCrypt hashes and local demo-account restrictions. |
| Task behavior | `backend/src/test/java/com/matriz/tasks/TaskControllerTest.java` | Task creation/defaults, listing/filtering/pagination, retrieval, update, validation and deletion authorization. Repository behavior is mocked in this class. |
| Assignments and comments | `backend/src/test/java/com/matriz/assignments/AssignmentControllerTest.java`; `backend/src/test/java/com/matriz/comments/CommentControllerTest.java` | API validation, authorization requirements, response fields, duplicate handling, author derivation, timestamps, and missing-resource responses. Most repository interactions are mocked. |
| Database safeguards | `backend/src/test/java/com/matriz/users/LocalDatabaseUrlGuardTest.java`; `backend/src/test/java/com/matriz/persistence/PostgresAssignmentCommentPersistenceTest.java` | Datasource URL safety checks and the fixed integration-test database target. |

Surefire reports are available under `backend/target/surefire-reports/`. Current reports confirm the 80-test full-suite result with three skipped integration tests and the 12-test project controller result. The earlier successful PostgreSQL integration-test run is recorded above; it was not repeated for this correction because the required test credentials were unavailable. No database was accessed during the current verification.

The revised `openapi.yaml` parsed successfully with the locally available SnakeYAML parser. Structural checks confirmed that `/dashboard` is absent, `Task` does not declare `assignees`, and the reusable `401` response has no body.

## AI-assisted development examples

These examples describe AI assistance and developer decisions; they do not replace the verification evidence above.

| Objective | Developer decision | Verification |
|---|---|---|
| Refine the requirements record without treating an early AI proposal as approved scope. | The developer rejected premature role and feature assumptions and retained only the three needs confirmed by the client: organising projects, managing tasks and following work progress. Detailed behavior remains identified as project decisions or unresolved questions. | Compared `AGENTS.md`, `docs/requirements.md`, `docs/constraints.md` and `docs/user-stories.md`; the documents preserve the distinction, and `docs/constraints.md` records that AI recommendations were assessed rather than adopted automatically. |
| Add real-PostgreSQL evidence that a task created through the API is available through a separate request. | The developer required the existing integration-test infrastructure, a fixed `matriz_test` URL and database-name assertion; the test uses unique data and does not target `matriz_db`. | `PostgresAssignmentCommentPersistenceTest.persistsTaskAcrossSeparateHttpRequests` performs separate POST and GET requests and checks the database guard. A prior targeted run with configured credentials was reported as passing; the current run skipped these tests because credentials were unavailable. This does not establish persistence across a backend restart. |
| Record direct API authorization evidence accurately. | The developer performed the checks with separate Alex and Sam sessions, required the observed 403 responses and unchanged resource state to be recorded, and instructed that unconfirmed cleanup not be claimed. | Scenario 08 records the user-reported HTTP 403 responses and state checks separately from automated controller-test references; cleanup remains explicitly unconfirmed. Raw request/response transcripts were not retained. |

## Limitations and unverified areas

- Manual checks were confirmed by the user, but no screenshots or detailed browser logs were retained.
- Direct API authorization results were reported, but raw PowerShell requests/responses were not retained.
- The frontend has no automated test suite or test runner at this stage; frontend behavior was checked manually.
- PostgreSQL integration tests establish persistence across separate HTTP requests, not across a backend restart. Restart persistence was checked manually in Scenario 03.
- Only two demo users were available for assignment checks; eligibility with a third user was not tested.
- Cleanup of `API Authorization Probe` is unconfirmed because no successful delete response or independent absence check was supplied. Do not treat cleanup as verified.
- Mocked repository tests do not prove real database persistence. PostgreSQL evidence is limited to the dedicated persistence-test class.
- This record makes no claims about scenarios or edge cases beyond the manual results and repository-backed tests listed above.

## Overall conclusion

The current full backend suite passed with 80 tests, 0 failures, 0 errors and 3 skipped opt-in PostgreSQL tests; the project controller tests passed with all 12 tests. A previous targeted PostgreSQL run was reported as passing, but was not repeated because credentials were unavailable in the current environment. User-reported manual checks cover all nine scenarios, including application flows, persistence through a backend restart, user-facing validation and direct API authorization. Evidence quality is limited by the absence of screenshots and detailed request logs; frontend behavior has no automated tests, third-user assignment eligibility was not checked, and temporary-task cleanup remains unconfirmed.
