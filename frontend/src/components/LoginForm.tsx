import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/useAuth'

const DEMO_LOGIN_ENABLED = import.meta.env.VITE_DEMO_LOGIN_ENABLED === 'true'

export function LoginForm() {
  const { signIn, signInAsDemo } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setError(null)

    if (!username.trim() || !password) {
      setError('Enter your username and password.')
      return
    }

    setIsSubmitting(true)
    try {
      await signIn(username, password)
    } catch (submitError) {
      setPassword('')
      setError(submitError instanceof Error ? submitError.message : 'Unable to sign in. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleDemoLogin(demoUsername: string) {
    setError(null)
    setIsSubmitting(true)
    try {
      await signInAsDemo(demoUsername)
    } catch (demoError) {
      setError(demoError instanceof Error ? demoError.message : 'Unable to sign in as demo user. Please try again.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form className="login-form" onSubmit={(event) => void handleSubmit(event)} noValidate>
      <div className="field">
        <label htmlFor="username">Username</label>
        <input
          id="username"
          name="username"
          type="text"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          required
          disabled={isSubmitting}
        />
      </div>
      <div className="field">
        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          required
          disabled={isSubmitting}
        />
      </div>
      {error && (
        <p className="form-error" role="alert">
          {error}
        </p>
      )}
      <button className="button button-primary login-submit" type="submit" disabled={isSubmitting}>
        {isSubmitting ? 'Signing in...' : 'Sign in'}
      </button>
      {DEMO_LOGIN_ENABLED && (
        <>
          <div className="login-divider">
            <span>or</span>
          </div>
          <div className="demo-login-section">
            <p className="demo-login-warning">
              These are shared demo accounts. Changes you make may be visible to other visitors.
            </p>
            <div className="demo-login-buttons">
              <button
                type="button"
                className="button button-secondary demo-button"
                disabled={isSubmitting}
                onClick={() => void handleDemoLogin('demo.alex')}
              >
                Enter as Alex
              </button>
              <button
                type="button"
                className="button button-secondary demo-button"
                disabled={isSubmitting}
                onClick={() => void handleDemoLogin('demo.sam')}
              >
                Enter as Sam
              </button>
            </div>
          </div>
        </>
      )}
    </form>
  )
}
