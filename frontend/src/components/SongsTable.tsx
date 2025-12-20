import type { SongRecommendationResponse } from '../api/types'

type SongsTableProps = {
  songs: SongRecommendationResponse[]
}

export const SongsTable = ({ songs }: SongsTableProps) => {
  if (songs.length === 0) {
    return <p className="empty-state">No song recommendations yet.</p>
  }

  return (
    <div className="recommendations-grid">
      {songs.map((song) => (
        <article key={`${song.songName}-${song.artistName}`} className="rec-card">
          {song.imageUrl ? (
            <img
              src={song.imageUrl}
              alt={`${song.songName} cover`}
              className="rec-card__image"
            />
          ) : null}
          <div className="rec-card__body">
            <h3>{song.songName}</h3>
            <p className="rec-card__meta">{song.artistName}</p>
            <dl className="rec-card__details">
              <div>
                <dt>Year</dt>
                <dd>{song.releaseYear || '—'}</dd>
              </div>
              <div>
                <dt>Popularity</dt>
                <dd>{song.popularityScore || '—'}</dd>
              </div>
            </dl>
            {song.externalUrl ? (
              <a
                className="rec-card__link"
                href={song.externalUrl}
                target="_blank"
                rel="noreferrer"
              >
                Open in Spotify
              </a>
            ) : null}
          </div>
        </article>
      ))}
    </div>
  )
}
