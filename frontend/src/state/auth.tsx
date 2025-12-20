import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

const STORAGE_KEY = 'moodverse.isUnlocked'

type AuthContextValue = {
  isUnlocked: boolean
  status: string | null
  setUnlocked: (value: boolean, status?: string | null) => void
  clear: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

type AuthProviderProps = {
  children: ReactNode
}

export const AuthProvider = ({ children }: AuthProviderProps) => {
  const [isUnlocked, setIsUnlocked] = useState(false)
  const [status, setStatus] = useState<string | null>(null)

  useEffect(() => {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    if (stored === 'true') {
      setIsUnlocked(true)
    }
  }, [])

  const setUnlocked = (value: boolean, nextStatus: string | null = null) => {
    setIsUnlocked(value)
    setStatus(nextStatus)

    if (value) {
      sessionStorage.setItem(STORAGE_KEY, 'true')
    } else {
      sessionStorage.removeItem(STORAGE_KEY)
    }
  }

  const clear = () => {
    setUnlocked(false, null)
  }

  const contextValue = useMemo(
    () => ({
      isUnlocked,
      status,
      setUnlocked,
      clear,
    }),
    [isUnlocked, status],
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
