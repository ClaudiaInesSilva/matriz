import { useCallback, useEffect, useState } from 'react'
import { useAuth } from './auth/useAuth'
import { Feedback } from './components/Feedback'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { ProjectPage } from './pages/ProjectPage'
import { TaskPage } from './pages/TaskPage'
import './App.css'

type Route =
  | { page: 'dashboard' }
  | { page: 'project'; projectId: string }
  | { page: 'task'; taskId: string }

function getRoute(hash: string): Route {
  const segments = hash.replace(/^#\/?/, '').split('/').filter(Boolean)
  try {
    if (segments.length === 2 && segments[0] === 'projects') {
      return { page: 'project', projectId: decodeURIComponent(segments[1]) }
    }
    if (segments.length === 2 && segments[0] === 'tasks') {
      return { page: 'task', taskId: decodeURIComponent(segments[1]) }
    }
  } catch {
    return { page: 'dashboard' }
  }
  return { page: 'dashboard' }
}

function App() {
  const { status, user, error, signOut, checkSession } = useAuth()
  const [logoutError, setLogoutError] = useState<string | null>(null)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [route, setRoute] = useState<Route>(() => getRoute(window.location.hash))
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    function syncRoute() {
      setRoute(getRoute(window.location.hash))
    }
    window.addEventListener('hashchange', syncRoute)
    return () => window.removeEventListener('hashchange', syncRoute)
  }, [])

  const onDataChanged = useCallback(() => setRevision((value) => value + 1), [])
  const onUnauthorized = useCallback(() => {
    void checkSession()
  }, [checkSession])

  async function handleLogout() {
    setLogoutError(null)
    setIsLoggingOut(true)

    try {
      await signOut()
    } catch {
      setLogoutError('Unable to log out. Please try again.')
    } finally {
      setIsLoggingOut(false)
    }
  }

  if (status === 'loading') {
    return (
      <main className="page-shell">
        <p className="loading-message" role="status">
          Checking your session...
        </p>
      </main>
    )
  }

  if (status === 'error') {
    return (
      <main className="page-shell">
        <section className="message-card" aria-labelledby="connection-title">
          <div className="brand" aria-label="Matriz">
            <span className="brand-mark" aria-hidden="true">M</span>
            <span>Matriz</span>
          </div>
          <h1 id="connection-title">Can't connect to Matriz</h1>
          <p className="form-error" role="alert">
            {error ?? 'Please check that the backend is running and try again.'}
          </p>
          <button className="button button-primary" type="button" onClick={() => void checkSession()}>
            Try again
          </button>
        </section>
      </main>
    )
  }

  if (status === 'unauthenticated' || !user) {
    return <LoginPage />
  }

  return (
    <div className="app-shell">
      <header className="app-header">
        <a className="brand brand-link" href="#/" aria-label="Matriz — painel">
          <span className="brand-mark" aria-hidden="true">M</span>
          <span>Matriz</span>
        </a>
        <nav className="main-nav" aria-label="Navegação principal">
          <a className={route.page === 'dashboard' ? 'nav-link active' : 'nav-link'} href="#/">Painel</a>
        </nav>
        <div className="user-actions">
          <div className="user-identity">
            <span className="user-name">{user.displayName}</span>
            <span className="user-username">{user.username}</span>
          </div>
          <button
            className="button button-secondary"
            type="button"
            onClick={() => void handleLogout()}
            disabled={isLoggingOut}
          >
            {isLoggingOut ? 'A terminar sessão…' : 'Terminar sessão'}
          </button>
        </div>
      </header>
      <main className="app-content">
        {logoutError && <Feedback>{logoutError}</Feedback>}
        {route.page === 'dashboard' ? (
          <DashboardPage
            user={user}
            revision={revision}
            onDataChanged={onDataChanged}
            onUnauthorized={onUnauthorized}
          />
        ) : route.page === 'project' ? (
          <ProjectPage
            projectId={route.projectId}
            userId={user.userId}
            revision={revision}
            onDataChanged={onDataChanged}
            onUnauthorized={onUnauthorized}
          />
        ) : (
          <TaskPage
            taskId={route.taskId}
            revision={revision}
            onDataChanged={onDataChanged}
            onUnauthorized={onUnauthorized}
          />
        )}
      </main>
    </div>
  )
}

export default App
