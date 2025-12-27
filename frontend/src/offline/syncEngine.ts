import { updateEntry, createEntry, deleteEntry } from '../api/entries'
import { isApiError } from '../api/http'
import { formatLocalDateTime } from '../api/localDateTime'
import { getEntry, removeEntry, upsertEntry } from './entriesRepo'
import {
  getNextSyncItem,
  removeSyncItem,
  updateSyncItem,
} from './syncQueueRepo'
import type { IndexedDbEntryRecord, IndexedDbSyncQueueItem } from './types'

const nowTimestamp = (): string => formatLocalDateTime(new Date())

const getErrorMessage = (error: unknown): string => {
  if (isApiError(error)) {
    return error.data.error
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Sync failed.'
}

const upsertEntryToApi = async (
  item: IndexedDbSyncQueueItem,
): Promise<IndexedDbEntryRecord> => {
  if (!item.payload) {
    throw new Error('Sync queue upsert item is missing payload.')
  }

  try {
    const response = await updateEntry(item.payload)
    return {
      userId: item.userId,
      storagePath: response.storagePath,
      createdAt: response.createdAt ?? item.payload.createdAt ?? null,
      updatedAt: response.updatedAt ?? null,
      titleCiphertext: response.titleCiphertext,
      titleIv: response.titleIv,
      bodyCiphertext: response.bodyCiphertext,
      bodyIv: response.bodyIv,
      algo: response.algo,
      version: response.version,
      dirty: false,
      deletedAt: null,
    }
  } catch (error) {
    if (isApiError(error) && error.status === 404) {
      const response = await createEntry(item.payload)
      return {
        userId: item.userId,
        storagePath: response.storagePath,
        createdAt: response.createdAt ?? item.payload.createdAt ?? null,
        updatedAt: response.updatedAt ?? null,
        titleCiphertext: response.titleCiphertext,
        titleIv: response.titleIv,
        bodyCiphertext: response.bodyCiphertext,
        bodyIv: response.bodyIv,
        algo: response.algo,
        version: response.version,
        dirty: false,
        deletedAt: null,
      }
    }
    throw error
  }
}

const updateEntryFromQueueItem = async (
  item: IndexedDbSyncQueueItem,
  record: IndexedDbEntryRecord,
): Promise<void> => {
  const existing = await getEntry(item.userId, record.storagePath)
  const createdAt =
    record.createdAt ?? existing?.createdAt ?? item.payload?.createdAt ?? null

  await upsertEntry({
    ...record,
    createdAt,
    dirty: false,
    deletedAt: null,
  })
}

const deleteEntryFromQueueItem = async (
  item: IndexedDbSyncQueueItem,
): Promise<void> => {
  await deleteEntry(item.storagePath)
  await removeEntry(item.userId, item.storagePath)
}

const markQueueFailure = async (
  item: IndexedDbSyncQueueItem,
  error: unknown,
): Promise<void> => {
  const timestamp = nowTimestamp()
  await updateSyncItem({
    ...item,
    retryCount: item.retryCount + 1,
    lastAttemptAt: timestamp,
    lastError: getErrorMessage(error),
  })
}

export const flushSyncQueue = async (userId: string): Promise<void> => {
  // Process items in order; stop at first failure to avoid churn.
  while (true) {
    const item = await getNextSyncItem(userId)
    if (!item) {
      return
    }

    try {
      if (item.op === 'upsert') {
        const record = await upsertEntryToApi(item)
        await updateEntryFromQueueItem(item, record)
      } else if (item.op === 'delete') {
        await deleteEntryFromQueueItem(item)
      } else {
        throw new Error(`Unknown sync operation: ${item.op}`)
      }

      await removeSyncItem(item.id!)
    } catch (error) {
      await markQueueFailure(item, error)
      return
    }
  }
}
