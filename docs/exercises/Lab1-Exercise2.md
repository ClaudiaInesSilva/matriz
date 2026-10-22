# Exercise 2 — Requirements Analysis with AI

## 1. Analysis of the Clarification Questions

The initial client description states that Matriz is an application intended to help work teams organise projects, manage tasks, and track work progress. However, the client has not yet defined detailed functionalities, business rules, technical requirements, or development priorities.

The following clarification questions were identified as important for defining the application's functional scope:

| Clarification question | Why it matters | Current status |
|---|---|---|
| Who will use Matriz? | Helps identify user types and their needs. | Work teams within a company are confirmed; specific roles remain undefined. |
| What is a project? | Defines how projects should be organised. | Requires clarification. |
| How should tasks be managed? | Determines the information and actions supported for each task. | Task management is confirmed; detailed rules are unknown. |
| Should tasks be assigned to team members? | Helps establish responsibility for work. | Requires validation. |
| How should progress be tracked? | Determines how users can understand the state of their work. | Progress tracking is confirmed; the method is undefined. |
| Are different user roles and permissions required? | Helps determine who can access or modify information. | Requires clarification. |
| Are notifications or reminders needed? | Helps identify communication and follow-up requirements. | Requires clarification. |
| Which features are essential for the first version? | Establishes the MVP scope and development priorities. | A provisional scope is proposed below. |

Since the client is not available to answer these questions during the exercise, reasonable assumptions will be documented and treated as provisional rather than confirmed requirements.

## 2. Answers and Working Assumptions

The following decisions provide a starting point for the functional specification.

| Topic | Working decision or assumption | Status |
|---|---|---|
| Target users | Teams working on shared projects within a company. | Confirmed by the client description. |
| Project organisation | Projects group tasks related to a common objective. | Assumption. |
| Task management | Users should be able to create, view and edit tasks. | Proposed functional interpretation. |
| Task status | Tasks should have a status that allows users to follow their progress. | Proposed assumption. |
| Task assignment | Tasks may be associated with responsible team members. | Provisional MVP assumption requiring client validation. |
| Progress tracking | Project progress can initially be represented by the status of its tasks. | Proposed assumption. |
| User permissions | Detailed roles and permissions will not be defined until the requirements are clarified. | Open question. |
| Notifications | Notifications and reminders will be considered future enhancements. | Proposed scope decision. |
| Technical decisions | Technologies, programming languages and architecture will remain undecided at this stage. | Deferred to a later phase. |

These assumptions allow the functional scope to be developed without presenting unconfirmed decisions as facts.

## 3. Main User Types

Two provisional user types are proposed for the initial version of Matriz.

### 3.1 Team Member

A team member participates in projects and performs tasks.

Main needs:
- View projects and their associated tasks.
- Create, view and edit tasks, subject to the permissions eventually defined.
- Update task status.
- Follow project progress.

### 3.2 Project Manager

A project manager coordinates project activities and monitors progress.

Main needs:
- Create and organise projects.
- View and manage tasks associated with projects.
- Monitor the status of project tasks.
- Obtain an overview of project progress.

These user types are proposed based on the application's intended purpose. The client has not confirmed that they must be separate roles; a single user may perform both sets of responsibilities.

A separate administrator role is not included in the initial scope because the need for dedicated user administration and permission management has not yet been established.

## 4. Proposed Functionalities and Priorities

The following priorities are proposed for the initial functional scope.

- **Essential:** Directly supports one of the client's three stated needs.
- **High — provisional:** Potentially valuable but requires validation.
- **Future:** Can be considered after the MVP.
- **To be assessed:** Requires further clarification before a scope decision can be made.

| ID | Functionality | Justification | Priority |
|---|---|---|---|
| F01 | Create and view projects. | Allows teams to organise their work into projects. | Essential |
| F02 | Create, view and edit tasks. | Provides the basic capabilities needed to manage tasks. | Essential |
| F03 | Associate tasks with projects. | Keeps tasks organised within the projects they belong to. | Essential |
| F04 | Update task status. | Allows users to follow the state of individual tasks. | Essential |
| F05 | View project progress based on task status. | Directly supports the client's requirement to track work progress. | Essential |
| F06 | Assign tasks to team members. | Could clarify responsibility and improve team coordination. | High — provisional, pending client validation |
| F07 | Define task deadlines. | Could help teams plan and monitor work over time. | Future |
| F08 | Receive notifications and reminders. | Could help users follow updates and upcoming deadlines. | Future |
| F09 | Generate advanced reports and analytics. | Could provide more detailed information about project performance. | Future |
| F10 | Manage detailed user roles and permissions. | Could support controlled access to projects and tasks. | To be assessed after clarification of access and security requirements |
| F11 | Integrate with external project management tools. | Could support workflows involving other applications. | Future |

The proposed priorities are based on the initial client description and our current assumptions. They must be reviewed if further information changes the understanding of the client's needs.

## 5. Essential Features Versus Future Features

### 5.1 Essential Features

The following functionalities are considered essential because they directly support the needs explicitly stated by the client:

- Project creation and viewing.
- Task creation, viewing and editing.
- Association of tasks with projects.
- Task status updates.
- A basic overview of project progress based on the status of associated tasks.

Task assignment is also proposed for consideration within the MVP, but it remains a provisional assumption until validated by the client.

### 5.2 Future Features

The following functionalities are excluded from the initial MVP proposal:

- Notifications and automated reminders.
- Advanced reporting and analytics.
- Integrations with external tools such as Jira, Azure DevOps or Trello.
- Workflow automation.
- Advanced dashboards and performance metrics.

Detailed user roles and permissions require further assessment. Their exclusion from the proposed MVP scope does not mean that basic authentication, access control, or security requirements can be ignored.

## 6. Minimum Viable Product (MVP)

The proposed MVP is the smallest useful version of Matriz that enables work teams to organise projects, manage tasks and track progress.

### 6.1 MVP Scope

The initial MVP should include:

1. **Project management:** Users can create and view projects.
2. **Task management:** Users can create, view and edit tasks.
3. **Project-task organisation:** Tasks are associated with their respective projects.
4. **Task status management:** Users can update the status of tasks.
5. **Basic progress tracking:** Users can view project progress based on the status of associated tasks.
6. **Task assignment — provisional:** Tasks may be associated with responsible team members, subject to client validation.

### 6.2 Out of Scope for the Initial MVP

The following are excluded from the initial proposal:

- Notifications and automated reminders.
- Advanced reporting and analytics.
- External integrations.
- Complex workflow automation.
- Advanced custom dashboards.

Detailed permissions, authentication requirements and other security-related decisions remain to be clarified before the functional and technical scope is finalised.

### 6.3 MVP Justification

This MVP focuses on the three needs explicitly mentioned by the client: project organisation, task management and progress tracking. It avoids adding features that have not yet been justified by the available requirements.

The proposed scope is provisional and should be validated against the client's expectations before implementation begins.

## 7. Summary of Decisions and Assumptions

The initial functional proposal establishes the following:

- Matriz is intended for work teams within a company.
- Project organisation, task management and progress tracking are confirmed needs.
- Projects are assumed to contain related tasks.
- Task statuses are proposed as a basic way to track progress.
- Task assignment is a provisional MVP assumption that requires client validation.
- Team members and project managers are proposed as the main user types, but they may not need to be separate roles.
- Notifications, advanced reports and integrations are considered future enhancements.
- Detailed permissions and security requirements need further clarification.
- Technical choices are deferred until the requirements have been sufficiently defined.

## Conclusion

This exercise produced an initial functional scope for Matriz by analysing the clarification questions, identifying the main user types, proposing and prioritising functionalities, and defining a provisional MVP.

The proposal distinguishes confirmed client needs from assumptions and decisions that still require validation. This provides a structured starting point for the next stages of requirements specification, user story creation and project documentation.
