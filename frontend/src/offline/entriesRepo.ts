import { ENTRIES_STORE, requestToPromise, withTransaction } from './db'
import type { IndexedDbEntryRecord } from './types'

type ListOptions = {
  includeDeleted?: boolean
}

const getRecordTimestamp = (record: IndexedDbEntryRecord): string =>
  record.updatedAt ?? record.createdAt ?? ''

export const upsertEntry = async (
  record: IndexedDbEntryRecord,
): Promise<void> => {
  await withTransaction(ENTRIES_STORE, 'readwrite', async ({ entries }) => {
    await requestToPromise(entries.put(record))
  })
}

export const getEntry = async (
  userId: string,
  storagePath: string,
): Promise<IndexedDbEntryRecord | null> =>
  withTransaction(ENTRIES_STORE, 'readonly', async ({ entries }) => {
    const record = await requestToPromise(
      entries.get([userId, storagePath]),
    )
    return record ?? null
  })

export const listEntriesByUser = async (
  userId: string,
  { includeDeleted = false }: ListOptions = {},
): Promise<IndexedDbEntryRecord[]> =>
  withTransaction(ENTRIES_STORE, 'readonly', async ({ entries }) => {
    const index = entries.index('byUserId')
    const records = await requestToPromise(index.getAll(userId))
    const visible = includeDeleted
      ? records
      : records.filter((record) => !record.deletedAt)

    return [...visible].sort((a, b) =>
      getRecordTimestamp(b).localeCompare(getRecordTimestamp(a)),
    )
  })

export const removeEntry = async (
  userId: string,
  storagePath: string,
): Promise<void> => {
  await withTransaction(ENTRIES_STORE, 'readwrite', async ({ entries }) => {
    await requestToPromise(entries.delete([userId, storagePath]))
  })
}
