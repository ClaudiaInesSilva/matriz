import { LoginForm } from '../components/LoginForm'

export function LoginPage() {
  return (
    <main className="page-shell">
      <section className="login-card" aria-labelledby="login-title">
        <div className="brand" aria-label="Matriz">
          <span className="brand-mark" aria-hidden="true">
            M
          </span>
          <span>Matriz</span>
        </div>
        <div className="login-intro">
          <p className="eyebrow">Your team workspace</p>
          <h1 id="login-title">Welcome back</h1>
          <p className="supporting-copy">Sign in with your Matriz account to continue.</p>
        </div>
        <LoginForm />
      </section>
      <p className="page-caption">Secure sign-in with your Matriz account</p>
    </main>
  )
}
