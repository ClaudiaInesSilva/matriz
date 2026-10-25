
# Matriz

Matriz is a project and task management application intended for work teams within a company.

## Problem Statement

The client brief confirms one need: teams need an application to organise projects, manage tasks and follow work progress. It does not define detailed functionalities, business rules, technical requirements or development priorities.

Matriz aims to provide a structured way to manage project work and make task progress visible.

## Main Objectives

- Organise work into projects.
- Create and manage tasks associated with projects.
- Track task status.
- Support the client's stated need to follow work progress. How progress is represented remains undecided.
- Establish a clear, documented foundation for future development.

## Proposed MVP Features

The proposed MVP for requirements validation is limited to:

- Creating and viewing projects.
- Creating, viewing and editing tasks.
- Associating tasks with projects.
- Updating task status.
- Viewing task statuses within their projects. This is the current proposed interpretation of progress tracking; no aggregate or numerical completion percentage is required by the brief.

Task assignment is a candidate feature, outside the core proposed MVP until the client validates it. Notifications, advanced reporting, external integrations, workflow automation and custom dashboards are scope proposals for consideration, not confirmed client exclusions.

All detailed features in this MVP are proposed interpretations, not confirmed client requirements. The MVP is a clear working baseline for the next course phase and remains subject to client validation.

## Documentation Structure

The `docs/` directory contains the main project documentation.

| File | Purpose |
|---|---|
| `docs/requirements.md` | Application vision, user types, functional and non-functional requirements, MVP scope and open questions. |
| `docs/user-stories.md` | Initial user stories, priorities, acceptance criteria and exceptional cases. |
| `docs/constraints.md` | Known constraints, assumptions, security and privacy considerations, pending technical decisions and scope limitations. |
| `docs/architecture.md` | Proposed modular monolith architecture for the MVP and its components. |
| `docs/database.md` | Technology-independent conceptual data model, relationships and open questions. |
| `openapi.yaml` | Simplified proposed API contract for selected MVP operations. |
| `docs/ui/dashboard-wireframe.html` | Low-fidelity HTML dashboard wireframe; illustrative, not an implemented application. |

## Current Project Status

**Phase:** Requirements analysis and context preparation.

The initial requirements proposal, user stories, acceptance criteria, assumptions, constraints and critical reviews have been documented. A modular monolith is proposed for the MVP; implementation technologies and detailed technical architecture remain undecided. Supporting artefacts include a conceptual data model, a simplified API contract and an illustrative HTML wireframe. No application implementation is included.

The next step is to validate open business questions and the proposed scope against any further client guidance, then follow the course guidance for the next development phase.

## Working Principles

- Consult the documentation before proposing or implementing changes.
- Distinguish confirmed requirements from assumptions.
- Do not add unapproved features or make unapproved technical decisions.
- Keep development aligned with the proposed MVP baseline until it is validated; do not imply client approval.
- Review and update the documentation when requirements or decisions change.
