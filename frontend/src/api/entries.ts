import { request } from './http'
import type {
  DeleteResponse,
  EntryRequest,
  EntryResponse,
  EntrySummaryResponse,
} from './types'

export const listEntries = (): Promise<EntrySummaryResponse[]> =>
  request<EntrySummaryResponse[]>('/api/entries')

export const getEntryByPath = (path: string): Promise<EntryResponse> => {
  const params = new URLSearchParams({ path })
  return request<EntryResponse>(`/api/entries/by-path?${params.toString()}`)
}

export const createEntry = (payload: EntryRequest): Promise<EntryResponse> =>
  request<EntryResponse>('/api/entries', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const updateEntry = (payload: EntryRequest): Promise<EntryResponse> =>
  request<EntryResponse>('/api/entries', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

export const deleteEntry = (path: string): Promise<DeleteResponse> => {
  const params = new URLSearchParams({ path })
  return request<DeleteResponse>(`/api/entries?${params.toString()}`, {
    method: 'DELETE',
  })
}
