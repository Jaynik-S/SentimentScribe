import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import type { RecommendationResponse } from '../api/types'

type RecommendationsContextValue = {
  recommendations: RecommendationResponse | null
  setRecommendations: (data: RecommendationResponse | null) => void
  clearRecommendations: () => void
}

const RecommendationsContext = createContext<RecommendationsContextValue | undefined>(
  undefined,
)

type RecommendationsProviderProps = {
  children: ReactNode
}

export const RecommendationsProvider = ({
  children,
}: RecommendationsProviderProps) => {
  const [recommendations, setRecommendationsState] =
    useState<RecommendationResponse | null>(null)

  const setRecommendations = (data: RecommendationResponse | null) => {
    setRecommendationsState(data)
  }

  const clearRecommendations = () => {
    setRecommendationsState(null)
  }

  const contextValue = useMemo(
    () => ({
      recommendations,
      setRecommendations,
      clearRecommendations,
    }),
    [recommendations],
  )

  return (
    <RecommendationsContext.Provider value={contextValue}>
      {children}
    </RecommendationsContext.Provider>
  )
}

export const useRecommendations = (): RecommendationsContextValue => {
  const context = useContext(RecommendationsContext)
  if (!context) {
    throw new Error(
      'useRecommendations must be used within RecommendationsProvider',
    )
  }
  return context
}
