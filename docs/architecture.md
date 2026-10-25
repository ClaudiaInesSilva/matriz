# Matriz — Architecture Proposal

## 1. Architecture Options

### Monolith

The application is developed and deployed as a single unit.

- **Advantages:** Simple to develop, test and deploy.
- **Disadvantages:** Responsibilities can become tightly coupled as the application grows.
- **Suitability:** Appropriate for the small scope of the Matriz MVP.

### Modular Monolith

The application is deployed as a single unit but organised into modules with separate responsibilities.

- **Advantages:** Keeps deployment simple while improving separation of concerns and maintainability.
- **Disadvantages:** Requires clear module boundaries and discipline to prevent excessive coupling.
- **Suitability:** Appropriate for the MVP, with a little more structure than a basic monolith.

### Microservices

The application is divided into independently deployable services.

- **Advantages:** Services can be developed, deployed and scaled independently when there is a genuine need.
- **Disadvantages:** Introduces additional complexity in communication, deployment, monitoring and data coordination.
- **Suitability:** Unnecessarily complex for the current MVP, given its limited scope and the close relationship between projects and tasks.

## 2. Recommended Architecture

**Proposed decision: Modular monolith.**

A modular monolith offers a good balance between simplicity and maintainability for Matriz. Project management, task management and progress tracking are closely related, so they can operate within one application while keeping their responsibilities separate.

This avoids the additional operational complexity of microservices while providing a clearer internal structure than an unstructured monolith. This is a proposed architectural decision for the MVP, not a requirement confirmed by the client.

## 3. Main Components and Responsibilities

- **Project Management:** Create and view projects, according to access rules still to be defined.
- **Task Management:** Create, view and edit tasks, associate them with projects, and manage task statuses.
- **Progress Tracking:** Display task statuses within each project to help users follow work progress. This is an internal responsibility of the application; it reads task information from Task Management and is not a separately deployed service.
- **Shared Validation and Access Protection:** Validate input and support protection against unauthorised access, with detailed rules still to be defined.

Task assignment may be added if confirmed as part of the MVP. It remains provisional pending client validation.

## 4. Assumptions and Pending Decisions

The client brief confirms the need to organise projects, manage tasks and follow work progress. The specific features listed in the project documentation, including the proposed MVP scope and viewing task statuses as the progress view, remain proposals pending client validation.

Detailed business rules, task statuses, access permissions and the inclusion of task assignment still require clarification. No programming language, framework, database or deployment technology has been selected.
