import type { LocalDateTime } from '../api/types'
import { requestToPromise, SYNC_QUEUE_STORE, withTransaction } from './db'
import type { IndexedDbSyncQueueItem } from './types'

const sortById = (items: IndexedDbSyncQueueItem[]): IndexedDbSyncQueueItem[] =>
  [...items].sort((a, b) => (a.id ?? 0) - (b.id ?? 0))

export const enqueueSyncItem = async (
  item: Omit<IndexedDbSyncQueueItem, 'id'>,
): Promise<number> =>
  withTransaction(SYNC_QUEUE_STORE, 'readwrite', async ({ syncQueue }) => {
    const id = await requestToPromise(syncQueue.add(item))
    return id as number
  })

export const enqueueDelete = async (
  userId: string,
  storagePath: string,
  enqueuedAt: LocalDateTime,
): Promise<number> =>
  enqueueSyncItem({
    userId,
    op: 'delete',
    storagePath,
    enqueuedAt,
    retryCount: 0,
    lastAttemptAt: null,
    lastError: null,
  })

export const listSyncQueueByUser = async (
  userId: string,
): Promise<IndexedDbSyncQueueItem[]> =>
  withTransaction(SYNC_QUEUE_STORE, 'readonly', async ({ syncQueue }) => {
    const index = syncQueue.index('byUserId')
    const items = await requestToPromise(index.getAll(userId))
    return sortById(items)
  })

export const getNextSyncItem = async (
  userId: string,
): Promise<IndexedDbSyncQueueItem | null> => {
  const items = await listSyncQueueByUser(userId)
  return items[0] ?? null
}

export const updateSyncItem = async (
  item: IndexedDbSyncQueueItem,
): Promise<void> => {
  if (typeof item.id !== 'number') {
    throw new Error('Sync queue item id is required to update.')
  }

  await withTransaction(SYNC_QUEUE_STORE, 'readwrite', async ({ syncQueue }) => {
    await requestToPromise(syncQueue.put(item))
  })
}

export const removeSyncItem = async (id: number): Promise<void> => {
  await withTransaction(SYNC_QUEUE_STORE, 'readwrite', async ({ syncQueue }) => {
    await requestToPromise(syncQueue.delete(id))
  })
}

export const countSyncQueueByUser = async (userId: string): Promise<number> =>
  withTransaction(SYNC_QUEUE_STORE, 'readonly', async ({ syncQueue }) => {
    const index = syncQueue.index('byUserId')
    return requestToPromise(index.count(userId))
  })
