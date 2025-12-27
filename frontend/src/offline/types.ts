import type { EntryRequest, LocalDateTime } from '../api/types'

export type IndexedDbEntryRecord = {
  userId: string
  storagePath: string
  createdAt: LocalDateTime | null
  updatedAt: LocalDateTime | null
  titleCiphertext: string
  titleIv: string
  bodyCiphertext: string
  bodyIv: string
  algo: string
  version: number
  dirty: boolean
  deletedAt: LocalDateTime | null
}

export type SyncQueueOperation = 'upsert' | 'delete'

export type IndexedDbSyncQueueItem = {
  id?: number
  userId: string
  op: SyncQueueOperation
  storagePath: string
  payload?: EntryRequest
  enqueuedAt: LocalDateTime
  retryCount: number
  lastAttemptAt: LocalDateTime | null
  lastError: string | null
}
