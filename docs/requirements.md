
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
- View available projects and their tasks.
- Create and edit tasks, subject to the permissions eventually defined.
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

The following requirements define the proposed functional scope.

| ID | Requirement | Priority | Status |
|---|---|---|---|
| FR01 | The application shall allow authorised users to create projects. | Essential | Proposed |
| FR02 | The application shall display projects available to the user. | Essential | Proposed |
| FR03 | The application shall allow authorised users to create, view and edit tasks. | Essential | Proposed |
| FR04 | Each task shall be associated with a project. | Essential | Proposed |
| FR05 | The application shall allow authorised users to update task status. | Essential | Proposed |
| FR06 | The application shall display task statuses within their respective projects. | Essential | Proposed |
| FR07 | The application shall let users follow work progress by viewing the statuses of tasks in a project. This proposal does not require a calculated or numerical completion value. | Essential | Proposed |
| FR08 | The application may allow tasks to be assigned to team members. | Medium | Candidate feature; pending client validation |

“Essential” means essential to this proposed MVP baseline, not approved or prioritised by the client. Task assignment is a candidate feature outside the core baseline until validated.

### 4.1 Functional Behaviour and Validation

- Project creation must reject missing required information.
- Task creation must reject missing required information.
- Tasks must belong to a valid project.
- Task status updates must use valid statuses.
- A project without tasks must indicate that there are no task statuses to display; it must not show a fabricated progress value.
- Task assignment, if later approved, must follow eligibility rules defined by the client.

The required fields, task statuses, access rules and assignment rules remain to be finalised.

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

The proposed Minimum Viable Product (MVP) will focus on the smallest useful set of features needed to organise projects, manage tasks and track work progress.

### Core proposed MVP baseline (pending client validation)

- Create and view projects.
- Create, view and edit tasks.
- Associate tasks with projects.
- Update task status.
- View task status within each project.
- Follow work progress by viewing task statuses within a project. No percentage or aggregate calculation is included in this baseline.

### Candidate feature pending validation

- Task assignment to team members. Do not treat this as approved MVP scope until the client confirms it and defines eligibility.

### Proposed out-of-scope items (not confirmed client exclusions)

- Notifications and automated reminders.
- Advanced reporting and analytics.
- External integrations with other project management tools.
- Advanced workflow automation.
- Advanced custom dashboards.
- Complex role and permission management beyond the access controls required to protect data.

Basic security and access protection remain necessary considerations even when advanced permission management is out of scope.

## 7. Open Questions

The following questions should be clarified before the requirements are considered final:

- What information must a project contain?
- What information must a task contain?
- Which task statuses should be available?
- Can users create projects, or is this restricted to project managers?
- Should tasks be assigned to specific team members?
- Must task assignment be mandatory or optional?
- Is viewing task statuses sufficient to follow progress, or is another representation required? No numerical percentage is assumed.
- What access restrictions are required?
- Are the provisional user types useful, and are they separate roles or overlapping responsibilities?
- Are authentication, notifications, deadlines or reporting required in the initial version?

## 8. Current Status

This document represents an initial functional proposal based on the client's brief and the analysis performed during the practical exercises. Requirements and priorities remain subject to validation.
