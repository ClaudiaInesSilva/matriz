import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { ApiError, apiClient, type AuthenticatedUser } from '../api/apiClient'
import { AuthContext, type AuthStatus } from './authContext'

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : 'An unexpected error occurred.'
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>('loading')
  const [user, setUser] = useState<AuthenticatedUser | null>(null)
  const [error, setError] = useState<string | null>(null)

  const loadCurrentUser = useCallback(async () => {
    try {
      const currentUser = await apiClient.getCurrentUser()
      setUser(currentUser)
      setStatus('authenticated')
    } catch (checkError) {
      setUser(null)
      if (checkError instanceof ApiError && checkError.status === 401) {
        setStatus('unauthenticated')
        return
      }

      setError(errorMessage(checkError))
      setStatus('error')
    }
  }, [])

  const checkSession = useCallback(async () => {
    setStatus('loading')
    setError(null)
    await loadCurrentUser()
  }, [loadCurrentUser])

  useEffect(() => {
    // Load the server-owned identity before rendering authenticated content.
    // oxlint-disable-next-line react/set-state-in-effect
    void loadCurrentUser()
  }, [loadCurrentUser])

  const signIn = useCallback(async (username: string, password: string) => {
    setError(null)

    try {
      const authenticatedUser = await apiClient.login(username, password)
      setUser(authenticatedUser)
      setStatus('authenticated')
    } catch (loginError) {
      setUser(null)
      setStatus('unauthenticated')
      throw loginError
    }
  }, [])

  const signOut = useCallback(async () => {
    try {
      await apiClient.logout()
    } catch (logoutError) {
      if (!(logoutError instanceof ApiError) || logoutError.status !== 401) {
        throw logoutError
      }
    }

    setUser(null)
    setError(null)
    setStatus('unauthenticated')
  }, [])

  const value = useMemo(
    () => ({ status, user, error, checkSession, signIn, signOut }),
    [status, user, error, checkSession, signIn, signOut],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
