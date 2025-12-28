import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { MoviesTable } from '../components/MoviesTable'
import { SongsTable } from '../components/SongsTable'
import { useEntryDraft } from '../state/entryDraft'
import { useE2ee } from '../state/e2ee'
import { useRecommendations } from '../state/recommendations'
import { useUi } from '../state/ui'

type RecommendationLocationState = {
  entryTitle?: string
  hasUnsavedChanges?: boolean
  toast?: string
}

export const RecommendationPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { recommendations } = useRecommendations()
  const { clearDraft } = useEntryDraft()
  const { clear } = useE2ee()
  const { setPageError, clearPageError } = useUi()
  const locationState = location.state as RecommendationLocationState | null
  const [toastMessage, setToastMessage] = useState<string | null>(
    locationState?.toast ?? null,
  )


  useEffect(() => {
    if (!recommendations) {
      setPageError(
        'Recommendations not available. Return to the entry and click Get Media Recommendations again.',
      )
      return
    }

    clearPageError()
  }, [clearPageError, recommendations, setPageError])

  useEffect(() => {
    if (!toastMessage) {
      return
    }

    const timeout = window.setTimeout(() => {
      setToastMessage(null)
    }, 3000)

    return () => {
      window.clearTimeout(timeout)
    }
  }, [toastMessage])

  const handleLock = () => {
    clearDraft()
    clear()
    navigate('/unlock')
  }

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">SentimentScribe</p>
          <h1>Recommendations</h1>
        </div>
        <div className="page-header__actions">
          <div className="page-header__nav">
            <button
              type="button"
              className="primary-button"
              onClick={() => navigate('/entry')}
            >
              Back to Entry
            </button>
            <button
              type="button"
              className="ghost-button"
              onClick={() => navigate('/home')}
            >
              Home
            </button>
            <button
              type="button"
              className="ghost-button danger"
              onClick={handleLock}
            >
              Lock
            </button>
          </div>
        </div>
      </header>

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}

      <div className="recommendations-section">
        <div className="recommendations-header">
          <div>
            <h2>Songs</h2>
            <p className="subtle">A small soundtrack for this entry.</p>
          </div>
          <span className="pill">
            {recommendations ? recommendations.songs.length : 0}
          </span>
        </div>
        <SongsTable songs={recommendations?.songs ?? []} />
      </div>

      <div className="recommendations-section">
        <div className="recommendations-header">
          <div>
            <h2>Movies</h2>
            <p className="subtle">A few stories with a similar mood.</p>
          </div>
          <span className="pill">
            {recommendations ? recommendations.movies.length : 0}
          </span>
        </div>
        <MoviesTable movies={recommendations?.movies ?? []} />
      </div>
    </section>
  )
}
