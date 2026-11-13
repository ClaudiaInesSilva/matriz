# Matriz

Matriz is a project and task management application intended for work teams within a company. The client brief confirms the need to organise projects, manage tasks and follow work progress; detailed workflows and business rules remain project decisions or unresolved questions.

## Current implementation

The repository contains a runnable Spring Boot backend foundation with PostgreSQL persistence, JPA mappings, Flyway migrations and reproducible demonstration records. The React/TypeScript/Vite frontend is a starter scaffold. Authentication, functional project and task workflows, and a complete frontend have not been implemented.

The persistence model currently covers users, projects, tasks, task assignments and comments. The backend has been started successfully on port 8080 against PostgreSQL 18.7; Flyway validated and applied both migrations, its history recorded both as successful, and Hibernate schema validation succeeded. The frontend production build and development server have also been verified. These checks do not establish application-level workflow or persistence constraint test coverage.

## Technology

- Backend: Java 21, Spring Boot 4.1.2, Maven (install Maven locally), Spring Data JPA and Hibernate.
- Database: PostgreSQL 18.7.
- Migrations: Flyway 13.12.1 with Spring Boot integration.
- Frontend: React 19.3.1, TypeScript 7.0.2, Vite declared as `^8.3.6` (lockfile resolves 8.3.8), Node.js 24 and npm 11.

## Repository structure

```text
backend/                         Spring Boot application and Maven build
  src/main/java/                 Application and JPA mappings
  src/main/resources/
    application.properties       Database and persistence configuration
    db/migration/                Versioned Flyway SQL migrations
frontend/                        React, TypeScript and Vite application
docs/                             Requirements, design and project documentation
openapi.yaml                      Proposed REST API contract
.env.example                      Fictional local configuration template
```

## Prerequisites

- Git.
- Java Development Kit 21.
- Maven (required for the `mvn` commands below; no Maven Wrapper is included).
- PostgreSQL 18 (or a compatible PostgreSQL installation).
- Node.js 24 and npm 11.
- PowerShell examples below assume Windows. Commands can be adapted for another shell.

## Local database setup

Create an application role and database on a fresh local PostgreSQL installation. Run these commands in `psql` as a local PostgreSQL administrator. `\password matriz_app` prompts for the role password without putting it in SQL or shell history.

```sql
CREATE ROLE matriz_app LOGIN;
```

In the same `psql` session, set the password interactively and create the database:

```text
\password matriz_app
```

```sql
CREATE DATABASE matriz_db OWNER matriz_app;
```

Configure the backend environment in the same PowerShell window from which it will be started:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/matriz_db'
$env:DB_USERNAME = 'matriz_app'
$securePassword = Read-Host 'Enter the local matriz_app password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$securePassword.Dispose()
```

Alternatively, set the three variables in the IntelliJ IDEA run configuration. Spring Boot does **not** automatically load `.env` files. Provide environment variables through the shell/process environment or the IDE run configuration. `.env.example` documents the expected variable names and URL using fictional values; it is a reference template, not a credential source. A developer may copy it as a reminder, but must provide actual values through a supported environment-variable mechanism and keep any real `.env` file uncommitted. The repository `.gitignore` excludes real `.env` files and permits `.env.example`.

## Start the backend

From the repository root in PowerShell, after setting the variables above:

```powershell
cd backend
mvn spring-boot:run
```

The backend listens on port 8080. Flyway applies pending versioned migrations from `backend/src/main/resources/db/migration` automatically at startup. A successful migration is recorded in `flyway_schema_history` and is not reapplied on each startup. Normal application restarts therefore do not reset existing data. Hibernate uses schema validation (`ddl-auto=validate`); it does not create or reset the schema.

V1 creates `users`, `projects`, `tasks`, `task_assignments` and `comments`. V2 inserts reproducible demonstration data: two users, two projects with different owners, two tasks, one assignment from each task to a user other than that project's owner (two cross-owner assignments), and one comment. Demo users `demo.alex` and `demo.sam` have no password hashes and cannot be used to log in.

## Start and build the frontend

Open a separate PowerShell window:

```powershell
cd frontend
npm install
npm run dev
```

Vite prints the local development URL. To create the production build:

```powershell
npm run build
```

## Inspect the database

In pgAdmin, connect to the local PostgreSQL server, open `matriz_db`, and inspect `public` tables. `flyway_schema_history` shows migration versions and success status; the five application tables contain the demonstration records.

The same data can be inspected with `psql`:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT username, display_name FROM users ORDER BY username;
SELECT name, owner_id FROM projects ORDER BY name;
SELECT title, status, priority FROM tasks ORDER BY title;
SELECT task_id, user_id FROM task_assignments ORDER BY task_id, user_id;
SELECT content, created_at FROM comments ORDER BY created_at;
```

## Limitations and project decisions

Authentication, functional project/task workflows, assignments/comments through application endpoints, search, filtering and dashboard behavior are not implemented. The API document is a proposed contract, not evidence of implemented endpoints. Detailed behaviors in requirements and user stories are project implementation decisions, not additional client-confirmed requirements. Account lifecycle, session/CSRF details, deployment, and non-functional targets remain unresolved.

See `docs/requirements.md`, `docs/user-stories.md` and the other files in `docs/` for scope, assumptions and open questions.
