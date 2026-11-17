
# Matriz — User Stories and Acceptance Criteria

## 1. Purpose

This document defines the user stories and acceptance criteria for the current Matriz implementation scope. The stories translate the functional requirements in `docs/requirements.md` into user-focused, testable outcomes.

The original client brief establishes the need to organise projects, manage tasks and follow work progress. The implementation scope extends the initial proposed MVP with authentication, project and task management, multiple task assignees, comments, search, filters and a dashboard.

The expanded scope is a Matriz project implementation target. It should not be interpreted as confirmation that the client independently requested or validated every detailed behaviour.

Project visibility and operation permissions, assignee eligibility, deletion behavior, search and filter semantics, dashboard counts and validation rules are recorded as project decisions in `docs/requirements.md`. These decisions are not requirements independently confirmed by the client. Technical account details remain unspecified.

### Priority Definitions

- **High:** Required functionality within the current implementation scope.
- **Medium:** Optional functionality or an extension not required for the initial implementation.
- **Low:** Functionality deferred from the initial implementation.

The priorities in this document reflect the project scope and implementation planning, not priorities independently assigned by the client.

“Project manager” and “team member” are provisional personas used to describe user goals. They do not establish separate roles or permission groups. Apply the approved project/task and assignment permissions in `docs/requirements.md`.

## 2. User Stories

### US01 — Create a Project

**Priority:** High

**Related requirement:** FR01

**User Story:**

As an authenticated user, I want to create a project, so that I can organise work around a shared objective.

**Acceptance Criteria:**

- **AC01.1 — Successful creation and ownership:** Given any authenticated user and a valid project, when the user submits the project form, then the project is created, the backend records that user as owner, and it appears in the project list.
- **AC01.2 — Project validation:** Given a project name shorter than 3 or longer than 150 characters, or a description longer than 2,000 characters, when the user submits the form, then creation is rejected with a validation message. A description may be omitted.
- **AC01.3 — Project visibility:** Given an authenticated user, when they list or view projects, then all projects are visible regardless of owner.

**Project decision:** Project name is required (3–150 characters); description is optional (maximum 2,000 characters). The creator is the owner.

### US02 — View, Search and Manage Projects

**Priority:** High

**Related requirements:** FR02, FR03, FR09

**User Story:**

As an authenticated user, I want to view and search all projects and manage projects I own, so that I can find relevant work and maintain my projects.

**Acceptance Criteria:**

- **AC02.1 — Available projects:** Given an authenticated user, when they open the project list, then all projects are displayed regardless of owner.
- **AC02.2 — No projects:** Given there are no projects, when an authenticated user opens the project list, then an empty state is shown.
- **AC02.3 — Search by name:** Given an authenticated user searches for a project, then matching is partial and case-insensitive after trimming leading/trailing query whitespace. An empty or omitted query returns all projects.
- **AC02.4 — Edit project:** Given a project owner saves valid changes, then the project is updated. An authenticated non-owner cannot edit it.
- **AC02.5 — Delete project:** Given a project owner deletes a project with no tasks, then it is deleted. A non-owner cannot delete it.
- **AC02.6 — Project with tasks:** Given a project contains one or more tasks, when its owner attempts deletion, then deletion is rejected with HTTP `409 Conflict` and the project remains.
- **AC02.7 — Non-owner operation:** Given an authenticated non-owner attempts to edit or delete a project, then the operation is rejected and no change is made.

**Project decisions:** All authenticated users can view all projects and create projects; only the owner can edit/delete. Search matching and project validation are specified above.

### US03 — Create, View, Edit and Delete Tasks

**Priority:** High

**Related requirements:** FR04, FR05

**User Story:**

As an authenticated user, I want to create, view and update tasks within any project, so that I can organise project work.

**Acceptance Criteria:**

- **AC03.1 — Task creation:** Given any authenticated user and a valid existing project, when the user submits a task with required information, then the task is created in that project and the backend records the task creator.
- **AC03.2 — Task editing:** Given any authenticated user updates a task in any project with valid changes, then the updated task is persisted and displayed.
- **AC03.3 — Task viewing:** Given any authenticated user opens a task in any project, then its details, status and priority are displayed.
- **AC03.4 — Task title and description validation:** Given that a task title contains fewer than 3 or more than 150 characters, or a description contains more than 2,000 characters, when the user submits the task form, then the task is not created or updated and a validation message is displayed. A valid title must contain 3–150 characters; a description may contain at most 2,000 characters.
- **AC03.5 — Invalid project:** Given the selected project does not exist, when a task is submitted, then no task is created.
- **AC03.6 — Task deletion:** Given the task creator or project owner confirms deletion, then the task is deleted together with its assignments and comments. Other authenticated users cannot delete it.

**Project decisions:** Authenticated users may create, view and update tasks in any project. Only the task creator or project owner may delete a task. Task title is required (3–150 characters); description is optional (maximum 2,000 characters). Status and priority use the documented project values and defaults.

### US04 — Update Task Status

**Priority:** High

**Related requirement:** FR05

**User Story:**

As an authenticated user, I want to update the status of a task in any project, so that the team can follow its progress.

**Acceptance Criteria:**

- **AC04.1 — Valid status update:** Given a task exists, when any authenticated user selects a supported status and saves it, then the new status is persisted and displayed.
- **AC04.2 — Current status:** Given that a task has a saved status, when the user views the task, then its current status is visible.
- **AC04.3 — Invalid status:** Given that a task has a saved status, when a value outside the supported status list is submitted, then the change is rejected and the previous status remains unchanged.

**Scope rule:** The project decision specifies `TODO`, `IN_PROGRESS` and `DONE`, with `TODO` as the default. Additional transition restrictions should be applied only if defined.

### US05 — View Project Progress and Dashboard Counts

**Priority:** High

**Related requirements:** FR05, FR11

**User Story:**

As an authenticated user, I want to view accessible projects and their task statuses, so that I can understand the current state of the work.

**Acceptance Criteria:**

- **AC05.1 — Task status visibility:** Given a project with tasks, when the user opens that project, then each displayed task has its current status shown.
- **AC05.2 — Reflected status change:** Given that a task's status has been changed successfully, when the user next views the project, then the saved status is displayed.
- **AC05.3 — Project without tasks:** Given a project with no tasks, when the user opens it, then the task list is empty and the interface handles the empty state clearly.
- **AC05.4 — Dashboard projects:** Given an authenticated user opens the dashboard, then all projects are displayed with navigation to their details.
- **AC05.5 — Counts by status:** Given the dashboard is displayed, then it shows counts across tasks in all projects for `TODO`, `IN_PROGRESS` and `DONE`, including zero counts. Counts include all priorities and assignees.
- **AC05.6 — Empty dashboard:** Given there are no projects or tasks, then the dashboard shows an appropriate empty state and zero for all three status counts.

**Scope note:** Task counts grouped by status are part of the current dashboard scope. Counts are derived and include all task priorities and assignees. No percentage, charts or advanced analytics are included.

### US06 — Assign Multiple Users to a Task

**Priority:** High

**Related requirement:** FR06

**User Story:**

As an authenticated user, I want to assign existing accounts to a task and remove assignments, so that task responsibility can be maintained.

**Acceptance Criteria:**

- **AC06.1 — Successful assignment:** Given a task and an existing user account, when an assignment is added, then the user is listed as an assignee regardless of project ownership.
- **AC06.2 — Multiple assignees:** Given a task and multiple existing user accounts, when assignments are added, then all assigned users are listed.
- **AC06.3 — Remove assignment:** Given a user is assigned to a task, when the assignment is removed, then that user is no longer listed.
- **AC06.4 — Duplicate assignment:** Given that a user is already assigned to a task, when the same assignment is submitted again, then the application does not create a duplicate task-user association.
- **AC06.5 — Non-existent user:** Given the selected user account does not exist, when the assignment is submitted, then it is rejected and existing assignments remain unchanged.

**Project decision:** Any authenticated user may add or remove assignments. Any existing user account may be assigned; project ownership is not required. Duplicate task/user assignments are prohibited.

### US07 — Authenticate and Identify the Current User

**Priority:** High

**Related requirement:** FR08

**User Story:**

As a registered demo user, I want to log in, log out and retrieve my authenticated identity, so that access to Matriz is associated with my account.

**Acceptance Criteria:**

- **AC07.1 — Successful login:** Given valid credentials for an existing account, when the user submits the login form, then authentication succeeds and the user can access the application according to their permissions.
- **AC07.2 — Invalid credentials:** Given invalid credentials, when the user attempts to log in, then authentication fails and an appropriate error is displayed without revealing sensitive account information.
- **AC07.3 — Current identity:** Given an authenticated session, when the application requests the current user's identity, then the backend returns the identity associated with that session.
- **AC07.4 — Logout:** Given an authenticated user, when the user logs out, then the session or authentication state is invalidated and protected operations can no longer be performed using that authentication state.
- **AC07.5 — Unauthenticated access:** Given that a user is not authenticated, when they attempt to access a protected operation, then the request is rejected.

**Scope note:** Public registration and password recovery are optional and are not required for the initial implementation. Demo accounts may be created through a database seed.

### US08 — Add and View Task Comments

**Priority:** High

**Related requirement:** FR07

**User Story:**

As an authenticated user, I want to add and view comments on any task, so that relevant discussion can be recorded alongside the work.

**Acceptance Criteria:**

- **AC08.1 — Add comment:** Given a task exists, when an authenticated user submits valid comment content, then the comment is saved and associated with the task.
- **AC08.2 — Comment author and date:** Given that a comment is saved, when it is displayed, then its author and creation date are shown using information recorded by the backend.
- **AC08.3 — View comments:** Given a task exists, when an authenticated user opens its comments, then the comments are listed with their authors and dates.
- **AC08.4 — Comment content validation:** Given that comment content is shorter than 1 character, longer than 2,000 characters, or contains only whitespace, when the user submits it, then the comment is rejected and is not saved. Valid content must contain 1–2,000 characters and at least one non-whitespace character.
- **AC08.5 — Unauthenticated comment access:** Given a user is not authenticated, when they attempt to view or add a comment, then the operation is rejected.

**Pending decision:** Comment editing and deletion are not included in the minimum scope, which requires adding and listing comments.

### US09 — Filter Tasks by Status and Priority

**Priority:** High

**Related requirement:** FR10

**User Story:**

As an authenticated user, I want to filter tasks by status and priority, so that I can find work matching the criteria I need to review.

**Acceptance Criteria:**

- **AC09.1 — Filter by status:** Given tasks have different statuses, when an authenticated user selects a status filter, then only tasks matching that status are returned.
- **AC09.2 — Filter by priority:** Given tasks have different priorities, when an authenticated user selects a priority filter, then only tasks matching that priority are returned.
- **AC09.3 — Combined filters:** Given an authenticated user supplies status and priority filters, then results match both criteria (AND behavior).
- **AC09.4 — Omitted filter:** Given either filter is omitted, then it does not restrict the results.
- **AC09.5 — No matching tasks:** Given no tasks match the selected criteria, when filters are applied, then no task entries are displayed and an appropriate empty state is shown.

**Project decision:** Any authenticated user may filter tasks across any project; filters combine with AND behavior.

## 3. Exceptional Situations and Edge Cases

### EC01 — Missing Required Information

**Situation:** A user attempts to create a project or task without providing required information.

**Expected behaviour:** The application rejects the submission, displays an appropriate validation message and does not create a record.

**Related stories:** US01, US03.

### EC02 — Project Without Tasks

**Situation:** A user opens a project that contains no tasks.

**Expected behaviour:** The task list is empty, the interface handles the empty state clearly, and dashboard counts remain consistent with the available data.

**Related stories:** US03, US05.

### EC03 — Invalid Task Status or Priority

**Situation:** A user attempts to submit a task with an unsupported status or priority.

**Expected behaviour:** The application rejects the invalid value and preserves the previously saved data.

**Related stories:** US03, US04, US09.

### EC04 — Non-Existent or Duplicate Task Assignment

**Situation:** A user account that does not exist is selected for assignment, or an existing task/user assignment is submitted again.

**Expected behaviour:** The application rejects the non-existent user and prevents duplicate task-user associations. Existing valid assignments remain unchanged. Any existing user account is eligible regardless of project ownership.

**Related story:** US06.

### EC05 — Task Associated with a Non-Existent Project

**Situation:** A user attempts to create or access a task associated with a project that does not exist.

**Expected behaviour:** The application rejects the operation and does not create an orphaned task. Any authenticated user may access an existing project.

**Related stories:** US02, US03.

### EC06 — Unauthenticated or Unauthorised Access

**Situation:** A user without a valid session, a non-owner attempts to edit/delete a project, or a user other than the project owner/task creator attempts to delete a task.

**Expected behaviour:** Unauthenticated operations are rejected. Authenticated users can view all projects and access tasks/comments in any project, while owner/creator-only delete/edit restrictions are enforced without applying additional roles.

**Related stories:** US01, US02, US03, US06, US07, US08, US09.

### EC07 — Empty or Invalid Comment

**Situation:** A user submits an empty or whitespace-only comment, content outside the length limits, or attempts comment access without authentication.

**Expected behaviour:** Comments must contain 1–2,000 characters and at least one non-whitespace character. Any authenticated user can view/add comments to any task; unauthenticated operations are rejected.

**Related story:** US08.

### EC08 — No Projects or No Matching Search Results

**Situation:** A user has no projects, or searches for a name that matches no project.

**Expected behaviour:** The application displays an appropriate empty state. An empty or omitted search returns all projects; a non-empty query is trimmed and matched partially without case sensitivity.

**Related story:** US02.

### EC09 — No Tasks Match the Selected Filters

**Situation:** A user applies status or priority filters that match no tasks.

**Expected behaviour:** The application displays no task entries and communicates that no results match the selected criteria.

**Related story:** US09.

### EC10 — Project or Task Deletion with Dependent Records

**Situation:** A project owner attempts to delete a project containing tasks, or an authorized task deletion targets a task with assignments/comments.

**Expected behaviour:** Project deletion is rejected with HTTP `409 Conflict` while tasks exist. Deleting a task also deletes its assignments and comments. Only a project owner can delete the project; only the project owner or task creator can delete a task.

**Related stories:** US02, US03, US06, US08.

### EC11 — Dashboard with No Accessible Data

**Situation:** An authenticated user has no projects or there are no tasks.

**Expected behaviour:** The dashboard displays all projects and counts across all project tasks, including zero for each of `TODO`, `IN_PROGRESS` and `DONE`; counts include every priority and assignee.

**Related story:** US05.

## 4. Validation Notes

- The user stories cover the functional requirements FR01–FR11 documented in `docs/requirements.md`.
- Acceptance criteria describe observable outcomes and should be verified through the user interface, API behaviour or automated tests, as appropriate.
- The current implementation scope includes authentication, project and task management, multiple task assignees, comments, project-name search, task filters and dashboard counts.
- Public registration and password recovery are optional extensions and are not required for the initial implementation.
- Project visibility and permissions, project/task validation, assignee eligibility, dependent-record deletion, search behavior, filter combination and dashboard count rules follow the project decisions recorded in `docs/requirements.md`.
- Technical account details remain unspecified; do not add roles or permission groups by inference.
- Task status and priority values, defaults and validation rules must be consistent across the requirements, database model, API contract and user interface.
- The user stories describe expected behaviour without prescribing the technical implementation.
- The original client brief and the Matriz implementation scope must remain distinguishable: the client confirmed only the high-level needs, while unspecified business rules must not be presented as client-confirmed decisions.

## 5. Current Status

This backlog reflects the current Matriz implementation scope. It includes user stories and acceptance criteria for authentication, project management, task management, multiple assignees, comments, project search, task filtering and dashboard counts.

The documented project decisions are reflected in these stories. Account representation remains unspecified; it must not be converted into invented roles or client-confirmed requirements.

The backlog should be reviewed alongside `docs/requirements.md`, `docs/constraints.md`, `docs/architecture.md`, `docs/database.md`, `openapi.yaml` and the UI wireframe during maintenance and verification. The implementation is in place; documentation and verification updates should reflect the current behavior without treating project decisions as additional client-confirmed requirements.
