import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, setUnauthorizedHandler } from '../api/client'
import type { MeResponse } from '../api/types'
import { AuthContext } from './authContext'
import { clearToken, getToken, setToken } from './tokenStorage'

/** Holds the logged-in user; the family always comes from the token, through /api/me. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<MeResponse | null>(null)
  const [loading, setLoading] = useState(() => getToken() !== null)

  const logout = useCallback(() => {
    clearToken()
    setMe(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    if (getToken() === null) {
      return
    }
    api.me()
      .then(setMe)
      .catch(logout)
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await api.login(email, password)
    setToken(accessToken)
    setMe(await api.me())
  }, [])

  const value = useMemo(() => ({ me, loading, login, logout }), [me, loading, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
