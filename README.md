# Matriz

Matriz is a project and task management application intended for work teams within a company. The client brief confirms the need to organise projects, manage tasks and follow work progress; detailed workflows and business rules remain project decisions or unresolved questions.

## Current implementation

The application provides session-based authentication and a React frontend for the dashboard, project and task management, assignments, and comments. The Spring Boot backend provides the corresponding APIs and PostgreSQL persistence through JPA and Flyway. The dashboard aggregates project and task data from the existing APIs; there is no separate dashboard API endpoint.

The backend uses authenticated sessions and CSRF protection. The local demo users are seeded without usable passwords; set local-only passwords using the provisioning instructions below before signing in. The frontend uses Vite's development proxy to call the backend. The three needs confirmed by the original client brief are to organise projects, manage tasks, and follow work progress; detailed behavior below reflects project decisions and is not additional client-confirmed scope.

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

- [Git for Windows](https://git-scm.com/download/win).
- [Java Development Kit 21](https://adoptium.net/temurin/releases/?version=21&os=any&arch=x64).
- [Apache Maven](https://maven.apache.org/install.html) installed and available as `mvn`. The repository does not include a Maven
  Wrapper or pin a Maven version.
- [PostgreSQL for Windows](https://www.postgresql.org/download/windows/) 18 (18.7 is the version verified for this project), with its service
  running. The PostgreSQL command-line client (`psql`) is needed for the setup
  commands below.
- [Node.js 24](https://nodejs.org/en/download) (npm 11 is included with the selected Node.js installation).

The commands below are for Windows PowerShell, run from the cloned repository
root unless a step says otherwise. Check that the tools are available:

```powershell
git --version
java -version
mvn -version
psql --version
node --version
npm --version
```

## Local database setup

Start the PostgreSQL Windows service if it is not already running. Open
**Services** from the Start menu (or run `services.msc`), find the PostgreSQL
service installed on your computer, and start it.

Open an administrator `psql` session. For a standard installation whose
administrator role is `postgres`, run this in PowerShell:

```powershell
psql -U postgres -h localhost -p 5432 -d postgres
```

If your PostgreSQL administrator uses a different role, substitute that role.
You can also open the installed **SQL Shell (psql)** and connect as your
administrator. The following commands are for the `psql` prompt, not pgAdmin's
SQL editor. On a fresh database installation, create the application role,
choose its password at the secure prompt, then create the database:

```sql
CREATE ROLE matriz_app LOGIN;
```

```text
\password matriz_app
```

```sql
CREATE DATABASE matriz_db OWNER matriz_app;
```

If the role or database already exists, do not run its corresponding `CREATE`
command again or reset existing data; use the existing local setup. Keep the
application role password private and never commit it to Git.

## Run the application

The backend, provisioning command and frontend run in **three separate
PowerShell sessions**. Values assigned using `$env:` are process-specific:
configure the database variables again in each new session that needs them.
Spring Boot does not load `.env` files automatically.

### 1. Backend application

In the first PowerShell session, configure the datasource and start the backend.
The password prompt is secure; the value is not part of the command text:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/matriz_db'
$env:DB_USERNAME = 'matriz_app'
$securePassword = Read-Host 'Enter the local matriz_app password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$securePassword.Dispose()

Set-Location .\backend
mvn spring-boot:run "-Dspring-boot.run.profiles=local"
```

The backend listens on `http://localhost:8080`. The `local` profile enables
local demo-account authentication and configures the session cookie for local
HTTP development. At startup, Flyway applies any pending migrations from
`backend/src/main/resources/db/migration`. Migrations are recorded and are not
reapplied on every startup; Hibernate validates the schema and does not create
or reset it. Leave this session running.

The backend datasource variables are `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`.
`MATRIZ_SESSION_COOKIE_SECURE` defaults to `true` outside the `local` profile;
the local profile disables the Secure cookie attribute for local HTTP.

### 2. Demo-account provisioning

The database migrations seed `demo.alex` and `demo.sam`, but do not give them
usable passwords. In a **second, interactive PowerShell session**, configure
the same database variables again; variables from the backend session are not
inherited by this separate window:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/matriz_db'
$env:DB_USERNAME = 'matriz_app'
$securePassword = Read-Host 'Enter the local matriz_app password' -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password
$securePassword.Dispose()

Set-Location .\backend
mvn spring-boot:run "-Dspring-boot.run.main-class=com.matriz.users.LocalDemoAccountProvisioningApplication"
```

The provisioning launcher activates both required Spring profiles,
`local` and `local-provisioning`, internally. It uses an interactive terminal
to prompt for a new password and confirmation for each account. Choose your
own local-only passwords for `demo.alex` and `demo.sam`; there are no default
demo passwords. The application stores password hashes, not plaintext
passwords. After provisioning succeeds, this session can be closed.

### 3. Frontend development server

In a **third PowerShell session**, install the locked frontend dependencies
and start Vite:

```powershell
Set-Location .\frontend
npm ci
npm run dev
```

Open the URL printed by Vite, normally
`http://localhost:5173`. If that port is in use, Vite may print a different
port. The development server proxies `/api` requests to
`http://localhost:8080`; both servers must be running. To use a different
backend address, set `VITE_API_PROXY_TARGET` in this PowerShell session before
starting Vite, for example:

```powershell
$env:VITE_API_PROXY_TARGET = 'http://localhost:8081'
```

Sign in with `demo.alex` or `demo.sam` and the password you chose for that
account during provisioning. There is no public registration or default demo
password. The frontend detects an existing session, displays the signed-in
user, and supports signing out. Session authentication uses an HttpOnly cookie;
the API client obtains and sends CSRF tokens for state-changing requests.

## Troubleshooting

- **PostgreSQL connection refused:** Start the PostgreSQL Windows service and
  confirm that the server listens on port `5432`.
- **Role or database does not exist:** Create `matriz_app` and `matriz_db` as
  described above, or correct the datasource variables to match your local
  database. Do not point local provisioning at a remote database.
- **Password authentication failed for `matriz_app`:** Check that
  `DB_PASSWORD` matches the password set for that PostgreSQL role, and that
  the variables were set in the PowerShell session running the command.
- **Demo login is rejected:** Run the backend successfully first so Flyway
  creates the seeded accounts, then run the interactive provisioning command
  and use the password set for the selected username.
- **Demo login still fails:** Start the normal backend with the `local` profile
  using the command above. The provisioning launcher activates its own
  `local` and `local-provisioning` profiles; do not use it to start the normal
  web application.
- **Provisioning reports that an interactive terminal is required:** Run the
  command in a regular interactive PowerShell window, not a background task,
  IDE process without a console, or redirected terminal.
- **Frontend cannot reach the backend:** Confirm the backend is running at
  `http://localhost:8080`. If it uses another host or port, set
  `VITE_API_PROXY_TARGET` before starting or restarting Vite.

## Build and checks

Run the frontend commands from `frontend`:

```powershell
npm run lint
npm run build
```

Run backend tests from `backend`:

```powershell
mvn test
```

The PostgreSQL persistence integration tests are opt-in and target only `jdbc:postgresql://127.0.0.1:5432/matriz_test`. They use the separate `matriz_test_app` role and must never be pointed at the development database `matriz_db`.

If the test role and database do not already exist, open an administrator `psql` session as described above and, only on a fresh setup, run:

```sql
CREATE ROLE matriz_test_app LOGIN;
```

At the `psql` prompt, set the role password interactively:

```text
\password matriz_test_app
```

Then create the dedicated test database owned by that role:

```sql
CREATE DATABASE matriz_test OWNER matriz_test_app;
```

Do not rerun `CREATE` commands for existing objects, and do not drop, reset, truncate, or reuse `matriz_db` for these tests. The integration test pins its JDBC URL to `matriz_test` and asserts `current_database()` is `matriz_test`.

To run the integration tests, open a PowerShell session, configure the test credentials without putting the password in command history, and run the targeted command from `backend`:

```powershell
$env:MATRIZ_TEST_DB_USERNAME = 'matriz_test_app'
$secureTestPassword = Read-Host 'Enter the local matriz_test_app password' -AsSecureString
$env:MATRIZ_TEST_DB_PASSWORD = [System.Net.NetworkCredential]::new('', $secureTestPassword).Password
$secureTestPassword.Dispose()

Set-Location .\backend
mvn "-Dtest=PostgresAssignmentCommentPersistenceTest" test
```

The full backend suite can be run from `backend` using `mvn test`. Without the test credentials, the opt-in PostgreSQL integration tests are skipped; with credentials set, they still connect only to the guarded `matriz_test` database.

## Limitations and project decisions

The frontend implements authentication, dashboard, project, task, assignment and comment workflows. Detailed behaviors in requirements and user stories are project implementation decisions, not additional client-confirmed requirements. Account lifecycle, deployment and non-functional targets remain unresolved; session and CSRF defaults are configured as described above. Frontend behavior has no automated test suite at this stage.

See `docs/requirements.md`, `docs/user-stories.md` and the other files in `docs/` for scope, assumptions and open questions.
