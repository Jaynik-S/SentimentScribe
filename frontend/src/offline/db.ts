export const OFFLINE_DB_NAME = 'sentimentscribe.offline'
export const OFFLINE_DB_VERSION = 1

export const ENTRIES_STORE = 'entries'
export const SYNC_QUEUE_STORE = 'syncQueue'

export type OfflineStoreName = typeof ENTRIES_STORE | typeof SYNC_QUEUE_STORE

const ensureEntriesStore = (db: IDBDatabase) => {
  if (db.objectStoreNames.contains(ENTRIES_STORE)) {
    return
  }

  const store = db.createObjectStore(ENTRIES_STORE, {
    keyPath: ['userId', 'storagePath'],
  })
  store.createIndex('byUserId', 'userId', { unique: false })
  store.createIndex('byUserUpdatedAt', ['userId', 'updatedAt'], { unique: false })
  store.createIndex('byUserStoragePath', ['userId', 'storagePath'], {
    unique: true,
  })
  store.createIndex('byUserDirty', ['userId', 'dirty'], { unique: false })
}

const ensureSyncQueueStore = (db: IDBDatabase) => {
  if (db.objectStoreNames.contains(SYNC_QUEUE_STORE)) {
    return
  }

  const store = db.createObjectStore(SYNC_QUEUE_STORE, {
    keyPath: 'id',
    autoIncrement: true,
  })
  store.createIndex('byUserId', 'userId', { unique: false })
}

let dbPromise: Promise<IDBDatabase> | null = null

export const openOfflineDb = (): Promise<IDBDatabase> => {
  if (dbPromise) {
    return dbPromise
  }

  if (typeof indexedDB === 'undefined') {
    return Promise.reject(new Error('IndexedDB is not available.'))
  }

  dbPromise = new Promise((resolve, reject) => {
    const request = indexedDB.open(OFFLINE_DB_NAME, OFFLINE_DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result
      ensureEntriesStore(db)
      ensureSyncQueueStore(db)
    }

    request.onblocked = () => {
      console.warn('Offline database upgrade blocked. Close other tabs and retry.')
    }

    request.onerror = () => {
      reject(request.error ?? new Error('Unable to open offline database.'))
    }

    request.onsuccess = () => {
      resolve(request.result)
    }
  })

  return dbPromise
}

export const requestToPromise = <T>(request: IDBRequest<T>): Promise<T> =>
  new Promise((resolve, reject) => {
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => {
      reject(request.error ?? new Error('IndexedDB request failed.'))
    }
  })

export const withTransaction = async <T>(
  storeNames: OfflineStoreName | OfflineStoreName[],
  mode: IDBTransactionMode,
  work: (
    stores: Record<OfflineStoreName, IDBObjectStore>,
    tx: IDBTransaction,
  ) => Promise<T> | T,
): Promise<T> => {
  const db = await openOfflineDb()
  const names = Array.isArray(storeNames) ? storeNames : [storeNames]
  const tx = db.transaction(names, mode)
  const stores = names.reduce(
    (acc, name) => {
      acc[name] = tx.objectStore(name)
      return acc
    },
    {} as Record<OfflineStoreName, IDBObjectStore>,
  )

  const result = await work(stores, tx)

  return new Promise<T>((resolve, reject) => {
    tx.oncomplete = () => resolve(result)
    tx.onerror = () => reject(tx.error ?? new Error('IndexedDB transaction failed.'))
    tx.onabort = () => reject(tx.error ?? new Error('IndexedDB transaction aborted.'))
  })
}
