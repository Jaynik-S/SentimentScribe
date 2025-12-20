import type { EntrySummaryResponse } from '../api/types'

type DeleteEntryModalProps = {
  open: boolean
  entry: EntrySummaryResponse | null
  error: string | null
  isDeleting: boolean
  onCancel: () => void
  onConfirm: () => void
}

export const DeleteEntryModal = ({
  open,
  entry,
  error,
  isDeleting,
  onCancel,
  onConfirm,
}: DeleteEntryModalProps) => {
  if (!open || !entry) {
    return null
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal">
        <header className="modal__header">
          <h2>Delete entry?</h2>
        </header>
        <div className="modal__body">
          <p>
            This will permanently delete <strong>{entry.title}</strong>.
          </p>
          <p className="entry-path">{entry.storagePath}</p>
          {error ? <p className="modal__error">{error}</p> : null}
        </div>
        <footer className="modal__footer">
          <button
            type="button"
            className="secondary-button"
            onClick={onCancel}
            disabled={isDeleting}
          >
            Cancel
          </button>
          <button
            type="button"
            className="primary-button danger"
            onClick={onConfirm}
            disabled={isDeleting}
          >
            {isDeleting ? (
              <span className="button-content">
                <span className="spinner" aria-hidden="true" />
                Deleting...
              </span>
            ) : (
              'Confirm Delete'
            )}
          </button>
        </footer>
      </div>
    </div>
  )
}
