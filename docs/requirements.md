
# Matriz — Requirements Specification

## 1. Application Vision

Matriz is a project and task management application intended for work teams within a company. Its purpose is to help teams organise projects, manage tasks and track work progress in a centralised application.

The initial client description establishes these three needs but does not define all the detailed functionalities or business rules. This document therefore distinguishes confirmed needs from proposed requirements and assumptions.

**Confirmed by the client brief:** teams need an application to organise projects, manage tasks and follow work progress. All detailed functionality below is a proposal for the course exercise, not a confirmed client commitment.

## 2. Objectives

The main objectives of Matriz are to:

- Enable teams to organise their work into projects.
- Allow users to create, view and edit tasks associated with projects.
- Allow users to update and follow task status.
- Support following work progress; its representation is not yet defined.
- Establish a clear functional foundation for future development.

## 3. User Types

### 3.1 Team Member (provisional persona)

A team member participates in projects and performs tasks.

Proposed needs:
- View all projects and their tasks under the Lab 03 course implementation decisions.
- Create and edit tasks under the Lab 03 course implementation decisions.
- Update task status.
- Follow project progress.

### 3.2 Project Manager (provisional persona)

A project manager coordinates project activities and monitors progress.

Proposed needs:
- Create and organise projects.
- Manage tasks associated with projects.
- Monitor task status.
- View project progress.

**Note:** These are provisional user types. The client has not confirmed whether they must be separate roles or whether a user may perform both sets of responsibilities.

## 4. Functional Requirements

The original client brief establishes the need to organise projects, manage tasks and follow work progress. The requirements below incorporate the minimum functional scope specified in Lab 03 for implementation.

The Lab 03 scope extends the earlier proposed MVP. Its additional functionality is included to satisfy the practical exercise and should not be interpreted as evidence that the client independently requested or validated every detail.

| ID | Requirement | Priority | Source / Status |
|---|---|---|---|
| FR01 | Any authenticated user shall be able to create a project; its creator is its owner. | Essential | Lab 03 course implementation decision |
| FR02 | Any authenticated user shall be able to list and view all projects. | Essential | Lab 03 course implementation decision |
| FR03 | Only a project's owner shall be able to edit or delete it; deletion is rejected with HTTP 409 while the project contains tasks. | Essential | Lab 03 course implementation decision |
| FR04 | Any authenticated user shall be able to create, view and update tasks in any project. Only the project owner or task creator may delete a task. | Essential | Lab 03 course implementation decision |
| FR05 | Each task shall belong to a valid project and have a status and priority. Status values are `TODO`, `IN_PROGRESS`, `DONE` (default `TODO`); priority values are `LOW`, `MEDIUM`, `HIGH` (default `MEDIUM`). | Essential | Lab 03 course implementation decision |
| FR06 | Any authenticated user may add or remove task assignments. Any existing user account may be assigned; multiple assignments are allowed, duplicate task/user assignments are prohibited, and project ownership is not required for eligibility. | Essential | Lab 03 course implementation decision |
| FR07 | Any authenticated user shall be able to view and add task comments; each comment records its authenticated author and creation date. | Essential | Lab 03 course implementation decision |
| FR08 | The application shall allow users to log in, log out and retrieve the identity of the currently authenticated user. | Essential | Lab 03 minimum scope |
| FR09 | The application shall allow authenticated users to search all projects by name, using case-insensitive partial matching after trimming query whitespace; an empty or omitted query returns all projects. | Essential | Lab 03 course implementation decision |
| FR10 | The application shall allow tasks to be filtered by status and priority. Omitted filters do not restrict results; when both are supplied, both conditions must match. | Essential | Lab 03 course implementation decision |
| FR11 | The dashboard shall display all projects and counts across their tasks for each of `TODO`, `IN_PROGRESS` and `DONE`, including zero counts. Counts include all priorities and assignees. | Essential | Lab 03 course implementation decision |

Public registration and password recovery are optional extensions and are not part of the required implementation scope. Demo accounts may be created using a database seed.

### 4.1 Functional Behaviour and Validation

- Any authenticated user may list/view all projects and create projects. The backend records the creator as project owner; only that owner may edit or delete the project.
- Any authenticated user may create, view and update tasks in any project. The backend records the task creator. Only the project owner or task creator may delete a task.
- Any authenticated user may view and add task comments; the backend records the authenticated user as author and sets the creation date.
- Any authenticated user may add or remove assignments. Any existing user account may be assigned regardless of project ownership. Prevent duplicate assignments.
- Tasks must belong to valid projects. Project deletion must return HTTP `409 Conflict` when any tasks remain. Task deletion also removes its assignments and comments.
- Project names are required and contain 3–150 characters; project descriptions are optional and at most 2,000 characters.
- Task titles are required and contain 3–150 characters; task descriptions are optional and at most 2,000 characters. Status and priority use the values and defaults in FR05.
- Comment content contains 1–2,000 characters and must not consist only of whitespace.
- Project search trims leading/trailing query whitespace, matches partial names without case sensitivity, and returns all projects for an empty or omitted query.
- Task filters may be used separately or together. When both are supplied, results satisfy both; omitted filters do not restrict results.
- Dashboard counts are derived from tasks across all projects and include all statuses, including zero, regardless of task priority or assignee. Do not calculate percentages or add charts/advanced analytics.
- Invalid input and unauthorised operations must be handled without creating inconsistent records or exposing protected data.

These are Lab 03 course implementation decisions, not requirements independently confirmed by the original client. Comment editing/deletion is outside the required scope.

## 5. Non-Functional Requirements

The client has not provided detailed non-functional requirements. The following are initial quality and security objectives to be validated.

| ID | Requirement | Priority | Status |
|---|---|---|---|
| NFR01 | The application should be easy to understand and use for its intended users. | High | Proposed |
| NFR02 | The application should validate user input and handle invalid data without creating inconsistent records. | High | Proposed |
| NFR03 | The application should protect project and task information from unauthorised access. | High | Proposed |
| NFR04 | The application should protect confidential information and avoid exposing credentials or sensitive data. | High | Proposed |
| NFR05 | The application should provide clear feedback when an operation succeeds or fails. | High | Proposed |
| NFR06 | The application should maintain consistent relationships between projects and tasks. | High | Proposed |

Specific performance targets, availability requirements, accessibility standards, backup policies and legal or regulatory obligations have not yet been defined.

## 6. MVP Scope

The original proposed MVP focused on basic project management, task management, task status updates and progress visibility. Lab 03 expands this scope and defines the minimum functionality to be implemented in the practical exercise.

The scope below is the **Lab 03 implementation target**. It does not imply that every feature was independently requested or validated by the original client.

### 6.1 Required Implementation Scope

- **Authentication:** Log in, log out and retrieve the currently authenticated user.
- **Project management:** All authenticated users can create, list and view projects. The creator owns the project; only its owner can edit/delete it. Deletion returns `409 Conflict` while tasks remain.
- **Task management:** Any authenticated user can create, view and update tasks in any project. Only the project owner or task creator can delete a task. Deleting a task removes its assignments and comments.
- **Task fields:** Required title (3–150 characters); optional description (maximum 2,000); statuses `TODO`, `IN_PROGRESS`, `DONE` (default `TODO`); priorities `LOW`, `MEDIUM`, `HIGH` (default `MEDIUM`).
- **Task assignments:** Any authenticated user may add or remove assignments. Any existing account may be assigned regardless of project ownership. Multiple assignments are supported; duplicate user/task pairs are prohibited.
- **Comments:** Any authenticated user can add and view task comments. Content must be 1–2,000 characters and not whitespace-only; author and creation date are recorded by the backend.
- **Search:** Case-insensitive partial project-name search after trimming query-edge whitespace; empty or omitted query lists all projects.
- **Filters:** Filter tasks by status and/or priority; if both are supplied, results match both; omitted filters do not restrict results.
- **Dashboard:** Display all projects and task counts across all project tasks for all three statuses, including zero. Counts ignore priority and assignees; no percentages, charts or advanced analytics.

### 6.2 Optional Extensions

The following are optional and are not required for the initial implementation:

- Public user registration.
- Password recovery.

Demo accounts may be created through a database seed to support authentication and access-control testing.

### 6.3 Out of Scope for the Initial Implementation

Unless required by a later exercise or explicitly approved, the following are outside the current implementation scope:

- Notifications and automated reminders.
- Advanced reporting and analytics.
- External integrations with other project management tools.
- Advanced workflow automation.
- Advanced custom dashboards beyond the required dashboard.
- Public registration and password recovery.

Basic authentication, authorisation, input validation and protection of project and task data remain required; they are not optional advanced features.

### 6.4 Scope and Validation Notes

The Lab 03 functional scope takes precedence as the implementation target for this practical exercise. The original client brief remains the source of the application's high-level purpose.

The project implementation decisions above are not client-confirmed requirements. Remaining matters such as account representation and comment editing/deletion must remain explicit; do not infer additional business rules. Selected technical decisions are documented in `docs/architecture.md`.

## 7. Open Questions

Lab 03 defines the minimum functional scope, but some business rules and implementation details remain unspecified. The following questions must be resolved or documented as explicit implementation decisions before the affected functionality is developed.

### 7.1 Project and Task Rules

- Are project or task fields beyond the documented name/description/title/status/priority required for this implementation?

### 7.2 Authentication and Access Control

- How are demo user accounts represented, and which account attributes are required?
- Are comment editing and deletion needed in a later extension? They are not part of the Lab 03 required scope.

### 7.3 Validation and Error Handling

- Are any API validation/error details needed beyond those documented in `openapi.yaml`?

### 7.4 Technical and Testing Details

- The project has selected Java 21, Spring Boot 4.1.2, React 19.3.1, TypeScript 7.0.2, Vite (`^8.3.6`, resolving to 8.3.8), Node.js 24, npm 11, PostgreSQL 18.7 and Flyway 13.12.1. These are project decisions, not client-confirmed requirements.
- Flyway manages versioned database schema changes and applies pending migrations at backend startup.
- The backend reads database connection settings from `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`; provide them through the process environment or IDE run configuration. Spring Boot does not automatically load `.env` files.
- Which deployment and hosting arrangements will be used?
- Which automated tests will verify validation, persistence and access control?

Decisions made to resolve the remaining questions must be documented in the relevant project files. Where the documented project scope does not specify a business rule, record an explicit implementation decision rather than presenting it as client-confirmed.

## 8. Current Status

This document preserves the original client brief and incorporates the minimum functional scope specified in Lab 03 as the implementation target.

The original client-confirmed need remains to organise projects, manage tasks and follow work progress. The expanded functionality in Sections 4 and 6 is included to satisfy the practical exercise and should not be interpreted as independent confirmation of every detail by the client.

The project decisions for permissions, assignment eligibility, dependent-record deletion, project search, task filters, dashboard counts, specified validation rules and the technical foundation are recorded in this document and `docs/architecture.md`. Remaining questions concern account representation, any additional fields, deployment, and future test coverage; these are not client-confirmed requirements.

The selected Modular Monolith architecture and technology stack are documented in `docs/architecture.md`. The database model, API contract, user stories, wireframe and implementation plan should remain consistent with this scope.

No application code has been implemented as part of this requirements update.
