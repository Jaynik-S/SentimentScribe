import type { EntryDraft } from '../state/entryDraft'

type KeywordsDropdownProps = {
  keywords: EntryDraft['keywords']
}

export const KeywordsDropdown = ({ keywords }: KeywordsDropdownProps) => {
  if (keywords.length === 0) {
    return <p className="keywords-empty">No keywords yet.</p>
  }

  return (
    <div className="keywords-panel">
      {keywords.map((keyword) => (
        <span key={keyword} className="keyword-chip">
          {keyword}
        </span>
      ))}
    </div>
  )
}
