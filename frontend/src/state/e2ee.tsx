import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { deriveAesKey } from '../crypto/kdf'
import type { E2eeParams } from '../api/types'
import { useAuth } from './auth'

type E2eeContextValue = {
  key: CryptoKey | null
  isUnlocked: boolean
  unlock: (passphrase: string, params: E2eeParams) => Promise<void>
  clear: () => void
}

const E2eeContext = createContext<E2eeContextValue | undefined>(undefined)

type E2eeProviderProps = {
  children: ReactNode
}

export const E2eeProvider = ({ children }: E2eeProviderProps) => {
  const [key, setKey] = useState<CryptoKey | null>(null)
  const { auth } = useAuth()

  useEffect(() => {
    if (!auth) {
      setKey(null)
    }
  }, [auth])

  const clear = useCallback(() => {
    setKey(null)
  }, [])

  const unlock = useCallback(async (passphrase: string, params: E2eeParams) => {
    if (!passphrase.trim()) {
      throw new Error('Passphrase is required.')
    }
    if (!params?.salt || !params?.iterations) {
      throw new Error('E2EE parameters are missing.')
    }

    const derived = await deriveAesKey(passphrase, params.salt, params.iterations)
    setKey(derived)
  }, [])

  const contextValue = useMemo(
    () => ({
      key,
      isUnlocked: Boolean(key),
      unlock,
      clear,
    }),
    [clear, key, unlock],
  )

  return <E2eeContext.Provider value={contextValue}>{children}</E2eeContext.Provider>
}

export const useE2ee = (): E2eeContextValue => {
  const context = useContext(E2eeContext)
  if (!context) {
    throw new Error('useE2ee must be used within E2eeProvider')
  }
  return context
}
