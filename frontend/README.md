# Matriz frontend

The frontend implements authentication and session handling, a dashboard with project/task summaries, project search and management, task management and filtering, assignments, and comments. It uses the existing backend APIs; dashboard counts are computed by the frontend rather than provided by a separate dashboard endpoint.

The root [README](../README.md) is the main guide for a fresh Windows setup,
including PostgreSQL, backend startup, demo-account provisioning, and the
three separate PowerShell sessions.

## Run the frontend

After starting the backend and provisioning a password for `demo.alex` and/or
`demo.sam`, open the third PowerShell session at the repository root:

```powershell
Set-Location .\frontend
npm ci
npm run dev
```

Open the URL printed by Vite, normally `http://localhost:5173`. Vite proxies
`/api` requests to `http://localhost:8080` by default, so keep the backend
running. To use a backend at another host or port, set
`VITE_API_PROXY_TARGET` before starting Vite, for example:

```powershell
$env:VITE_API_PROXY_TARGET = 'http://localhost:8081'
```

The API base path defaults to `/api`. Do not put credentials in frontend
environment variables.

Authentication uses the backend session cookie. The client includes browser
credentials and obtains a fresh CSRF token before each state-changing request.
Passwords and session credentials are not stored in browser storage.

Run frontend checks from this directory:

```powershell
npm run lint
npm run build
```
