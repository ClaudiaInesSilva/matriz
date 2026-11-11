# Matriz — Implementation Plan (Lab 03)

## 1. Purpose and Baseline

This plan sequences implementation of the Lab 03 course scope documented in `docs/requirements.md`. The original client brief confirms only the high-level needs to organise projects, manage tasks and follow work progress. The detailed Lab 03 functionality and the working technical choices below are not additional client-confirmed requirements.

The working architecture is a Modular Monolith with a Java/Spring Boot/Maven backend, React/TypeScript frontend using Vite and npm, PostgreSQL with Spring Data JPA, and Spring Security with server-side sessions. Backend tests use JUnit 5 and Spring Boot Test. These are proposed project decisions for the course. Versions, deployment configuration and some test tooling remain undecided.

The plan covers authentication; project and task CRUD; task status (`TODO`, `IN_PROGRESS`, `DONE`, default `TODO`) and priority (`LOW`, `MEDIUM`, `HIGH`, default `MEDIUM`); multiple assignees; task comments; project-name search; task status/priority filters; and the dashboard with all projects and status-grouped task counts.

## 2. Decisions and Documentation Issues to Track

The following details remain open and must stay visible during planning. They do not block work that can proceed without them, and they are not client-confirmed requirements:

- **Account representation:** Confirm the login identifier, required account attributes and seed-account handling before finalizing the User persistence model and authentication setup.
- **Project/task fields:** No additional fields beyond project name/description and task title/description/status/priority are specified. Do not add fields without a decision.
- **Technical setup:** Confirm dependency/runtime versions, database migration approach, environment variables and deployment arrangements.
- **Session/security details:** Select concrete Spring Security session-cookie and CSRF configuration for the target environment.
- **Frontend verification:** No frontend test tool is documented; decide whether one is needed before adding a dependency.
- **Non-functional targets:** Performance, availability, backup, recovery and monitoring targets remain unspecified.

## 3. Phased Implementation

### Phase 1 — Project Setup and Development Environment

**Objective:** Establish the proposed development structure and local build/run workflow.

**Main tasks:**

- Confirm required Java, Spring Boot, Node.js and package versions against course/environment constraints.
- Set up the backend Maven project and install and initially configure the frontend React/TypeScript/Vite application with npm as parts of one project, keeping the backend as one deployable application.
- Record local configuration needs and ensure secrets are supplied outside versioned documentation/source.
- Establish a minimal build and startup check for each application part.

**Relevant files/modules:** `docs/architecture.md`; backend and frontend project files to be created during implementation (paths not yet established).

**Dependencies:** None.

**Deliverables:** Buildable backend and frontend skeletons; documented local setup and configuration approach.

**Completion verification:** Backend Maven build and frontend npm build/start checks succeed in the intended environment; no application features are assumed complete.

### Phase 2 — Database Configuration, Schema and Persistence

**Objective:** Configure PostgreSQL persistence and implement the documented conceptual entities and relationships.

**Main tasks:**

- Configure the application connection without committing credentials.
- Implement `User`, `Project`, `Task`, `TaskAssignment` and `Comment` persistence based on `docs/database.md`.
- Represent one project per task, many-to-many task assignments with a unique task/user pair, and task comments with one author and creation date.
- Apply project name (3–150 characters) and optional description (maximum 2,000); task title (3–150), optional description (maximum 2,000), status and priority values/defaults; apply comment length and non-whitespace validation consistently at the application boundary as well as any suitable persistence constraints.
- Choose and document a schema migration approach before schema changes accumulate.
- Keep dashboard counts derived from task records; do not create a separate persisted count entity.

**Relevant files/modules:** `docs/database.md`; persistence/configuration and migration files to be established.

**Dependencies:** Phase 1; choose a schema/migration approach. Implement the approved project/task fields and rules from `docs/database.md`.

**Deliverables:** Database configuration, schema/migrations, persistence mappings and focused persistence/constraint checks.

**Completion verification:** Application connects to PostgreSQL; relationship and uniqueness checks behave as intended; validation boundary tests cover stated limits and defaults.

### Phase 3 — Authentication and Current-User Handling

**Objective:** Establish server-side session authentication and a trusted current-user identity for protected operations.

**Main tasks:**

- Configure Spring Security for the proposed session-based approach and align cookie/CSRF settings with the chosen environment.
- Implement login, logout and current-user behavior matching `/auth/login`, `/auth/logout` and `/auth/me` in `openapi.yaml`.
- Use seeded demo accounts if needed for the documented course workflow; do not add public registration or password recovery to the required scope.
- Ensure ownership and comment authorship derive from the authenticated user rather than client-supplied identity fields.
- Establish authentication tests for valid/invalid credentials, session invalidation, current identity and unauthenticated protected requests.

**Relevant files/modules:** Authentication and security modules; `openapi.yaml` Authentication paths; `database.md` User entity.

**Dependencies:** Phases 1–2, including a usable User persistence model and the account identifier decision.

**Deliverables:** Working session authentication, current-user endpoint and authentication test coverage.

**Completion verification:** API checks confirm session establishment, current-user identity, logout invalidation and rejection of unauthenticated protected calls without exposing sensitive details.

### Phase 4 — Backend Modules and REST API

**Objective:** Implement the project/task domain operations and their dependent assignments and comments through the documented REST contract.

**Main tasks:**

- Implement project create/list/view/edit/delete operations and task create/list/view/edit/delete operations.
- Apply approved authorization checks: all authenticated users can list/view/create projects; only the owner edits/deletes a project; all authenticated users create/view/update tasks; only the owner or task creator deletes a task; all authenticated users view/add comments.
- Enforce task status and priority values/defaults and the task validation limits.
- Implement the eligible-user listing operation (`GET /projects/{projectId}/eligible-assignees`) to return existing accounts. Any existing account is eligible regardless of project ownership.
- Implement assignment listing and add/remove operations for any authenticated user; duplicate task/user pairs are prohibited.
- Implement add/list comments, deriving the author and timestamp on the server and enforcing length/non-whitespace validation.
- Match request/response fields and HTTP outcomes to `openapi.yaml`; update that contract only if an approved implementation decision requires a documented correction.
- Add focused service/API tests for valid operations, invalid input, missing/inaccessible resources and denied operations.

**Relevant files/modules:** Project Management, Task Management, Assignments, Comments, shared validation/access protection; endpoints in `openapi.yaml`.

**Dependencies:** Phases 2–3. Approved permission, eligibility and dependent-record rules are documented.

**Deliverables:** Backend operations for projects, tasks, assignments and comments with API-level tests.

**Completion verification:** Each operation has successful and relevant failure-path checks; unauthenticated requests are rejected and owner/creator restrictions are enforced only for the specified project/task mutations.

### Phase 5 — Frontend Setup and Application Structure

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

### Phase 6 — Frontend Integration and User Workflows

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

**Deliverables:** Integrated core user workflows for all required project/task operations, assignments and comments.

**Completion verification:** Walk through user stories US01–US04 and US06–US08 using authorized and unauthorized scenarios and confirm the interface reflects persisted API results.

### Phase 7 — Dashboard, Search and Filtering

**Objective:** Deliver the Lab 03 overview and discovery features for the authenticated user across all projects.

**Main tasks:**

- Implement project-name search using trimmed, case-insensitive partial matching; empty or omitted query returns all projects.
- Implement task filters by status and priority; when both are supplied, apply AND behavior, and let omitted filters leave results unrestricted.
- Implement dashboard data and UI for all projects and counts across their tasks for TODO, IN_PROGRESS and DONE, including zero counts. Count each task exactly once.
- Counts include every priority and assignee and are independent of both; do not calculate percentages or add charts/advanced analytics.

**Relevant files/modules:** Project Management search, Task Management filters, Dashboard module/API, frontend dashboard and list controls; `openapi.yaml` search/filter/dashboard schemas.

**Dependencies:** Phases 4–6. Search, filter, access and count rules are already documented.

**Deliverables:** Searchable project list, filterable task list and dashboard with all projects and status counts.

**Completion verification:** Check matching/non-matching search, each filter, combined-filter AND results, all-project visibility, and dashboard totals including empty/zero states.

### Phase 8 — Testing, Validation and Final Verification

**Objective:** Verify that the integrated implementation meets the approved Lab 03 baseline and records any remaining limitations.

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

- This plan follows the Lab 03 course implementation target and does not expand it. The original client-confirmed brief remains the three high-level needs described in `README.md` and `docs/requirements.md`.
- Public registration, password recovery, notifications, reporting, integrations and workflow automation are not included in this plan.
- The existing wireframe is an earlier illustrative project-detail view, not the complete Lab 03 dashboard specification.
- Implement the approved business rules as Lab 03 project decisions, not as client-confirmed requirements.
- Keep account representation visible as an open decision; do not block unrelated implementation phases on it.
