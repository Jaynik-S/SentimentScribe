import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type UiContextValue = {
  pageError: string | null
  pageErrorAction: PageErrorAction | null
  setPageError: (message: string | null, action?: PageErrorAction | null) => void
  clearPageError: () => void
  isPageLoading: boolean
  setPageLoading: (value: boolean) => void
}

export type PageErrorAction = {
  label: string
  onClick: () => void
}

const UiContext = createContext<UiContextValue | undefined>(undefined)

type UiProviderProps = {
  children: ReactNode
}

export const UiProvider = ({ children }: UiProviderProps) => {
  const [pageError, setPageErrorState] = useState<string | null>(null)
  const [pageErrorAction, setPageErrorAction] =
    useState<PageErrorAction | null>(null)
  const [isPageLoading, setPageLoadingState] = useState(false)

  const setPageError = useCallback(
    (message: string | null, action: PageErrorAction | null = null) => {
      setPageErrorState(message)
      setPageErrorAction(message ? action : null)
    },
    [],
  )

  const clearPageError = useCallback(() => {
    setPageErrorState(null)
    setPageErrorAction(null)
  }, [])

  const setPageLoading = useCallback((value: boolean) => {
    setPageLoadingState(value)
  }, [])

  const contextValue = useMemo(
    () => ({
      pageError,
      pageErrorAction,
      setPageError,
      clearPageError,
      isPageLoading,
      setPageLoading,
    }),
    [clearPageError, isPageLoading, pageError, pageErrorAction, setPageError, setPageLoading],
  )

  return <UiContext.Provider value={contextValue}>{children}</UiContext.Provider>
}

export const useUi = (): UiContextValue => {
  const context = useContext(UiContext)
  if (!context) {
    throw new Error('useUi must be used within UiProvider')
  }
  return context
}
