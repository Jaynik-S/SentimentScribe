import { analyzeText } from './analysis'
import { verifyPassword } from './auth'
import {
  createEntry,
  deleteEntry,
  getEntryByPath,
  listEntries,
  updateEntry,
} from './entries'
import { formatLocalDateTime } from './localDateTime'
import { getRecommendations } from './recommendations'
import type { AuthResponse, EntryRequest } from './types'

type ApiDebug = {
  listEntries: typeof listEntries
  getEntryByPath: typeof getEntryByPath
  createEntry: (payload: EntryRequest) => ReturnType<typeof createEntry>
  updateEntry: (payload: EntryRequest) => ReturnType<typeof updateEntry>
  deleteEntry: typeof deleteEntry
  analyzeText: typeof analyzeText
  getRecommendations: typeof getRecommendations
  verifyPassword: (password: string) => Promise<AuthResponse>
  formatLocalDateTime: typeof formatLocalDateTime
  smoke: () => Promise<void>
}

declare global {
  interface Window {
    moodverseApi?: ApiDebug
  }
}

export const registerApiDebug = (): void => {
  if (!import.meta.env.DEV) {
    return
  }

  const api: ApiDebug = {
    listEntries,
    getEntryByPath,
    createEntry,
    updateEntry,
    deleteEntry,
    analyzeText,
    getRecommendations,
    verifyPassword: (password: string) => verifyPassword({ password }),
    formatLocalDateTime,
    smoke: async () => {
      const entries = await listEntries()
      console.info('moodverseApi.smoke entries', entries)
    },
  }

  window.moodverseApi = api
  console.info('moodverseApi debug helpers ready', api)
}
