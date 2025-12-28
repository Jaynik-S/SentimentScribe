import type { MovieRecommendationResponse } from '../api/types'

type MoviesTableProps = {
  movies: MovieRecommendationResponse[]
}

export const MoviesTable = ({ movies }: MoviesTableProps) => {
  if (movies.length === 0) {
    return <p className="empty-state">No movie recommendations yet.</p>
  }

  return (
    <div className="recommendations-grid">
      {movies.map((movie, index) => {
        const isTopPick = index === 0
        return (
          <article
            key={`${movie.movieTitle}-${movie.releaseYear}`}
            className={`rec-card${isTopPick ? ' rec-card--top' : ''}`}
          >
            {movie.imageUrl ? (
              <img
                src={movie.imageUrl}
                alt={`${movie.movieTitle} poster`}
                className="rec-card__image"
              />
            ) : (
              <div className="rec-card__placeholder" aria-hidden="true" />
            )}
            <div className="rec-card__body">
              <div className="rec-card__title">
                <h3>{movie.movieTitle}</h3>
                {isTopPick ? (
                  <span className="rec-card__badge">Top pick</span>
                ) : null}
              </div>
              <p className="rec-card__meta">Rating: {movie.movieRating || '-'}</p>
              <dl className="rec-card__details">
                <div>
                  <dt>Year</dt>
                  <dd>{movie.releaseYear || '-'}</dd>
                </div>
              </dl>
              <p className="rec-card__overview">
                {movie.overview || 'No overview available.'}
              </p>
            </div>
          </article>
        )
      })}
    </div>
  )
}
