import { createContext } from 'react'
import type { AuthenticatedUser } from '../api/apiClient'

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated' | 'error'

export interface AuthContextValue {
  status: AuthStatus
  user: AuthenticatedUser | null
  error: string | null
  checkSession: () => Promise<void>
  signIn: (username: string, password: string) => Promise<void>
  signOut: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
