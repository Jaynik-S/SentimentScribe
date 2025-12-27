import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { flushSyncQueue } from '../offline/syncEngine'
import { countSyncQueueByUser } from '../offline/syncQueueRepo'
import { useAuth } from './auth'

type OfflineContextValue = {
  isOffline: boolean
  pendingCount: number
  isSyncing: boolean
  syncNow: () => Promise<void>
  refreshPendingCount: () => Promise<void>
}

const OfflineContext = createContext<OfflineContextValue | undefined>(undefined)

type OfflineProviderProps = {
  children: ReactNode
}

const hasIndexedDb = (): boolean => typeof indexedDB !== 'undefined'

export const OfflineProvider = ({ children }: OfflineProviderProps) => {
  const { auth } = useAuth()
  const [isOffline, setIsOffline] = useState(() => {
    if (typeof navigator === 'undefined') {
      return false
    }
    return !navigator.onLine
  })
  const [pendingCount, setPendingCount] = useState(0)
  const [isSyncing, setIsSyncing] = useState(false)

  const userId = auth?.user?.id ?? null

  const refreshPendingCount = useCallback(async () => {
    if (!userId || !hasIndexedDb()) {
      setPendingCount(0)
      return
    }

    try {
      const count = await countSyncQueueByUser(userId)
      setPendingCount(count)
    } catch {
      setPendingCount(0)
    }
  }, [userId])

  const syncNow = useCallback(async () => {
    if (!userId || !hasIndexedDb() || isSyncing) {
      return
    }

    setIsSyncing(true)
    try {
      await flushSyncQueue(userId)
    } finally {
      setIsSyncing(false)
      await refreshPendingCount()
    }
  }, [isSyncing, refreshPendingCount, userId])

  useEffect(() => {
    if (!userId) {
      setPendingCount(0)
      return
    }

    void refreshPendingCount()
  }, [refreshPendingCount, userId])

  useEffect(() => {
    if (typeof window === 'undefined') {
      return
    }

    const handleOnline = () => {
      setIsOffline(false)
      void refreshPendingCount()
    }
    const handleOffline = () => {
      setIsOffline(true)
    }

    setIsOffline(!navigator.onLine)
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)

    return () => {
      window.removeEventListener('online', handleOnline)
      window.removeEventListener('offline', handleOffline)
    }
  }, [refreshPendingCount])

  const contextValue = useMemo(
    () => ({
      isOffline,
      pendingCount,
      isSyncing,
      syncNow,
      refreshPendingCount,
    }),
    [isOffline, isSyncing, pendingCount, refreshPendingCount, syncNow],
  )

  return <OfflineContext.Provider value={contextValue}>{children}</OfflineContext.Provider>
}

export const useOffline = (): OfflineContextValue => {
  const context = useContext(OfflineContext)
  if (!context) {
    throw new Error('useOffline must be used within OfflineProvider')
  }
  return context
}
