# Matriz — Implementation Plan

## 1. Purpose and Baseline

This plan sequences implementation of the project scope documented in `docs/requirements.md`. The original client brief confirms only the high-level needs to organise projects, manage tasks and follow work progress. The detailed functionality and the working technical choices below are not additional client-confirmed requirements.

The selected architecture is a Modular Monolith with a Java 21/Spring Boot 4.1.2/Maven backend, Spring Security 7.1.2, React 19.3.1/TypeScript 7.0.2 frontend using Vite declared as `^8.3.6` (lockfile resolves 8.3.8) and npm 11, PostgreSQL 18.7 with Spring Data JPA, and Flyway 13.12.1. The authentication/session foundation, technical setup, database persistence and application workflows are implemented. Deployment and some test tooling remain undecided.

The plan covers authentication; project and task CRUD; task status (`TODO`, `IN_PROGRESS`, `DONE`, default `TODO`) and priority (`LOW`, `MEDIUM`, `HIGH`, default `MEDIUM`); multiple assignees; task comments; project-name search; task status/priority filters; and the dashboard with all projects and status-grouped task counts.

## 2. Decisions and Documentation Issues to Track

The following details remain open and must stay visible during planning. They do not block work that can proceed without them, and they are not client-confirmed requirements:

- **Account representation:** `username` is the selected login identifier and the two demo accounts have explicit local-only provisioning. Registration, recovery and the wider account lifecycle remain outside the current implementation.
- **Project/task fields:** No additional fields beyond project name/description and task title/description/status/priority are specified. Do not add fields without a decision.
- **Technical setup:** Runtime versions, migration strategy and environment variable names are selected. Hosting and deployment arrangements remain open.
- **Session/security details:** Spring Security 7.1.2, session and CSRF settings, and secure-cookie defaults are selected and implemented. Account lifecycle, hosting/deployment and non-functional targets remain open.
- **Frontend verification:** No frontend test tool is documented; decide whether one is needed before adding a dependency.
- **Non-functional targets:** Performance, availability, backup, recovery and monitoring targets remain unspecified.

## 3. Phased Implementation

### Phase 1 — Project Setup and Development Environment

**Objective:** Establish the proposed development structure and local build/run workflow.

**Main tasks:**

- Selected versions are Java 21, Spring Boot 4.1.2, PostgreSQL 18.7, Flyway 13.12.1, React 19.3.1, TypeScript 7.0.2, Vite declared as `^8.3.6` (lockfile resolves 8.3.8), Node.js 24 and npm 11.
- Backend Maven project and frontend React/TypeScript/Vite scaffold are in place as parts of one project, with a single backend deployment.
- Record local configuration needs and ensure secrets are supplied outside versioned documentation/source.
- Backend startup, database connection, Flyway migration and schema validation, plus frontend development-server and production-build checks, have been completed.

**Relevant files/modules:** `docs/architecture.md`; `backend/` and `frontend/`.

**Dependencies:** None.

**Deliverables:** Completed foundation: backend and frontend scaffolds and documented local setup/configuration.

**Completion verification:** Backend startup and database connection, Flyway migrations and Hibernate schema validation were verified. Frontend development server and production build were verified. No business workflows are implied complete.

### Phase 2 — Database Configuration, Schema and Persistence

**Objective:** Configure PostgreSQL persistence and implement the documented conceptual entities and relationships. **Status: completed for schema and startup foundation.**

**Main tasks:**

- Configured the application connection using environment variables without committing credentials.
- Implemented JPA mappings for `User`, `Project`, `Task`, `TaskAssignment` and `Comment` based on `docs/database.md`.
- The schema represents one project per task, many-to-many task assignments with a unique task/user pair, and task comments with one author and creation date.
- V1 applies project/task field lengths and status/priority constraints and defaults, comment length/non-whitespace constraints, foreign keys, delete behavior and indexes. Request validation is implemented at the API boundary.
- Flyway applies the core schema and demonstration data through V1 and V2; V3 disables seeded demo credentials until local provisioning.
- The conceptual model does not require a separately persisted dashboard count entity.

**Relevant files/modules:** `docs/database.md`; `backend/src/main/resources/application.properties`, `backend/src/main/resources/db/migration/` and persistence packages under `backend/src/main/java/com/matriz/`.

**Dependencies:** Phase 1. The schema/migration approach and persistence mappings are implemented.

**Deliverables:** Database configuration, V1–V3 migrations, persistence mappings and dedicated PostgreSQL persistence tests.

**Completion verification:** PostgreSQL connection, Flyway migrations and Hibernate schema validation have been verified. The dedicated persistence tests target the separate `matriz_test` database; their execution depends on locally configured test credentials.

### Phase 3 — Authentication and Current-User Handling — Completed

**Objective:** Establish server-side session authentication and a trusted current-user identity for protected operations.

**Status:** Completed. Backend login, logout, current-user identity, session handling, CSRF, password hashing and protected-by-default access are implemented and covered by security tests. Resource-specific identity and authorization are implemented in the project, task and comment workflows.

**Main tasks:**

- The `com.matriz.security` module implements login and current-user behavior matching `/auth/login` and `/auth/me`, and configures the Spring Security logout filter for `POST /auth/logout` with a `204` success response and `401` for unauthenticated requests.
- Demo records have no usable credentials unless provisioned locally through the separate local-only process. Registration and password recovery remain outside the implemented foundation.
- The authentication principal supplies the identity used for project ownership, task creation and comment authorship.
- Authentication tests cover valid/invalid credentials, session invalidation, current identity, CSRF behavior and unauthenticated protected requests.
- `demo.alex` and `demo.sam` are reserved demo usernames; tests cover disabled-demo restrictions and BCrypt credentials.
- Logout tests exercise the configured security filter chain, unauthenticated 401 behavior and obtaining and using a fresh CSRF token after logout.

**Relevant files/modules:** Authentication and security modules; `openapi.yaml` Authentication paths; `database.md` User entity.

**Dependencies:** Phases 1–2, including the User persistence model and selected `username` login identifier.

**Deliverables:** Working session authentication, current-user endpoint and authentication test coverage.

**Completion verification:** API checks confirm session establishment, current-user identity, logout invalidation and rejection of unauthenticated protected calls without exposing sensitive details.

### Phase 4 — Backend Modules and REST API — Completed

**Objective:** Implement the project/task domain operations and their dependent assignments and comments through the documented REST contract.

**Status:** Project and task APIs, assignments and comments are implemented. Project ownership and task creator/owner deletion authorization are enforced; validation, filtering, pagination and dependent-record handling have backend test coverage.

**Main tasks:**

- Project and task create/list/view/update/delete operations are implemented with the documented authorization and validation rules.
- Task status and priority values/defaults and field limits are enforced.
- `GET /projects/{projectId}/eligible-assignees` lists existing accounts; any existing account is eligible regardless of project ownership.
- Assignment listing, add and removal operations are implemented for authenticated users; duplicate task/user pairs are prohibited.
- Comment creation and listing derive author and timestamp on the server and enforce length/non-whitespace validation.
- The OpenAPI contract documents the implemented API operations and response behavior.
- Focused service/API tests cover valid operations, invalid input, missing resources and denied operations; PostgreSQL persistence tests cover writes followed by independent HTTP queries.

**Relevant files/modules:** Project Management, Task Management, Assignments, Comments, shared validation/access protection; endpoints in `openapi.yaml`.

**Dependencies:** Phases 2–3. Approved permission, eligibility and dependent-record rules are documented.

**Deliverables:** Project, task, assignment and comment APIs with focused tests are complete.

**Completion verification:** Each operation has successful and relevant failure-path checks; unauthenticated requests are rejected and owner/creator restrictions are enforced only for the specified project/task mutations.

### Phase 5 — Frontend Setup and Application Structure — Completed

**Objective:** Create the frontend shell and shared interaction structure for authenticated project work.

**Main tasks:**

- Establish the React/TypeScript application structure and API communication configuration using the frontend tooling initialized in Phase 1.
- Add shared handling for session state, current-user identity, loading, empty and error responses.
- Establish navigation for the dashboard, project detail and task workflows; keep the existing wireframe marked as an earlier illustration rather than treating it as the complete dashboard specification.
- Select a front-end test approach only if required/approved; no front-end test dependency is specified in the current stack documentation.

**Relevant files/modules:** Frontend application structure; `docs/ui/dashboard-wireframe.html` as a limited project-detail reference; `openapi.yaml`.

**Dependencies:** Phase 1. This phase may proceed alongside Phases 2–4 using the published API contract, with end-to-end integration deferred to Phase 6.

**Deliverables:** Frontend shell, API client boundary and basic navigation/session handling.

**Completion verification:** Frontend builds and renders the shell; unauthenticated and authenticated navigation states can be exercised against the API or a controlled development environment.

### Phase 6 — Frontend Integration and User Workflows — Completed

**Objective:** Connect the interface to the project, task, assignment and comment API workflows.

**Main tasks:**

- Implement login/logout/current-user flows.
- Implement project list, detail, create, edit and delete interactions.
- Implement project task list and task create, view, edit and delete interactions, including status and priority choices.
- Implement eligible-user selection when assigning multiple users to a task, plus assignment removal; any existing account may be selected regardless of project ownership.
- Implement comment add/list interactions.
- Present validation and authorization failures clearly without exposing protected data.
- Ensure destructive actions follow the decided dependent-record rules.

**Relevant files/modules:** Frontend auth, project, task, assignment and comment views/components; corresponding API paths.

**Dependencies:** Phases 3–5.

**Deliverables:** Integrated authentication, project/task workflows, assignments and comments.

**Completion verification:** Walk through user stories US01–US04 and US06–US08 using authorized and unauthorized scenarios and confirm the interface reflects persisted API results.

### Phase 7 — Dashboard, Search and Filtering — Completed

**Objective:** Deliver the project overview and discovery features for the authenticated user across all projects.

**Main tasks:**

- Project-name search uses trimmed, case-insensitive partial matching; empty or omitted query returns all projects.
- Task filters by status and priority apply AND behavior when both are supplied; omitted filters leave results unrestricted.
- The frontend dashboard obtains project/task data from existing APIs and computes counts across all projects for TODO, IN_PROGRESS and DONE, including zero counts. It counts each task once.
- Counts include every priority and assignee and are independent of both; do not calculate percentages or add charts/advanced analytics.

**Relevant files/modules:** Project Management search, Task Management filters, frontend dashboard and list controls; `openapi.yaml` search/filter schemas. No separate dashboard endpoint is exposed.

**Dependencies:** Phases 4–6. Search, filter, access and count rules are already documented.

**Deliverables:** Searchable project list, filterable task list and frontend dashboard with all projects and status counts.

**Completion verification:** Check matching/non-matching search, each filter, combined-filter AND results, all-project visibility, and dashboard totals including empty/zero states.

### Phase 8 — Testing, Validation and Final Verification

**Objective:** Verify that the integrated implementation meets the documented project baseline and records any remaining limitations.

**Status:** The current full backend suite passed with 80 tests, 0 failures, 0 errors and 3 opt-in PostgreSQL tests skipped because credentials were unavailable. The project controller tests passed with 12 tests; frontend lint and production build passed. The OpenAPI document parsed successfully. Manual scenario evidence is user-reported. A prior PostgreSQL integration run was reported as passing, but the tests must be rerun with the dedicated test database credentials for current execution evidence.

**Main tasks:**

- Run backend unit/integration tests using JUnit 5 and Spring Boot Test, including persistence, validation, authentication and authorization.
- Run build checks for backend and frontend and the selected frontend checks, if a tool is approved.
- Exercise the API contract for all documented operations, responses and session behavior.
- Run end-to-end workflows for authentication, project/task CRUD, assignments, comments, search, filters and dashboard counts.
- Verify project-name and task-title boundaries at 3 and 150 characters; project and task description maximum length of 2,000 characters; and comment lengths of 1 and 2,000 characters, including rejection of whitespace-only comments.
- Verify duplicate task/user assignment rejection, default task status/priority, and empty or inaccessible result sets.
- Verify that deleting a project containing tasks returns HTTP `409 Conflict`.
- Verify that deleting a task also deletes its associated assignments and comments.
- Review access-control behavior against the explicit decisions, then record unresolved issues rather than silently broadening scope.

**Relevant files/modules:** Backend and frontend test suites, build configuration, `openapi.yaml`, and the acceptance criteria in `docs/user-stories.md`.

**Dependencies:** Phases 1–7 and any remaining decision required by the behavior under test.

**Deliverables:** Passing available build/test checks, completed acceptance-criteria review and a concise list of known limitations/open decisions.

**Completion verification:** All required checks have recorded results; failures are fixed or documented, and no feature is claimed complete solely because its UI or endpoint exists.

## 4. Dependency Summary

Project setup precedes backend and frontend work. Database/User persistence precedes session authentication; authentication precedes protected domain operations. Project/task persistence and authorization precede assignments, comments, search, filters and dashboard calculations. Frontend scaffolding can proceed in parallel after setup, while integrated workflows depend on the corresponding API behavior. Testing is included within each implementation phase and repeated across the integrated application in Phase 8.

## 5. Scope and Decision Notes

- This plan follows the documented project implementation target and does not expand it. The original client-confirmed brief remains the three high-level needs described in `README.md` and `docs/requirements.md`.
- Public registration, password recovery, notifications, reporting, integrations and workflow automation are not included in this plan.
- The existing wireframe is an earlier illustrative project-detail view, not the complete dashboard specification.
- Implement the approved business rules as project decisions, not as client-confirmed requirements.
- Keep account representation visible as an open decision; do not block unrelated implementation phases on it.
