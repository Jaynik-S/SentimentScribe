import type { EntrySummaryView } from '../types/entries'

type EntriesTableProps = {
  entries: EntrySummaryView[]
  onRowClick: (entry: EntrySummaryView) => void
  onDeleteClick: (entry: EntrySummaryView) => void
  activePath?: string | null
}

const formatDate = (value: string | null): string => {
  if (!value) {
    return '-'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString()
}

export const EntriesTable = ({
  entries,
  onRowClick,
  onDeleteClick,
  activePath,
}: EntriesTableProps) => {
  if (entries.length === 0) {
    return (
      <div className="entries-empty">
        <h2>Your journal is ready.</h2>
        <p className="subtle">
          When you save entries, they will appear here for quick return visits.
        </p>
      </div>
    )
  }

  const createSnippet = (value?: string): string => {
    if (!value) {
      return ''
    }
    return value.replace(/\s+/g, ' ').trim()
  }

  return (
    <div className="entries-list" role="list">
      {entries.map((entry) => {
        const title =
          entry.titlePlaintext || entry.titleCiphertext || 'Encrypted entry'
        const isActive = Boolean(activePath && activePath === entry.storagePath)
        const snippet = createSnippet(entry.bodyPlaintext)

        return (
          <article
            key={entry.storagePath}
            className={`entry-card${isActive ? ' entry-card--active' : ''}`}
            role="button"
            tabIndex={0}
            onClick={() => onRowClick(entry)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                onRowClick(entry)
              }
            }}
            aria-label={`Open ${title}`}
          >
            <div className="entry-card__content">
              <p className="entry-card__title">{title}</p>
              <p className="entry-card__snippet">{snippet}</p>
            </div>

            <div className="entry-card__footer">
              <div className="entry-card__meta">
                <div className="entry-card__meta-item">
                  <svg viewBox="0 0 20 20" className="icon" aria-hidden="true">
                    <path
                      d="M6 3.5v3M14 3.5v3M4 8.5h12M5.5 5h9a1 1 0 0 1 1 1v9a1.5 1.5 0 0 1-1.5 1.5h-8A1.5 1.5 0 0 1 4 15V6a1 1 0 0 1 1-1Z"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.4"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                  <span>Created {formatDate(entry.createdAt)}</span>
                </div>
                <div className="entry-card__meta-item">
                  <svg viewBox="0 0 20 20" className="icon" aria-hidden="true">
                    <path
                      d="M10 6.2v4l2.6 1.6M10 3.5a6.5 6.5 0 1 0 0 13 6.5 6.5 0 0 0 0-13Z"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.4"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                  <span>Edited {formatDate(entry.updatedAt)}</span>
                </div>
              </div>

              <button
                type="button"
                className="icon-button danger entry-card__delete"
                onClick={(event) => {
                  event.stopPropagation()
                  onDeleteClick(entry)
                }}
              >
                <svg viewBox="0 0 20 20" className="icon" aria-hidden="true">
                  <path
                    d="M4.5 6.5h11m-9.5 0 .5 9a1 1 0 0 0 1 .9h4a1 1 0 0 0 1-.9l.5-9m-6-2h4m-6 2v-1a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v1"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                Delete
              </button>
            </div>
          </article>
        )
      })}
    </div>
  )
}
