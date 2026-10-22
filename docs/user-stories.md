
# Matriz — User Stories and Acceptance Criteria

## 1. Purpose

This document defines the initial user stories for the proposed Matriz MVP. The stories translate the functional requirements into user-focused, testable outcomes.

The client has not confirmed these detailed behaviours or personas. “High” and “Medium” are proposal priorities for this backlog, not client-assigned priorities. Stories with criteria that depend on unresolved access rules, required fields, status values or assignment eligibility are not ready for final acceptance until those decisions are made.

### Priority Definitions

- **High:** Essential to the proposed MVP.
- **Medium:** Provisional functionality requiring validation.
- **Low:** Not prioritised for the initial MVP.

“Project manager” and “team member” in the stories are provisional personas, not confirmed roles or permission groups.

## 2. User Stories

### US01 — Create a Project

**Priority:** High

**Related requirement:** FR01

**User Story:**

As a project manager, I want to create a project, so that I can organise work around a shared objective.

**Acceptance Criteria:**

- **AC01.1 — Successful creation:** Given a user permitted to create projects under the client-approved access rules and a non-empty project name, when the user submits the project form, then the project is created and appears in the project list.
- **AC01.2 — Missing name:** Given that the project name is missing, when the user submits the form, then the project is not created and a validation message is displayed.
- **AC01.3 — Project visibility:** Given that a project has been created successfully, when the user views the project list, then the new project is displayed according to the applicable access rules.

**Assumption:** A project must have a name. Other required project fields remain undefined.

### US02 — View Projects

**Priority:** High

**Related requirement:** FR02

**User Story:**

As a team member, I want to view the projects available to me, so that I can identify and access the work I need to follow.

**Acceptance Criteria:**

- **AC02.1 — Available projects:** Given that projects are available to the user, when the user opens the project list, then the projects they are authorised to access are displayed.
- **AC02.2 — No projects:** Given that the user has access to zero projects, when the user opens the project list, then the list contains zero project entries and indicates that no projects are available.

**Open question:** The rules defining project visibility and access must be confirmed.

### US03 — Create and Edit Tasks

**Priority:** High

**Related requirements:** FR03, FR04

**User Story:**

As a team member, I want to create and edit tasks within a project, so that I can organise the work needed to achieve the project's objectives.

**Acceptance Criteria:**

- **AC03.1 — Task creation:** Given that a valid project exists and the user is authorised, when the user submits a task with all required information, then the task is created and associated with that project.
- **AC03.2 — Task editing:** Given that a task exists and the user is authorised to edit it, when the user saves valid changes, then the updated information is displayed.
- **AC03.3 — Missing information:** Given that the task title is empty, when the user submits the task form, then the task is not created and a validation message is displayed. Other required fields remain subject to client validation.
- **AC03.4 — Invalid project:** Given that the selected project does not exist or the user lacks access under the client-approved access rules, when the user submits the task, then no task is created.
- **AC03.5 — View task:** Given a task the user may access, when the user opens its project, then the task title and current status are displayed.

**Assumption:** A task must have a title and belong to a project. Additional required fields must be defined.

### US04 — Update Task Status

**Priority:** High

**Related requirement:** FR05

**User Story:**

As a team member, I want to update the status of a task, so that the team can follow its progress.

**Acceptance Criteria:**

- **AC04.1 — Valid status update:** Given that a task exists and the user is authorised to edit it, when the user selects a value from the client-approved status list and saves the change, then that value is displayed for the task.
- **AC04.2 — Current status:** Given that a task has a status, when the user views the task, then its current status is visible.
- **AC04.3 — Invalid status:** Given that a task has a saved status, when a value outside the client-approved status list is submitted, then the change is rejected and the previously saved status remains displayed.

**Open question:** The permitted status values and any restrictions on status transitions must be defined.

### US05 — View Project Progress

**Priority:** High

**Related requirements:** FR06, FR07

**User Story:**

As a project manager, I want to view project progress based on its tasks, so that I can understand the current state of the work.

**Acceptance Criteria:**

- **AC05.1 — Task status visibility:** Given a project with one or more tasks, when the user opens that project, then each displayed task has its current status shown.
- **AC05.2 — Reflected status change:** Given a task whose status is changed to a value from the client-approved status list, when the user next views the project, then that task displays the saved value.
- **AC05.3 — Project without tasks:** Given a project with zero tasks, when the user opens it, then the task list is empty and no completion percentage is displayed.

**Scope note:** This story proposes showing task statuses as the progress view. A numerical completion percentage or other aggregate calculation is not required by the client brief and remains undecided.

### US06 — Assign a Task to a Team Member

**Priority:** Medium — candidate feature; pending client validation

**Related requirement:** FR08

**User Story:**

As a project manager, I want to assign a task to a team member, so that responsibility for the task is clear.

**Acceptance Criteria:**

- **AC06.1 — Successful assignment:** Given that a task and an eligible team member exist, when an authorised user assigns that member to the task, then the assignment is saved and displayed.
- **AC06.2 — Change assignment:** Given that a task has an assigned team member, when an authorised user changes the assignment, then the new assignment is displayed.
- **AC06.3 — Invalid assignment:** Given that a selected person is not eligible for assignment, when the assignment is submitted, then the assignment is rejected and a validation message is displayed.

**Scope note:** Task assignment is a candidate feature outside the core proposed MVP, pending client validation. Eligibility rules must be defined before implementation.

## 3. Exceptional Situations and Edge Cases

### EC01 — Missing Required Information

**Situation:** A user attempts to create a project or task without providing required information.

**Expected behaviour:** The application rejects the submission, displays a validation message identifying the missing required information and does not create a record.

**Related stories:** US01, US03.

### EC02 — Project Without Tasks

**Situation:** A user opens a project that contains no tasks.

**Expected behaviour:** The project displays zero task entries and no completion percentage.

**Related story:** US05.

### EC03 — Invalid Task Status

**Situation:** A user attempts to set a task to an unsupported status.

**Expected behaviour:** The application rejects the invalid change and preserves the existing status.

**Related story:** US04.

### EC04 — Invalid Task Assignment

**Situation:** A user attempts to assign a task to a person who is not eligible for assignment.

**Expected behaviour:** The application rejects the assignment and explains the validation failure.

**Related story:** US06. This case depends on task assignment being approved for the MVP and eligibility rules being defined.

### EC05 — Task Associated with a Non-Existent Project

**Situation:** A user attempts to create a task associated with a project that does not exist or is unavailable.

**Expected behaviour:** The application rejects the operation and does not create an orphaned task.

**Related story:** US03.

## 4. Validation Notes

- Acceptance criteria describe observable outcomes. Criteria depending on unresolved business rules are conditional and require validation before final acceptance.
- Every core functional requirement FR01–FR07 is linked to at least one story; FR06 and FR07 are linked to US05.
- High-priority stories form the proposed core MVP baseline; this is a course proposal, not a client-approved priority.
- US06 and FR08 are a candidate feature pending client validation, outside the core proposed MVP.
- Project access rules, task fields, permitted statuses and any progress calculation remain open decisions.
- No technical architecture or implementation details are prescribed in this backlog.

## 5. Current Status

This is an initial backlog for requirements validation and future development. It should be updated when outstanding assumptions and business rules are clarified.
