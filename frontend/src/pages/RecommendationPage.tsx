import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { MoviesTable } from '../components/MoviesTable'
import { SongsTable } from '../components/SongsTable'
import { useRecommendations } from '../state/recommendations'
import { useUi } from '../state/ui'

export const RecommendationPage = () => {
  const navigate = useNavigate()
  const { recommendations } = useRecommendations()
  const { setPageError, clearPageError } = useUi()

  useEffect(() => {
    if (!recommendations) {
      setPageError(
        'Recommendations not available. Return to the entry and click Get Media Recommendations again.',
      )
      return
    }

    clearPageError()
  }, [clearPageError, recommendations, setPageError])

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">MoodVerse</p>
          <h1>Recommendations</h1>
          <p className="subtle">Based on the themes of your entry.</p>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={() => navigate('/entry')}
        >
          Back
        </button>
      </header>

      <div className="recommendations-section">
        <div className="recommendations-header">
          <h2>Songs</h2>
          <span className="pill">
            {recommendations ? recommendations.songs.length : 0}
          </span>
        </div>
        <SongsTable songs={recommendations?.songs ?? []} />
      </div>

      <div className="recommendations-section">
        <div className="recommendations-header">
          <h2>Movies</h2>
          <span className="pill">
            {recommendations ? recommendations.movies.length : 0}
          </span>
        </div>
        <MoviesTable movies={recommendations?.movies ?? []} />
      </div>
    </section>
  )
}
