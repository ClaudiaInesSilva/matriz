
# Exercise 3 — User Stories and Acceptance Criteria

## 1. Introduction

The objective of this exercise is to transform the functional requirements identified in Exercise 2 into clear, prioritised and testable user stories.

The user stories focus on the proposed MVP for Matriz, which aims to help work teams organise projects, manage tasks and track work progress.

Task assignment is included as a provisional feature that requires client validation. Notifications, advanced reporting and external integrations are excluded from the initial MVP.

### Priority Definitions

- **High:** Essential to the core purpose of the MVP.
- **Medium:** Useful for the MVP but subject to validation or dependent on other functionality.
- **Low:** Not prioritised for the initial MVP.

## 2. User Stories

### US01 — Create a Project

**Priority:** High

**User Story:**

As a project manager, I want to create a project, so that I can organise work around a shared objective.

**Acceptance Criteria:**

- **AC1:** Given that the user is authorised to create projects, when they provide a valid project name and submit the form, then the project is created and becomes available in the project list.
- **AC2:** Given that the project name is missing, when the user attempts to create the project, then the project is not created and a validation message is displayed.
- **AC3:** Given that a project has been created successfully, when the user views the project list, then the new project is displayed.

**Assumption:** A project must have a name. Additional project fields have not yet been defined.

### US02 — View Projects

**Priority:** High

**User Story:**

As a team member, I want to view the projects available to me, so that I can identify and access the work I need to follow.

**Acceptance Criteria:**

- **AC1:** Given that at least one project is available to the user, when they open the project list, then the available projects are displayed.
- **AC2:** Given that no projects are available to the user, when they open the project list, then an appropriate empty-state message is displayed.

**Assumption:** The rules determining which projects each user can access remain to be defined.

### US03 — Create and Manage Tasks

**Priority:** High

**User Story:**

As a team member, I want to create and edit tasks within a project, so that I can organise the work required to achieve the project's objectives.

**Acceptance Criteria:**

- **AC1:** Given that a project exists, when an authorised user provides the required task information and submits it, then the task is created and associated with that project.
- **AC2:** Given that a task exists, when an authorised user edits its information and saves the changes, then the updated information is displayed.
- **AC3:** Given that the required task information is missing, when the user attempts to create the task, then the task is not created and a validation message is displayed.

**Assumption:** A task must have a title and belong to a project. Other required fields remain to be confirmed.

### US04 — Update Task Status

**Priority:** High

**User Story:**

As a team member, I want to update the status of a task, so that the team can follow its progress.

**Acceptance Criteria:**

- **AC1:** Given that a task exists, when an authorised user selects a valid status and saves the change, then the task displays the selected status.
- **AC2:** Given that a task has a status, when the user views the task, then its current status is visible.
- **AC3:** Given that a user attempts to set an invalid status, when the change is submitted, then the change is rejected and the existing status remains unchanged.

**Assumption:** The initial set of task statuses must be agreed upon. Specific status names and permitted transitions have not yet been confirmed.

### US05 — View Project Progress

**Priority:** High

**User Story:**

As a project manager, I want to view the progress of a project based on its tasks, so that I can understand the current state of the work.

**Acceptance Criteria:**

- **AC1:** Given that a project contains tasks with different statuses, when the project progress view is opened, then the statuses of its tasks are displayed.
- **AC2:** Given that a task's status is updated successfully, when the project progress view is refreshed, then the updated status is reflected.
- **AC3:** Given that a project contains no tasks, when its progress view is opened, then the application indicates that there are no tasks to report.

**Assumption:** The MVP will initially track progress through task statuses. A numerical completion percentage or other progress calculation requires a separate decision.

### US06 — Assign a Task to a Team Member

**Priority:** Medium — provisional, pending client validation

**User Story:**

As a project manager, I want to assign a task to a team member, so that responsibility for the task is clear.

**Acceptance Criteria:**

- **AC1:** Given that a task and an eligible team member exist, when an authorised user assigns that member to the task, then the assignment is saved and displayed.
- **AC2:** Given that a task has an assigned team member, when an authorised user changes the assignment, then the new assignment is displayed.
- **AC3:** Given that the selected team member is not eligible for assignment, when the assignment is submitted, then the application rejects the assignment and displays a validation message.

**Assumption:** Task assignment is a proposed feature. The rules defining eligible team members and whether assignment is mandatory must be validated with the client.

## 3. Exceptional Situations and Edge Cases

The following situations should be considered during requirements validation and testing.

### EC01 — Missing Required Information

**Situation:** A user attempts to create a project or task without providing the required information.

**Expected behaviour:** The application rejects the submission, displays a clear validation message and does not create an incomplete record.

### EC02 — Project Without Tasks

**Situation:** A user opens a project that does not contain any tasks.

**Expected behaviour:** The application displays an appropriate empty state and does not show misleading progress information.

### EC03 — Invalid Task Status

**Situation:** A user attempts to set a task to a status that is not supported by the application.

**Expected behaviour:** The application rejects the invalid status change and preserves the existing status.

### EC04 — Invalid Task Assignment

**Situation:** A user attempts to assign a task to a person who is not eligible for assignment.

**Expected behaviour:** The application rejects the assignment and explains that the selected person cannot be assigned to the task.

**Note:** EC04 depends on task assignment being confirmed as part of the MVP and on the eligibility rules being defined.

## 4. Review and Validation

The user stories and acceptance criteria were reviewed against the initial functional scope defined in Exercise 2.

The following principles were applied:

- Each user story identifies a user type, a desired capability and its benefit.
- High-priority stories support the core needs of project organisation, task management and progress tracking.
- Acceptance criteria describe observable outcomes that can be tested.
- Given / When / Then is used where it helps clarify the initial state, action and expected result.
- Assumptions are explicitly identified rather than presented as confirmed client requirements.
- Task assignment remains provisional pending client validation.
- Notifications, advanced reporting and external integrations are not included in the initial backlog.
- Technical implementation details are deliberately excluded because they have not yet been decided.

Some details still require clarification, including the exact task fields, the list of task statuses, project access rules and the calculation (if any) used to summarise project progress.

## 5. Conclusion

This exercise produced an initial backlog for Matriz containing six user stories with priorities, acceptance criteria and exceptional cases.

The stories provide a basis for validating the proposed MVP and for checking whether its main functionalities behave as expected. Before implementation, the remaining assumptions and business rules should be reviewed and confirmed where necessary.
