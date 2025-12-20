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
      {movies.map((movie) => (
        <article key={`${movie.movieTitle}-${movie.releaseYear}`} className="rec-card">
          {movie.imageUrl ? (
            <img
              src={movie.imageUrl}
              alt={`${movie.movieTitle} poster`}
              className="rec-card__image"
            />
          ) : null}
          <div className="rec-card__body">
            <h3>{movie.movieTitle}</h3>
            <p className="rec-card__meta">Rating: {movie.movieRating || '—'}</p>
            <dl className="rec-card__details">
              <div>
                <dt>Year</dt>
                <dd>{movie.releaseYear || '—'}</dd>
              </div>
            </dl>
            <p className="rec-card__overview">
              {movie.overview || 'No overview available.'}
            </p>
          </div>
        </article>
      ))}
    </div>
  )
}
