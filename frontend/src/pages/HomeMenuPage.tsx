import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { deleteEntry, listEntries } from '../api/entries'
import { isApiError } from '../api/http'
import type { EntrySummaryResponse } from '../api/types'
import { DeleteEntryModal } from '../components/DeleteEntryModal'
import { EntriesTable } from '../components/EntriesTable'
import { useEntryDraft } from '../state/entryDraft'
import { useUi } from '../state/ui'

const TOAST_DURATION_MS = 3000

export const HomeMenuPage = () => {
  const [entries, setEntries] = useState<EntrySummaryResponse[]>([])
  const [deleteTarget, setDeleteTarget] = useState<EntrySummaryResponse | null>(
    null,
  )
  const [deleteError, setDeleteError] = useState<string | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const navigate = useNavigate()
  const { setPageError, clearPageError, setPageLoading } = useUi()
  const { startNewEntry } = useEntryDraft()

  const loadEntries = useCallback(async () => {
    setPageLoading(true)
    clearPageError()

    try {
      const response = await listEntries()
      setEntries(response)
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to load entries.'
      setEntries([])
      setPageError(message, {
        label: 'Retry',
        onClick: () => {
          void loadEntries()
        },
      })
    } finally {
      setPageLoading(false)
    }
  }, [clearPageError, setPageError, setPageLoading])

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

  const handleRowClick = (entry: EntrySummaryResponse) => {
    const path = encodeURIComponent(entry.storagePath)
    navigate(`/entry?path=${path}`)
  }

  const handleDeleteClick = (entry: EntrySummaryResponse) => {
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

  const isDeleteOpen = Boolean(deleteTarget)

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">SentimentScribe</p>
          <h1>Home Menu</h1>
          <p className="subtle">Open an entry or start a new one.</p>
        </div>
        <button className="primary-button" type="button" onClick={handleNewEntry}>
          New Entry
        </button>
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
