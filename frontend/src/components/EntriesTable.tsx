import type { EntrySummaryResponse } from '../api/types'

type EntriesTableProps = {
  entries: EntrySummaryResponse[]
  onRowClick: (entry: EntrySummaryResponse) => void
  onDeleteClick: (entry: EntrySummaryResponse) => void
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
            <th>Keywords</th>
            <th aria-label="Delete" />
          </tr>
        </thead>
        <tbody>
          {entries.length === 0 ? (
            <tr>
              <td colSpan={5} className="entries-table__empty">
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
                    <span>{entry.title}</span>
                    <span className="entry-path">{entry.storagePath}</span>
                  </div>
                </td>
                <td>{formatDate(entry.createdAt)}</td>
                <td>{formatDate(entry.updatedAt)}</td>
                <td className="entries-table__keywords">
                  {entry.keywords.length > 0 ? entry.keywords.join(', ') : '—'}
                </td>
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
