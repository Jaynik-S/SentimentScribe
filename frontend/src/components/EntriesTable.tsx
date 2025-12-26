import type { EntrySummaryView } from '../types/entries'

type EntriesTableProps = {
  entries: EntrySummaryView[]
  onRowClick: (entry: EntrySummaryView) => void
  onDeleteClick: (entry: EntrySummaryView) => void
}

const formatDate = (value: string | null): string => {
  if (!value) {
    return '—'
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
}: EntriesTableProps) => {
  return (
    <div className="entries-table">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Date Created</th>
            <th>Date Edited</th>
            <th aria-label="Delete" />
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 ? (
            <tr>
              <td colSpan={4} className="entries-table__empty">
                No entries yet.
              </td>
            </tr>
          ) : (
            entries.map((entry) => (
              <tr
                key={entry.storagePath}
                className="entries-table__row"
                role="button"
                tabIndex={0}
                onClick={() => onRowClick(entry)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') {
                    onRowClick(entry)
                  }
                }}
              >
                <td>
                  <div className="entry-title">
                    <span>
                      {entry.titlePlaintext ||
                        entry.titleCiphertext ||
                        'Encrypted entry'}
                    </span>
                    <span className="entry-path">{entry.storagePath}</span>
                  </div>
                </td>
                <td>{formatDate(entry.createdAt)}</td>
                <td>{formatDate(entry.updatedAt)}</td>
                <td>
                  <button
                    type="button"
                    className="link-button danger"
                    onClick={(event) => {
                      event.stopPropagation()
                      onDeleteClick(entry)
                    }}
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))
          )}
        </tbody>
      </table>
    </div>
  )
}
