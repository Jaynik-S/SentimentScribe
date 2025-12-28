import type { SongRecommendationResponse } from '../api/types'

type SongsTableProps = {
  songs: SongRecommendationResponse[]
}

export const SongsTable = ({ songs }: SongsTableProps) => {
  if (songs.length === 0) {
    return <p className="empty-state">No song recommendations yet.</p>
  }

  return (
    <div className="recommendations-grid recommendations-grid--songs">
      {songs.map((song, index) => {
        const isTopPick = index === 0
        const key = song.songId || `${song.songName}-${song.artistName}`
        return (
          <article
            key={key}
            className={`rec-card${isTopPick ? ' rec-card--top' : ''}`}
          >
            {song.imageUrl ? (
              <img
                src={song.imageUrl}
                alt={`${song.songName} cover`}
                className="rec-card__image"
              />
            ) : (
              <div className="rec-card__placeholder" aria-hidden="true" />
            )}
            <div className="rec-card__body">
              <div className="rec-card__title">
                <h3>{song.songName}</h3>
                {isTopPick ? (
                  <span className="rec-card__badge">Top pick</span>
                ) : null}
              </div>
              <p className="rec-card__meta">{song.artistName}</p>
              <dl className="rec-card__details">
                <div>
                  <dt>Year</dt>
                  <dd>{song.releaseYear || '-'}</dd>
                </div>
                <div>
                  <dt>Popularity</dt>
                  <dd>{song.popularityScore || '-'}</dd>
                </div>
              </dl>
              {song.externalUrl ? (
                <a
                  className="secondary-button rec-card__button"
                  href={song.externalUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Open in Spotify
                </a>
              ) : null}
            </div>
          </article>
        )
      })}
    </div>
  )
}
