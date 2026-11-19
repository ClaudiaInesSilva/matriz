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
  Dockerfile                     Backend Docker image
  src/main/java/                 Application and JPA mappings
  src/main/resources/
    application.properties       Database and persistence configuration
    db/migration/                Versioned Flyway SQL migrations
database/
  init/                          Database initialisation scripts for Compose
frontend/                        React, TypeScript and Vite application
  Dockerfile                     Frontend Docker image
docs/                             Requirements, design and project documentation
compose.yaml                     Docker Compose services
compose.provisioning.yaml        Compose override for demo-account provisioning
openapi.yaml                      Proposed REST API contract
.env.example                      Configuration template for local and Compose
```

## Prerequisites

- [Git for Windows](https://git-scm.com/download/win).
- [Java Development Kit 21](https://adoptium.net/temurin/releases/?version=21&os=any&arch=x64).
- [Apache Maven](https://maven.apache.org/install.html) installed and available as `mvn`. The repository does not include a Maven
  Wrapper or pin a Maven version.
- [PostgreSQL for Windows](https://www.postgresql.org/download/windows/) 18 (18.7 is the version verified for this project), with its service
  running. The PostgreSQL command-line client (`psql`) is needed for the local
  database setup below.
- [Node.js 24](https://nodejs.org/en/download) (npm 11 is included with the selected Node.js installation).
- [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/) for running the application with Docker Compose.

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

The backend also includes a `render` Spring profile (`application-render.properties`)
for Render deployment preparation. This profile enables demo-account auto-login
and sets the session cookie `Secure` attribute. Render deployment has not yet
been completed; the profile and supporting Nginx configuration are prepared but
not deployed. See `docs/deployment-plan.md` for details.

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

#### Compose database demo-account provisioning

The Compose database uses a separate, strict provisioning launcher. It accepts
only `matriz_compose` through `127.0.0.1:5433` and the `matriz_compose_app`
role. It does not use the PostgreSQL administrator account. All commands below
run from the repository root unless stated otherwise. Use the temporary
loopback-only Compose override to expose the database port:

```powershell
docker compose -f compose.yaml -f compose.provisioning.yaml up -d --force-recreate database
```

In an interactive PowerShell session, enter the application-role password at
the secure prompt, then run the dedicated launcher from the `backend` directory.
Passwords are not command arguments; the launcher prompts interactively for
demo-account passwords.

```powershell
$securePassword = Read-Host 'Enter the local matriz_compose_app password' -AsSecureString
try {
    $env:DB_URL = 'jdbc:postgresql://127.0.0.1:5433/matriz_compose'
    $env:DB_USERNAME = 'matriz_compose_app'
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password

    Set-Location .\backend
    mvn spring-boot:run "-Dspring-boot.run.main-class=com.matriz.users.ComposeDemoAccountProvisioningApplication"
    if ($LASTEXITCODE -ne 0) {
        throw 'Compose demo-account provisioning failed.'
    }
}
finally {
    Remove-Item Env:DB_URL, Env:DB_USERNAME, Env:DB_PASSWORD -ErrorAction SilentlyContinue
    $securePassword.Dispose()
}
```

After provisioning, return to the repository root and recreate the database
service with the normal Compose configuration to remove the published port.
This keeps the `matriz_compose_data` volume; do not use `down -v`.

```powershell
Set-Location ..
docker compose -f compose.yaml up -d --force-recreate database
```

Do not point this launcher at `matriz_db` or `matriz_test`. The Compose-specific
guard checks the final datasource URL and username, explicit URL and username
overrides, and the existing datasource redirection protections before
Spring can create a datasource.

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

## Run with Docker Compose

This section describes how to run the complete application using Docker Compose
on a fresh clone. The Compose stack includes three services: `database`
(PostgreSQL), `backend` (Spring Boot) and `frontend` (nginx serving the
compiled React application). The frontend is accessible at
`http://localhost:8081`.

### Prerequisites

Docker Desktop for Windows must be installed and running. The commands below
use Windows PowerShell from the repository root.

### 1. Configure environment variables

Copy the example environment file and replace the Compose password placeholders
with locally chosen values:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and replace both Compose passwords at the bottom of the file:

- `MATRIZ_COMPOSE_ADMIN_PASSWORD` — password for the PostgreSQL administrator
  role inside the Compose database container.
- `MATRIZ_COMPOSE_DB_PASSWORD` — password for the application role
  `matriz_compose_app` that the backend uses to connect.

Choose strong, local-only passwords. The `.env` file must not be committed to
Git; it is listed in `.gitignore`.

### 2. Build the Docker images

Build the backend and frontend images from the repository root:

```powershell
docker build -t matriz-backend:0.1.0 ./backend
docker build -t matriz-frontend:0.1.0 ./frontend
```

### 3. Start the database

Start only the database service:

```powershell
docker compose up -d database
```

Wait for the database to become healthy before continuing. Check the status
with `docker compose ps`.

### 4. Run the Flyway migrations

Flyway migrations run when the Spring Boot backend starts, not when the
database container starts. Start the backend once so it creates the schema
and seeds `demo.alex` and `demo.sam`, then stop it:

```powershell
docker compose up -d backend
```

Wait for the backend to become healthy, then stop it:

```powershell
docker compose stop backend
```

### 5. Provision the demo accounts

The database migrations create `demo.alex` and `demo.sam` without usable
passwords. Use the Compose-specific provisioning procedure to set passwords
interactively. The provisioning override temporarily exposes PostgreSQL only on
`127.0.0.1:5433` so the provisioning launcher can reach the container.

Follow the instructions in the **Compose database demo-account provisioning**
subsection under **Demo-account provisioning** above. That section runs from
the repository root and transitions to the `backend` directory before
executing the Maven command. In short:

1. Start the database with the provisioning override to expose the port.
2. Run the provisioning launcher from the `backend` directory, which prompts
   for passwords for each demo account.
3. Return to the repository root and recreate the database with the normal
   configuration to remove the exposed port.

Choose your own passwords for `demo.alex` and `demo.sam`; no default demo
passwords are provided. After provisioning, the passwords are stored as hashes
in the database.

### 6. Start the complete application

Start all three Compose services:

```powershell
docker compose up -d
```

Wait for the backend and frontend to become healthy. The frontend is available
at `http://localhost:8081`.

### 7. Sign in

Open `http://localhost:8081` and sign in with either `demo.alex` or
`demo.sam` using the password you set during provisioning.

### Stopping the application

To stop the services without removing the database volume, run:

```powershell
docker compose down
```

Do not use `docker compose down -v`, because it deletes the `matriz_compose_data`
volume and all application data.

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
- **Compose services will not start:** Confirm Docker Desktop is running and
  check `docker compose ps` for service health status. Ensure `.env` exists
  and both `MATRIZ_COMPOSE_ADMIN_PASSWORD` and `MATRIZ_COMPOSE_DB_PASSWORD`
  have been replaced with real values.
- **Compose demo login is rejected:** Run the provisioning procedure described
  in the **Provision the demo accounts** step above, then sign in with the
  password you set. The database migrations create the accounts without usable
  passwords.
- **Compose database connection errors after recreating the container:** Stale
  backend connections may appear briefly. Recreate the backend service with
  `docker compose up -d --force-recreate backend` to re-establish connections.

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

The frontend implements authentication, dashboard, project, task, assignment and comment workflows. Detailed behaviors in requirements and user stories are project implementation decisions, not additional client-confirmed requirements. Account lifecycle, deployment and non-functional targets remain unresolved; session and CSRF defaults are configured as described above. The frontend includes Playwright end-to-end tests covering authentication and core workflows, which also run in the GitHub Actions CI pipeline.

See `docs/requirements.md`, `docs/user-stories.md` and the other files in `docs/` for scope, assumptions and open questions.
