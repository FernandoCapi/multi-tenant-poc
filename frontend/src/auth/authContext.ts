import { createContext } from 'react'
import type { MeResponse } from '../api/types'

export interface AuthState {
  me: MeResponse | null
  loading: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthState | null>(null)
