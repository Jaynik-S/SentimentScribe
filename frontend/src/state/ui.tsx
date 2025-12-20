import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'

export type UiContextValue = {
  pageError: string | null
  setPageError: (message: string | null) => void
  clearPageError: () => void
  isPageLoading: boolean
  setPageLoading: (value: boolean) => void
}

const UiContext = createContext<UiContextValue | undefined>(undefined)

type UiProviderProps = {
  children: ReactNode
}

export const UiProvider = ({ children }: UiProviderProps) => {
  const [pageError, setPageErrorState] = useState<string | null>(null)
  const [isPageLoading, setPageLoadingState] = useState(false)

  const setPageError = (message: string | null) => {
    setPageErrorState(message)
  }

  const clearPageError = () => {
    setPageErrorState(null)
  }

  const setPageLoading = (value: boolean) => {
    setPageLoadingState(value)
  }

  const contextValue = useMemo(
    () => ({
      pageError,
      setPageError,
      clearPageError,
      isPageLoading,
      setPageLoading,
    }),
    [pageError, isPageLoading],
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
