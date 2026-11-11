# Matriz — Architecture Proposal

## 1. Architecture Options

### Monolith

The application is developed and deployed as a single unit.

- **Advantages:** Simple to develop, test and deploy.
- **Disadvantages:** Responsibilities can become tightly coupled as the application grows.
- **Suitability:** Could support the Lab 03 implementation scope as a single application.

### Modular Monolith

The application is deployed as a single unit but organised into modules with separate responsibilities.

- **Advantages:** Keeps deployment simple while improving separation of concerns and maintainability.
- **Disadvantages:** Requires clear module boundaries and discipline to prevent excessive coupling.
- **Suitability:** Proposed for the Lab 03 implementation scope, with internal responsibility boundaries while retaining a single backend deployment.

### Microservices

The application is divided into independently deployable services.

- **Advantages:** Services can be developed, deployed and scaled independently when there is a genuine need.
- **Disadvantages:** Introduces additional complexity in communication, deployment, monitoring and data coordination.
- **Suitability:** Not selected for Lab 03; the documented scope does not require independently deployed services.

## 2. Recommended Architecture

**Proposed decision for Lab 03: Modular Monolith.**

A Modular Monolith offers a good balance between simplicity and maintainability for Matriz. Authentication, project management, task management, assignments, comments, search, filtering and dashboard counts can operate within one backend application while keeping their responsibilities separate.

This avoids the additional operational complexity of microservices while providing a clearer internal structure than an unstructured monolith. This is a proposed working architecture for the Lab 03 course implementation, not a requirement confirmed by the client.

## 3. Main Components and Responsibilities

- **Authentication:** Support login, logout and retrieval of the current authenticated user using the proposed server-side session approach.
- **Project Management:** Any authenticated user can create, list and view all projects, and search by name. The creator owns a project; only its owner can edit or delete it. Deletion is restricted while tasks exist.
- **Task Management:** Create, list, view, edit and delete project tasks; manage status (`TODO`, `IN_PROGRESS`, `DONE`, default `TODO`), priority (`LOW`, `MEDIUM`, `HIGH`, default `MEDIUM`) and task filters.
- **Assignments:** Any authenticated user may add or remove assignments. Support multiple assignees; any existing user account is eligible regardless of project ownership, and duplicate task/user pairs are prohibited.
- **Comments:** Any authenticated user can add and list task comments with author and creation date.
- **Dashboard:** Show all projects and task counts across them for all three statuses, including zero; derive counts from task data.
- **Shared Validation and Access Protection:** Enforce the approved project and task rules. Any authenticated user can create, view and update tasks in any project; only the project owner or task creator can delete a task. Project deletion is restricted while tasks exist, and task deletion removes assignments and comments.

These responsibilities are internal application modules, not separately deployed services. The Lab 03 scope is a course implementation target and does not imply client confirmation of the detailed features.

## 4. Technology Decisions, Assumptions and Pending Decisions

### 4.1 Proposed Technology Stack

The following technology stack has been selected as the working proposal for the Matriz implementation in Lab 03. These are implementation decisions made for the project and are not requirements explicitly imposed by the client or instructor.

| Area | Technology | Purpose |
|---|---|---|
| Programming language | Java | Backend development |
| Backend framework | Spring Boot | REST API, business logic and application configuration |
| Build tool | Maven | Dependency management and build automation |
| Frontend | React with TypeScript | User interface |
| Frontend tooling | Vite and npm | Frontend development and dependency management |
| Database | PostgreSQL | Persistent data storage |
| Data access | Spring Data JPA | Database access and persistence |
| Authentication and access protection | Spring Security with server-side session-based authentication | Authentication and request authorisation |
| Backend testing | JUnit 5 and Spring Boot Test | Automated backend testing |

The backend will follow the proposed Modular Monolith architecture. The frontend will be a separate application that communicates with the backend through a REST API. PostgreSQL will persist the application's data.

IntelliJ IDEA Community Edition will be used for backend development, and Visual Studio Code will be used for frontend development. Both are intended to be used with their respective free features.

### 4.2 Architectural Decisions

The Modular Monolith remains the proposed architecture because it provides clear separation of responsibilities without the operational complexity of microservices. Authentication is proposed to use server-side sessions managed by Spring Security, consistent with the session cookie contract in `openapi.yaml`.

The frontend and backend will be developed as separate parts of the same project, but the backend will remain a single deployable application. Project Management, Task Management, Progress Tracking, validation and access protection will be organised as internal responsibilities rather than independently deployed services.

### 4.3 Assumptions and Pending Decisions

The client brief confirms the need to organise projects, manage tasks and follow work progress. The expanded functionality is the Lab 03 course implementation target and is not additional client-confirmed scope.

The following matters still require clarification or explicit implementation decisions:

- Concrete session configuration and CSRF configuration.
- Account lifecycle and required account attributes.
- Whether project/task fields beyond the documented scope are needed; do not add fields without a decision.
- Database schema and migration strategy, dependency versions, environment configuration and deployment arrangements.
- Frontend test tooling and project-specific non-functional targets.

The technology stack above is the current working proposal. Specific versions, session and CSRF configuration details, database migration approach and deployment arrangements remain to be established as implementation proceeds.
