import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteEntry, listEntries } from '../api/entries'
import { isApiError } from '../api/http'
import { DeleteEntryModal } from '../components/DeleteEntryModal'
import { EntriesTable } from '../components/EntriesTable'
import { decrypt } from '../crypto/diaryCrypto'
import { listEntriesByUser, upsertEntry } from '../offline/entriesRepo'
import type { IndexedDbEntryRecord } from '../offline/types'
import { useAuth } from '../state/auth'
import { useEntryDraft } from '../state/entryDraft'
import { useE2ee } from '../state/e2ee'
import { useOffline } from '../state/offline'
import { useUi } from '../state/ui'
import type { EntrySummaryView } from '../types/entries'

const TOAST_DURATION_MS = 3000

export const HomeMenuPage = () => {
  const [entries, setEntries] = useState<EntrySummaryView[]>([])
  const [deleteTarget, setDeleteTarget] = useState<EntrySummaryView | null>(
    null,
  )
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const navigate = useNavigate()
  const { auth } = useAuth()
  const { setPageError, clearPageError, setPageLoading } = useUi()
  const { startNewEntry, clearDraft } = useEntryDraft()
  const { key, clear } = useE2ee()
  const { isOffline } = useOffline()

  const userId = auth?.user.id ?? null

  const mapRecordToSummary = useCallback(
    (record: IndexedDbEntryRecord): EntrySummaryView => ({
      storagePath: record.storagePath,
      createdAt: record.createdAt,
      updatedAt: record.updatedAt,
      titleCiphertext: record.titleCiphertext,
      titleIv: record.titleIv,
      algo: record.algo,
      version: record.version,
    }),
    [],
  )

  const hydrateEntries = useCallback(
    async (records: IndexedDbEntryRecord[]) => {
      const summaries = records.map(mapRecordToSummary)
      if (!summaries.length) {
        setEntries([])
        return
      }

      if (!key) {
        setEntries(
          summaries.map((entry) => ({
            ...entry,
            titlePlaintext: 'Encrypted entry',
          })),
        )
        setPageError('Unlock your diary to decrypt entries.')
        return
      }

      let hadFailure = false
      const decrypted = await Promise.all(
        summaries.map(async (entry) => {
          try {
            const titlePlaintext = await decrypt(
              {
                ciphertext: entry.titleCiphertext,
                iv: entry.titleIv,
                algo: entry.algo,
                version: entry.version,
              },
              key,
            )
            return { ...entry, titlePlaintext }
          } catch {
            hadFailure = true
            return { ...entry, titlePlaintext: 'Encrypted entry' }
          }
        }),
      )
      if (hadFailure) {
        setPageError('Unable to decrypt some entries.')
      }
      setEntries(decrypted)
    },
    [key, mapRecordToSummary, setPageError],
  )

  const loadLocalEntries = useCallback(async () => {
    if (!userId) {
      setEntries([])
      return []
    }

    const records = await listEntriesByUser(userId)
    await hydrateEntries(records)
    return records
  }, [hydrateEntries, userId])

  const refreshFromApi = useCallback(async (): Promise<boolean> => {
    if (!userId || isOffline) {
      return false
    }

    try {
      clearPageError()
      const response = await listEntries()
      const localRecords = await listEntriesByUser(userId, { includeDeleted: true })
      const localByPath = new Map(
        localRecords.map((record) => [record.storagePath, record]),
      )

      await Promise.all(
        response.map(async (entry) => {
          const local = localByPath.get(entry.storagePath)
          if (local?.dirty || local?.deletedAt) {
            return
          }

          await upsertEntry({
            userId,
            storagePath: entry.storagePath,
            createdAt: entry.createdAt ?? local?.createdAt ?? null,
            updatedAt: entry.updatedAt ?? local?.updatedAt ?? null,
            titleCiphertext: entry.titleCiphertext,
            titleIv: entry.titleIv,
            bodyCiphertext: local?.bodyCiphertext ?? null,
            bodyIv: local?.bodyIv ?? null,
            algo: entry.algo,
            version: entry.version,
            dirty: false,
            deletedAt: null,
          })
        }),
      )

      return true
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to refresh entries.'
      setPageError(message, {
        label: 'Retry',
        onClick: () => {
          void refreshFromApi()
        },
      })
      return false
    }
  }, [clearPageError, isOffline, setPageError, userId])

  const loadEntries = useCallback(async () => {
    setPageLoading(true)
    clearPageError()

    try {
      await loadLocalEntries()
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Unable to load entries.'
      setPageError(message)
    } finally {
      setPageLoading(false)
    }

    const refreshed = await refreshFromApi()
    if (refreshed) {
      await loadLocalEntries()
    }
  }, [
    clearPageError,
    loadLocalEntries,
    refreshFromApi,
    setPageError,
    setPageLoading,
  ])

  useEffect(() => {
    void loadEntries()
  }, [loadEntries])

  useEffect(() => {
    if (!toastMessage) {
      return
    }

    const timeout = window.setTimeout(() => {
      setToastMessage(null)
    }, TOAST_DURATION_MS)

    return () => {
      window.clearTimeout(timeout)
    }
  }, [toastMessage])

  const handleRowClick = (entry: EntrySummaryView) => {
    const path = encodeURIComponent(entry.storagePath)
    navigate(`/entry?path=${path}`)
  }

  const handleDeleteClick = (entry: EntrySummaryView) => {
    setDeleteTarget(entry)
    setDeleteError(null)
  }

  const handleDeleteCancel = () => {
    setDeleteTarget(null)
    setDeleteError(null)
    setIsDeleting(false)
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget || isDeleting) {
      return
    }

    setIsDeleting(true)
    setDeleteError(null)

    try {
      const response = await deleteEntry(deleteTarget.storagePath)
      if (response.deleted) {
        setDeleteTarget(null)
        setToastMessage('Deleted entry')
        await loadEntries()
      } else {
        setDeleteError('Delete failed. Please retry.')
      }
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to delete entry.'
      setDeleteError(message)
    } finally {
      setIsDeleting(false)
    }
  }

  const handleNewEntry = () => {
    startNewEntry()
    navigate('/entry')
  }

  const handleLock = () => {
    clearDraft()
    clear()
    navigate('/unlock')
  }

  const isDeleteOpen = Boolean(deleteTarget)

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">SentimentScribe</p>
          <h1>Home Menu</h1>
          <p className="subtle">Open an entry or start a new one.</p>
        </div>
        <div className="page-header__actions">
          <button
            className="secondary-button"
            type="button"
            onClick={handleLock}
          >
            Lock
          </button>
          <button
            className="primary-button"
            type="button"
            onClick={handleNewEntry}
          >
            New Entry
          </button>
        </div>
      </header>

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}

      <EntriesTable
        entries={entries}
        onRowClick={handleRowClick}
        onDeleteClick={handleDeleteClick}
      />

      <DeleteEntryModal
        open={isDeleteOpen}
        entry={deleteTarget}
        error={deleteError}
        isDeleting={isDeleting}
        onCancel={handleDeleteCancel}
        onConfirm={handleDeleteConfirm}
      />
    </section>
  )
}
