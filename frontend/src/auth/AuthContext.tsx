import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import {
  clearTokens,
  hasStoredSession,
  onSessionExpired,
  readRefreshToken,
  refreshSession,
  storeTokens,
} from '../api/client'
import { auth } from '../api/endpoints'
import type { GuildRole, Me } from '../api/types'

type Status = 'loading' | 'anonymous' | 'authenticated'

interface AuthValue {
  status: Status
  user: Me | null
  isAdmin: boolean
  /** true - naudotojas pats atsijunge (ne pasibaigusi sesija): grizti i pradzia, ne i paskutini puslapi. */
  signedOut: boolean
  roleIn: (guildId: number) => GuildRole | null
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  reloadUser: () => Promise<void>
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<Me | null>(null)
  const [status, setStatus] = useState<Status>(hasStoredSession() ? 'loading' : 'anonymous')
  const [signedOut, setSignedOut] = useState(false)

  const reset = useCallback(() => {
    clearTokens()
    setUser(null)
    setStatus('anonymous')
    queryClient.clear()
  }, [queryClient])

  const reloadUser = useCallback(async () => {
    setUser(await auth.me())
    setStatus('authenticated')
  }, [])

  // Atidarius puslapi: jei yra issaugotas refresh zetonas - atkuriam sesija.
  useEffect(() => {
    if (!hasStoredSession()) return
    let cancelled = false
    refreshSession()
      .then((ok) => (ok ? auth.me() : null))
      .then((me) => {
        if (cancelled) return
        if (me) {
          setUser(me)
          setStatus('authenticated')
        } else {
          reset()
        }
      })
      .catch(() => !cancelled && reset())
    return () => {
      cancelled = true
    }
  }, [reset])

  useEffect(() => {
    onSessionExpired(reset)
    return () => onSessionExpired(null)
  }, [reset])

  const login = useCallback(
    async (username: string, password: string) => {
      storeTokens(await auth.login(username, password))
      setSignedOut(false)
      queryClient.clear()
      await reloadUser()
    },
    [queryClient, reloadUser],
  )

  const logout = useCallback(async () => {
    const refreshToken = readRefreshToken()
    if (refreshToken) await auth.logout(refreshToken).catch(() => undefined)
    setSignedOut(true)
    reset()
  }, [reset])

  const value = useMemo<AuthValue>(
    () => ({
      status,
      user,
      isAdmin: user?.role === 'ADMIN',
      signedOut,
      roleIn: (guildId) => user?.memberships.find((m) => m.guildId === guildId)?.role ?? null,
      login,
      logout,
      reloadUser,
    }),
    [status, user, signedOut, login, logout, reloadUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth naudojamas be AuthProvider')
  return value
}
