# Matriz — Proposed MVP Data Model

## 1. Summary and Scope

This document defines the proposed conceptual data model for the Matriz implementation required by Lab 03. It describes the entities, attributes and relationships needed to support user authentication, project and task management, multiple task assignees, task comments and progress visibility.

The original client brief establishes the need to organise projects, manage tasks and follow work progress. Lab 03 expands the earlier proposed MVP and defines the minimum functionality to implement for the practical exercise. This expanded scope should not be interpreted as independent confirmation that the client validated every detailed behaviour.

The model must support:

- User accounts and identification of the authenticated user.
- All authenticated users can create, list and view all projects; each project's creator is its owner, and only the owner can edit or delete it.
- Tasks associated with projects, including creator, status and priority. Any authenticated user can create, view and update a task in any project; only the project owner or task creator can delete it.
- Multiple assignments to any existing user account, regardless of project ownership; any authenticated user may add or remove assignments, and duplicate task/user pairs are prevented.
- Task comments with author and creation date; any authenticated user can view and add them.
- Project-name search and task status/priority filtering with the approved matching rules.
- Dashboard counts derived from all tasks across all projects, grouped for all three statuses including zero counts.

The model is conceptual. Database-specific implementation types, schema migrations, indexes and configuration will be defined during implementation planning. The selected working database technology is PostgreSQL, as documented in `docs/architecture.md`.

These are project decisions for the Lab 03 course implementation, not client-confirmed requirements. Account lifecycle details remain unspecified. Project/task fields, validation, access and deletion behavior are recorded below.

## 2. Entities and Attributes

The entities below represent the proposed data model for the Lab 03 implementation. Attribute types are conceptual labels rather than database-specific types. Requiredness and validation rules must be implemented consistently with the requirements and API contract.

### User — account entity

Represents a user account that can authenticate and interact with Matriz.

| Attribute | Key | Meaning and status |
|---|---|---|
| `user_id` | Primary key (PK) | Unique identifier for the user. |
| `username` | Unique candidate | Identifier used to distinguish accounts. The final login identifier must be documented. |
| `password_hash` | — | Password hash for local authentication. Plain-text passwords must not be stored. |
| `display_name` | — | Name displayed in the interface and alongside comments and assignments. |

The account model supports login, logout and retrieval of the current authenticated user. Public registration and password recovery are optional and are not required for the initial implementation. Demo accounts may be created using a seed.

### Project — project entity

Represents a project that groups related tasks.

| Attribute | Key | Meaning and status |
|---|---|---|
| `project_id` | Primary key (PK) | Unique identifier for the project. |
| `name` | — | Required project name, 3–150 characters. |
| `description` | — | Optional project description, maximum 2,000 characters. |
| `owner_id` | Foreign key (FK) | References `User.user_id`; set by the backend to the authenticated user who created the project. |

All authenticated users may view the project. Only its owner may edit or delete it. A project with tasks cannot be deleted; the API returns HTTP `409 Conflict`.

### Task — task entity

Represents a task associated with one project.

| Attribute | Key | Meaning and status |
|---|---|---|
| `task_id` | Primary key (PK) | Unique identifier for the task. |
| `project_id` | Foreign key (FK) | References `Project.project_id`; each task belongs to one valid project. |
| `creator_id` | Foreign key (FK) | References `User.user_id`; records the authenticated user who created the task. |
| `title` | — | Task title. Lab 03 specifies a length between 3 and 150 characters. |
| `description` | — | Optional task description, with a maximum length of 2,000 characters under the lab's stated validation rules. |
| `status` | — | Current task status. Supported values: `TODO`, `IN_PROGRESS`, `DONE`. Default: `TODO`. |
| `priority` | — | Task priority. Supported values: `LOW`, `MEDIUM`, `HIGH`. Default: `MEDIUM`. |

The status and priority values and defaults above follow the Lab 03 exercise specification. They must be used consistently in the API, database and interface.

### TaskAssignment — task-user association

Represents the assignment of a user to a task. This association allows a task to have multiple assignees; any existing account may be assigned regardless of project ownership.

| Attribute | Key | Meaning and status |
|---|---|---|
| `task_id` | Composite primary key (part) and FK | References `Task.task_id`. |
| `user_id` | Composite primary key (part) and FK | References `User.user_id`. |

The combination of `task_id` and `user_id` must be unique, preventing the same user from being assigned to the same task more than once. Any authenticated user may add or remove these assignment links.

### Comment — task comment entity

Represents a comment added to a task.

| Attribute | Key | Meaning and status |
|---|---|---|
| `comment_id` | Primary key (PK) | Unique identifier for the comment. |
| `task_id` | Foreign key (FK) | References the task being commented on. |
| `author_id` | Foreign key (FK) | References `User.user_id`; the backend derives the author from the authenticated identity. |
| `content` | — | Comment text. Lab 03 specifies a length between 1 and 2,000 characters and rejects whitespace-only content. |
| `created_at` | — | Date and time the comment was created, recorded by the backend. |

Any authenticated user may add and list comments on any task. Comment editing is not included in the Lab 03 scope. Deleting a task also deletes its comments.

### Access Rules and Derived Information

The approved project access rules are: all authenticated users can create/list/view projects; only the owner can edit/delete a project. Any authenticated user can create/view/update tasks in any project; only the project owner or task creator can delete a task. Any authenticated user can view/add comments and add/remove task assignments. Any existing account is assignment-eligible, regardless of project ownership.

Project deletion is restricted while tasks exist (`409 Conflict`). Task deletion cascades to its assignments and comments. Dashboard counts across all projects are derived from tasks, include `TODO`, `IN_PROGRESS` and `DONE` even at zero, and ignore priority and assignees; no separate count/progress entity is needed. Search and filters operate on existing records.

## 3. Relationships and Cardinalities

| Relationship | Cardinality | Justification |
|---|---|---|
| User to Project (owner) | One User may own zero or many Projects; each Project has one owner. | The backend records the authenticated project creator as owner. |
| Project to Task | One Project has zero or many Tasks; each Task belongs to exactly one Project. | Projects may have no tasks; every task must reference a valid project. |
| User to Task (creator) | One User may create zero or many Tasks; each Task has exactly one creator. | Supports the approved task deletion rule. |
| User to Task (assignee) | One User may be assigned to zero or many Tasks; each Task may have zero or many assigned Users. | Lab 03 requires multiple eligible users to be assignable to a task. |
| Task to TaskAssignment | One Task has zero or many TaskAssignment records; each TaskAssignment refers to exactly one Task. | Implements the task side of the many-to-many assignment relationship. |
| User to TaskAssignment | One User has zero or many TaskAssignment records; each TaskAssignment refers to exactly one User. | Implements the user side of the many-to-many assignment relationship. |
| Task to Comment | One Task has zero or many Comments; each Comment belongs to exactly one Task. | Lab 03 requires comments to be added to and listed for a task. |
| User to Comment (author) | One User may author zero or many Comments; each Comment has exactly one author. | The backend records the authenticated user as the comment author. |

The `TaskAssignment` entity is an association between `Task` and `User`. The pair `(task_id, user_id)` must be unique so that a user cannot be assigned to the same task more than once.

Project ownership governs only project edit/delete and is one of the two bases for task deletion. All authenticated users can view all projects and access tasks/comments in any project. Assignment eligibility does not depend on ownership.

## 4. Business Rules Represented

- **Project ownership and permissions:** The creator is recorded as owner. All authenticated users may create/list/view projects; only the owner may edit/delete.
- **Project-task relationship:** Each task must belong to one valid project. A project may have zero or many tasks. The application must prevent orphaned tasks.
- **Task creator and permissions:** The backend records the authenticated task creator. Any authenticated user may create/view/update tasks in any project; only the project owner or task creator may delete.
- **Project deletion:** Deletion is restricted while tasks exist and returns HTTP `409 Conflict`.
- **Task deletion:** Deleting a task removes its task-assignment and comment records.
- **Project fields and validation:** Name is required and 3–150 characters; description is optional and at most 2,000 characters.
- **Task fields and validation:** Task title is required and must contain 3–150 characters. Description is optional and may contain up to 2,000 characters.
- **Task status:** A task stores its current status. Supported values are `TODO`, `IN_PROGRESS` and `DONE`; the default is `TODO`.
- **Task priority:** A task stores its priority. Supported values are `LOW`, `MEDIUM` and `HIGH`; the default is `MEDIUM`.
- **Multiple task assignees:** A task may have zero or many assignees. Each assignment links one task to one user, and duplicate task-user associations are not allowed.
- **Assignment permissions and eligibility:** Any authenticated user may add/remove task assignments. Any existing user account may be assigned regardless of project ownership. A task may have multiple assignees; duplicate task/user associations are prohibited.
- **Comments:** Each comment belongs to one task and has one author and a creation date. Any authenticated user may view/add comments; the backend derives author/date. Content must be 1–2,000 characters and not whitespace-only.
- **Authentication:** User accounts support login, logout and retrieval of the current authenticated user's identity. Passwords must not be stored in plain text.
- **Access control:** The backend enforces the approved project/task rules above. All authenticated users can search/list all projects, filter tasks across projects and view the global dashboard; project edit/delete and task delete use the specified owner/creator rules.
- **Dashboard counts:** Include all projects and tasks across them, grouped for all statuses (including zero) regardless of priority/assignee. Counts are derived and not stored separately.
- **Search and filters:** Search is partial/case-insensitive after trimming query-edge whitespace; empty/omitted query returns all projects. Task filters combine with AND; omitted filters do not restrict results.
- **Deletion and dependent records:** Project deletion is restricted if it contains tasks (`409 Conflict`). Task deletion cascades to its assignments and comments.

## 5. Mermaid ER Diagram

The diagram represents the proposed conceptual data model for the Lab 03 implementation. `PK` identifies a primary key and `FK` identifies a foreign key.

`TASK_ASSIGNMENT` implements the many-to-many relationship between users and tasks. Its composite primary key prevents duplicate assignments of the same user to the same task.

```mermaid
erDiagram
    USER {
        identifier user_id PK
        text username
        text password_hash
        text display_name
    }

    PROJECT {
        identifier project_id PK
        text name
        text description
        identifier owner_id FK
    }

    TASK {
        identifier task_id PK
        identifier project_id FK
        identifier creator_id FK
        text title
        text description
        status_value status
        priority_value priority
    }

    TASK_ASSIGNMENT {
        identifier task_id PK, FK
        identifier user_id PK, FK
    }

    COMMENT {
        identifier comment_id PK
        identifier task_id FK
        identifier author_id FK
        text content
        datetime created_at
    }

    USER ||--o{ PROJECT : owns
    PROJECT ||--o{ TASK : contains
    USER ||--o{ TASK : creates
    USER ||--o{ TASK_ASSIGNMENT : receives
    TASK ||--o{ TASK_ASSIGNMENT : has
    USER ||--o{ COMMENT : writes
    TASK ||--o{ COMMENT : contains
```

The diagram represents the following rules:

- Each project has one owner.
- Each task belongs to exactly one project.
- Each task has exactly one creator.
- A task may have zero or many assignees.
- A user may be assigned to zero or many tasks.
- Each comment belongs to exactly one task and has exactly one author.
- A project or task may exist without comments or assignments.

Referential behavior: a project with tasks cannot be deleted; task deletion cascades to its assignment and comment rows. Application authorization follows the approved rules in §4.

## 6. Assumptions and Questions for Implementation

### 6.1 Decisions Based on Lab 03

The following model elements reflect the minimum functional scope and validation rules specified by Lab 03:

- User accounts support authentication and identification of the current user.
- Any authenticated user can create/list/view all projects; the creator is owner and only the owner can edit/delete.
- Project name is required (3–150 characters); description is optional (maximum 2,000 characters).
- Tasks belong to projects and support creation, viewing, editing and deletion.
- Any authenticated user can create/view/update tasks in any project; the backend records the creator; only the project owner or task creator may delete.
- Task titles contain between 3 and 150 characters.
- Task descriptions have a maximum length of 2,000 characters.
- Supported task statuses are `TODO`, `IN_PROGRESS` and `DONE`, with `TODO` as the default.
- Supported task priorities are `LOW`, `MEDIUM` and `HIGH`, with `MEDIUM` as the default.
- Any authenticated user may add/remove assignments; any existing account is eligible regardless of project ownership, and tasks may have multiple assignees.
- Duplicate task-user assignments are not allowed.
- Project deletion is restricted while tasks exist; task deletion cascades to assignments and comments.
- Comments are associated with tasks and record their author and creation date.
- Comment content contains between 1 and 2,000 characters and must not consist only of whitespace.
- Dashboard counts across all projects include `TODO`, `IN_PROGRESS` and `DONE`, including zero; counts ignore priority and assignees and are derived from tasks.
- Project search is trimmed, case-insensitive partial matching; empty/omitted query returns all projects. Task filters combine with AND; omitted filters do not restrict results.

### 6.2 Recorded Project Decisions for the Lab 03 Implementation

These project decisions are approved for the course implementation, not independently confirmed by the original client:

- Each project has one owner, identified by `owner_id`; the backend derives ownership and comment authorship from the authenticated user, not client-supplied identity values.
- Each task records `creator_id`, used with project ownership to authorize deletion.
- `TaskAssignment` uses a composite key consisting of `task_id` and `user_id`.
- Any existing user account is eligible for assignment; assignment eligibility does not require project ownership.
- The project/task/comment/assignment permissions, project restriction and task cascade deletion rules in §4 apply.
- Dashboard counts, project search and task filters follow the rules in §4 and use existing records rather than separate persisted data.
- Comments can be added and listed; editing is outside Lab 03 scope.

These decisions implement the course scope; they are not additional requirements confirmed by the original client.

### 6.3 Remaining Questions

The following matters must be resolved or documented before the affected functionality is implemented:

- Which account identifier is used for login?
- Which user attributes are required beyond the conceptual fields shown here?
- Should project and task creation dates or update dates be stored?
- Should comment editing be added in a future extension? It is outside the current scope.

Decisions must be recorded consistently in the requirements, architecture, API contract and implementation documentation.

## 7. Consistency Review and Limitations

- The model includes the entities needed to support the Lab 03 functional scope: `User`, `Project`, `Task`, `TaskAssignment` and `Comment`.
- Each task belongs to exactly one project, while a project may contain zero or many tasks.
- Multiple task assignees are represented through the `TaskAssignment` association rather than a single assignee field.
- The composite key on `TaskAssignment` prevents duplicate assignments of the same user to the same task.
- Comments are associated with a task and a user author. Their creation date is recorded as part of the comment.
- Task status and priority values and defaults follow the Lab 03 specification.
- Dashboard counts, project search and task filters use existing project and task records. They do not require separate data entities.
- Project ownership and task creator support the approved project-edit/delete and task-delete rules; all authenticated users otherwise have the documented project/task/comment access.
- The model records the approved restrictive project deletion and task dependent-record cascades.
- The model does not prescribe database-specific implementation details, indexes, migration scripts or deployment configuration. These will be addressed during implementation.
- The model must remain consistent with `docs/requirements.md`, `docs/user-stories.md`, `docs/architecture.md`, `docs/constraints.md` and `openapi.yaml`.
- Any unresolved business rules must be documented before implementing the affected operations.

This model is the proposed conceptual basis for the Lab 03 implementation. It should be reviewed alongside the API contract and implementation plan before database schema creation begins.
