# Matriz

Matriz is a project and task management application intended for work teams within a company. The client brief confirms the need to organise projects, manage tasks and follow work progress; detailed workflows and business rules remain project decisions or unresolved questions.

## Current implementation

The repository contains a runnable Spring Boot backend with PostgreSQL persistence, JPA mappings, Flyway migrations, reproducible demonstration records, session-based authentication and project-management endpoints. Authenticated users can list, search, view and create projects; only project owners can update or delete them, and projects with tasks cannot be deleted. The React/TypeScript/Vite frontend remains a starter scaffold; task, assignment, comment and dashboard workflows have not been implemented.

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
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The backend listens on port 8080. The local profile sets the session cookie's `Secure` attribute to false for local HTTP development. Outside that profile, `Secure` defaults to true; deployments can override it through `MATRIZ_SESSION_COOKIE_SECURE` when required. The `JSESSIONID` cookie is HttpOnly and SameSite=Lax. Flyway applies pending versioned migrations from `backend/src/main/resources/db/migration` automatically at startup. A successful migration is recorded in `flyway_schema_history` and is not reapplied on each startup. Normal application restarts therefore do not reset existing data. Hibernate uses schema validation (`ddl-auto=validate`); it does not create or reset the schema.

Spring Security requires authentication for protected requests. Authentication endpoints are:

- `GET /api/auth/csrf` returns JSON containing the BREACH-protected `token` exposed by Spring Security's XOR request handler and `headerName: X-CSRF-TOKEN`.
- `POST /api/auth/login` accepts a JSON `username` and `password`, establishes an authenticated server-side session and returns the user profile.
- `GET /api/auth/me` returns the authenticated user's ID, username and display name.
- `POST /api/auth/logout` invalidates the authenticated session and returns `204`; unauthenticated requests receive `401`.

Send the token returned by `/api/auth/csrf` unchanged in the `X-CSRF-TOKEN` header on login, logout and every other state-changing request. Login and logout invalidate the previous CSRF token; obtain a fresh token afterward. Project endpoints are available under `/api/projects`; task, assignment and comment workflows are not yet implemented.

V1 creates `users`, `projects`, `tasks`, `task_assignments` and `comments`. V2 inserts reproducible demonstration data: two users, two projects with different owners, two tasks, one assignment from each task to a user other than that project's owner (two cross-owner assignments), and one comment. V3 disables credentials for the two demo users and makes `password_hash` non-null. The migration contains no usable demo credentials. On the next backend startup Flyway applies V3 if it is pending.

### Optional local demo-account provisioning

Demo accounts cannot log in by default. To use them locally, first let the backend apply V3 to your local database, then run the separate provisioning entry point from `backend`:

```powershell
mvn spring-boot:run "-Dspring-boot.run.main-class=com.matriz.users.LocalDemoAccountProvisioningApplication"
```

The command requires a loopback PostgreSQL URL (`localhost`, `127.0.0.1` or `::1`), activates the local profile, does not run Flyway, and prompts for each demo password without echoing it. Choose fictional local-development-only passwords; the application stores BCrypt hashes. This command is separate from ordinary backend startup. The local profile enables authentication only for the two reserved demo usernames, `demo.alex` and `demo.sam`; ordinary configuration keeps those accounts disabled. Future non-demo accounts are not disabled by this flag.

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

Task, assignment and comment workflows, task filtering and dashboard behavior are not implemented. Project operations are implemented as described above; other API operations remain planned. Detailed behaviors in requirements and user stories are project implementation decisions, not additional client-confirmed requirements. Account lifecycle, deployment and non-functional targets remain unresolved; session and CSRF defaults are configured as described above.

See `docs/requirements.md`, `docs/user-stories.md` and the other files in `docs/` for scope, assumptions and open questions.
