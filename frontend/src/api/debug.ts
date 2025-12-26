import { analyzeText } from './analysis'
import { login, register } from './auth'
import {
  createEntry,
  deleteEntry,
  getEntryByPath,
  listEntries,
  updateEntry,
} from './entries'
import { formatLocalDateTime } from './localDateTime'
import { getRecommendations } from './recommendations'
import type { AuthTokenResponse, EntryRequest } from './types'

type ApiDebug = {
  listEntries: typeof listEntries
  getEntryByPath: typeof getEntryByPath
  createEntry: (payload: EntryRequest) => ReturnType<typeof createEntry>
  updateEntry: (payload: EntryRequest) => ReturnType<typeof updateEntry>
  deleteEntry: typeof deleteEntry
  analyzeText: typeof analyzeText
  getRecommendations: typeof getRecommendations
  login: (username: string, password: string) => Promise<AuthTokenResponse>
  register: (username: string, password: string) => Promise<AuthTokenResponse>
  formatLocalDateTime: typeof formatLocalDateTime
  smoke: () => Promise<void>
}

declare global {
  interface Window {
    sentimentScribeApi?: ApiDebug
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
    login: (username: string, password: string) => login({ username, password }),
    register: (username: string, password: string) => register({ username, password }),
    formatLocalDateTime,
    smoke: async () => {
      const entries = await listEntries()
      console.info('sentimentScribeApi.smoke entries', entries)
    },
  }

  window.sentimentScribeApi = api
  console.info('sentimentScribeApi debug helpers ready', api)
}
