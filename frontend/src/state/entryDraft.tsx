import { createContext, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { formatLocalDateTime } from '../api/localDateTime'
import type { LocalDateTime } from '../api/types'

type EntryDraft = {
  title: string
  text: string
  storagePath: string | null
  keywords: string[]
  createdAt: LocalDateTime | null
}

type EntryDraftContextValue = {
  draft: EntryDraft
  keywordsVisible: boolean
  setDraft: (draft: EntryDraft) => void
  updateDraft: (partial: Partial<EntryDraft>) => void
  setKeywordsVisible: (value: boolean) => void
  startNewEntry: () => void
}

const defaultDraft: EntryDraft = {
  title: '',
  text: '',
  storagePath: null,
  keywords: [],
  createdAt: null,
}

const EntryDraftContext = createContext<EntryDraftContextValue | undefined>(
  undefined,
)

type EntryDraftProviderProps = {
  children: ReactNode
}

export const EntryDraftProvider = ({ children }: EntryDraftProviderProps) => {
  const [draft, setDraftState] = useState<EntryDraft>(defaultDraft)
  const [keywordsVisible, setKeywordsVisibleState] = useState(false)

  const setDraft = (nextDraft: EntryDraft) => {
    setDraftState(nextDraft)
  }

  const updateDraft = (partial: Partial<EntryDraft>) => {
    setDraftState((current) => ({ ...current, ...partial }))
  }

  const setKeywordsVisible = (value: boolean) => {
    setKeywordsVisibleState(value)
  }

  const startNewEntry = () => {
    setDraftState({
      title: '',
      text: '',
      storagePath: null,
      keywords: [],
      createdAt: formatLocalDateTime(new Date()),
    })
    setKeywordsVisibleState(false)
  }

  const contextValue = useMemo(
    () => ({
      draft,
      keywordsVisible,
      setDraft,
      updateDraft,
      setKeywordsVisible,
      startNewEntry,
    }),
    [draft, keywordsVisible],
  )

  return (
    <EntryDraftContext.Provider value={contextValue}>
      {children}
    </EntryDraftContext.Provider>
  )
}

export const useEntryDraft = (): EntryDraftContextValue => {
  const context = useContext(EntryDraftContext)
  if (!context) {
    throw new Error('useEntryDraft must be used within EntryDraftProvider')
  }
  return context
}
