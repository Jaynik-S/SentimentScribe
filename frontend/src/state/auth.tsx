import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { AuthUser, E2eeParams } from '../api/types'

const STORAGE_KEY = 'sentimentscribe.auth'

export type AuthSession = {
  accessToken: string
  user: AuthUser
  e2eeParams: E2eeParams
}

type AuthContextValue = {
  auth: AuthSession | null
  isAuthenticated: boolean
  setAuth: (value: AuthSession | null) => void
  clear: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

type AuthProviderProps = {
  children: ReactNode
}

const getSessionStorage = (): Storage | null => {
  if (typeof window === 'undefined') {
    return null
  }

  try {
    return window.sessionStorage
  } catch {
    return null
  }
}

const isValidSession = (value: unknown): value is AuthSession => {
  if (!value || typeof value !== 'object') {
    return false
  }

  const record = value as AuthSession

  return (
    typeof record.accessToken === 'string' &&
    record.accessToken.length > 0 &&
    typeof record.user?.id === 'string' &&
    typeof record.user?.username === 'string' &&
    typeof record.e2eeParams?.kdf === 'string' &&
    typeof record.e2eeParams?.salt === 'string' &&
    typeof record.e2eeParams?.iterations === 'number'
  )
}

const readStoredAuth = (): AuthSession | null => {
  const storage = getSessionStorage()
  if (!storage) {
    return null
  }

  const raw = storage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw)
    if (isValidSession(parsed)) {
      return parsed
    }
  } catch {
    // Fall through to cleanup.
  }

  storage.removeItem(STORAGE_KEY)
  return null
}

const writeStoredAuth = (value: AuthSession | null): void => {
  const storage = getSessionStorage()
  if (!storage) {
    return
  }

  if (!value) {
    storage.removeItem(STORAGE_KEY)
    return
  }

  storage.setItem(STORAGE_KEY, JSON.stringify(value))
}

export const getStoredAuth = (): AuthSession | null => readStoredAuth()

export const getAccessToken = (): string | null => {
  return readStoredAuth()?.accessToken ?? null
}

export const clearStoredAuth = (): void => {
  writeStoredAuth(null)
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [auth, setAuthState] = useState<AuthSession | null>(() => readStoredAuth())
  const setAuth = useCallback((value: AuthSession | null) => {
    setAuthState(value)
    writeStoredAuth(value)
  }, [])

  const clear = useCallback(() => {
    setAuth(null)
  }, [setAuth])

  const contextValue = useMemo(
    () => ({
      auth,
      isAuthenticated: Boolean(auth?.accessToken),
      setAuth,
      clear,
    }),
    [auth, clear, setAuth],
  )

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
