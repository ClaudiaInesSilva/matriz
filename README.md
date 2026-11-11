
# Matriz

Matriz is a project and task management application intended for work teams within a company.

## Problem Statement

The client brief confirms one need: teams need an application to organise projects, manage tasks and follow work progress. It does not define detailed functionalities, business rules, technical requirements or development priorities.

Matriz aims to provide a structured way to manage project work and make task progress visible.

## Main Objectives

- Organise work into projects.
- Create and manage tasks associated with projects.
- Track task status and follow work progress.
- Establish a clear, documented foundation for future development.

The first three points reflect the client's high-level brief. It does not specify the detailed functionality or rules below.

## Lab 03 Implementation Scope

The following functionality forms the documented Lab 03 course implementation target. These course requirements and working decisions expand the original brief; they are not additional requirements independently confirmed by the client:

- Authentication: log in, log out and retrieve the current authenticated user.
- Projects: create, list and view all projects; only the owner can edit or delete.
- Tasks: create, list, view, edit and delete tasks associated with projects.
- Task status: `TODO`, `IN_PROGRESS` and `DONE`, with `TODO` as the default.
- Task priority: `LOW`, `MEDIUM` and `HIGH`, with `MEDIUM` as the default.
- Any authenticated user can assign existing users to a task and remove assignments; project ownership does not affect eligibility.
- Add and list task comments with author and creation date.
- Search projects by name and filter tasks by status and priority.
- Show all projects and task counts grouped by status on the dashboard.

Project visibility and ownership, task and assignment permissions, deletion behavior, assignment eligibility, search and filter semantics, dashboard counts, and project validation are recorded as approved working rules for the Lab 03 course implementation. They are not additional requirements independently confirmed by the original client. Account lifecycle details remain open.

## Proposed Architecture and Technology Stack

The proposed architecture is a Modular Monolith: a single backend deployment organized into modules, with a separate frontend communicating through a REST API. The current working technology proposal is:

- Backend: Java, Spring Boot and Maven.
- Frontend: React with TypeScript, using Vite and npm.
- Database and persistence: PostgreSQL and Spring Data JPA.
- Security: Spring Security with server-side session-based authentication.
- Backend testing: JUnit 5 and Spring Boot Test.

These are project working decisions for Lab 03, not client-confirmed technical requirements. Specific versions, configuration and deployment details remain to be established.

## Documentation Structure

The `docs/` directory contains the main project documentation.

| File | Purpose |
|---|---|
| `docs/requirements.md` | Application vision, user types, functional and non-functional requirements, MVP scope and open questions. |
| `docs/user-stories.md` | Initial user stories, priorities, acceptance criteria and exceptional cases. |
| `docs/constraints.md` | Known constraints, assumptions, security and privacy considerations, pending technical decisions and scope limitations. |
| `docs/architecture.md` | Proposed Modular Monolith architecture and working technology stack for Lab 03. |
| `docs/database.md` | Technology-independent conceptual data model, relationships and open questions. |
| `openapi.yaml` | Proposed REST API contract for the Lab 03 implementation scope. |
| `docs/ui/dashboard-wireframe.html` | Low-fidelity static HTML wireframe; illustrative, not an implemented application. |
| `docs/ui/wireframe.png` | PNG rendering of the illustrative wireframe. |

## Current Project Status

**Phase:** Lab 03 documentation preparation and consistency review.

Requirements, user stories, architecture, conceptual data model, API contract and UI wireframe are documented for implementation planning. The Lab 03 functional scope, business rules and proposed Modular Monolith technology stack are recorded as course requirements and working project decisions; none changes the limited client-confirmed brief. Remaining business questions concern account lifecycle; technical setup details also remain open.

Application implementation has not started. No application code has been implemented. The next step is to complete planning and resolve implementation-specific details before development begins.

## Working Principles

- Consult the documentation before proposing or implementing changes.
- Distinguish confirmed requirements from assumptions.
- Do not add unapproved features or make unapproved technical decisions.
- Keep the Lab 03 implementation target distinct from the original client-confirmed needs; do not imply that course requirements or working technical decisions are client-approved.
- Review and update the documentation when requirements or decisions change.
