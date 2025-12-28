import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { getRecommendations } from '../api/recommendations'
import type {
  MovieRecommendationResponse,
  SongRecommendationResponse,
} from '../api/types'
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

const MAX_PAGES = 5

const getSongKey = (song: SongRecommendationResponse): string =>
  song.songId?.trim() || `${song.songName}-${song.artistName}`.trim()

const getMovieKey = (movie: MovieRecommendationResponse): string =>
  movie.movieId?.trim() || `${movie.movieTitle}-${movie.releaseYear}`.trim()

export const RecommendationPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { recommendations, sourceText } = useRecommendations()
  const { clearDraft } = useEntryDraft()
  const { clear } = useE2ee()
  const { setPageError, clearPageError } = useUi()
  const locationState = location.state as RecommendationLocationState | null
  const [toastMessage, setToastMessage] = useState<string | null>(
    locationState?.toast ?? null,
  )
  const [songPages, setSongPages] = useState<SongRecommendationResponse[][]>([])
  const [moviePages, setMoviePages] = useState<MovieRecommendationResponse[][]>(
    [],
  )
  const [songPageIndex, setSongPageIndex] = useState(0)
  const [moviePageIndex, setMoviePageIndex] = useState(0)
  const [songError, setSongError] = useState<string | null>(null)
  const [movieError, setMovieError] = useState<string | null>(null)
  const [isSongsLoading, setIsSongsLoading] = useState(false)
  const [isMoviesLoading, setIsMoviesLoading] = useState(false)


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
    if (!recommendations) {
      return
    }

    setSongPages([recommendations.songs])
    setMoviePages([recommendations.movies])
    setSongPageIndex(0)
    setMoviePageIndex(0)
    setSongError(null)
    setMovieError(null)
  }, [recommendations])

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

  const songSeenIds = useMemo(() => {
    const ids = new Set<string>()
    songPages.flat().forEach((song) => {
      if (song.songId?.trim()) {
        ids.add(song.songId.trim())
      }
    })
    return ids
  }, [songPages])

  const movieSeenIds = useMemo(() => {
    const ids = new Set<string>()
    moviePages.flat().forEach((movie) => {
      if (movie.movieId?.trim()) {
        ids.add(movie.movieId.trim())
      }
    })
    return ids
  }, [moviePages])

  const songSeenKeys = useMemo(() => {
    const keys = new Set<string>()
    songPages.flat().forEach((song) => {
      const key = getSongKey(song)
      if (key) {
        keys.add(key)
      }
    })
    return keys
  }, [songPages])

  const movieSeenKeys = useMemo(() => {
    const keys = new Set<string>()
    moviePages.flat().forEach((movie) => {
      const key = getMovieKey(movie)
      if (key) {
        keys.add(key)
      }
    })
    return keys
  }, [moviePages])

  const visibleSongs = songPages[songPageIndex] ?? []
  const visibleMovies = moviePages[moviePageIndex] ?? []

  const handleLoadMoreSongs = async () => {
    if (isSongsLoading || songPages.length >= MAX_PAGES) {
      return
    }
    if (!sourceText) {
      setSongError('Unable to load more songs.')
      return
    }

    setIsSongsLoading(true)
    setSongError(null)

    try {
      const response = await getRecommendations({
        text: sourceText,
        excludeSongIds: Array.from(songSeenIds),
        excludeMovieIds: Array.from(movieSeenIds),
      })
      const nextSeen = new Set(songSeenKeys)
      const nextSongs = response.songs.filter((song) => {
        const key = getSongKey(song)
        if (!key || nextSeen.has(key)) {
          return false
        }
        nextSeen.add(key)
        return true
      })
      if (nextSongs.length === 0) {
        setSongError('Unable to load more songs.')
        return
      }
      const nextIndex = songPages.length
      setSongPages((prev) => [...prev, nextSongs])
      setSongPageIndex(nextIndex)
    } catch (error) {
      setSongError('Unable to load more songs.')
    } finally {
      setIsSongsLoading(false)
    }
  }

  const handleLoadMoreMovies = async () => {
    if (isMoviesLoading || moviePages.length >= MAX_PAGES) {
      return
    }
    if (!sourceText) {
      setMovieError('Unable to load more movies.')
      return
    }

    setIsMoviesLoading(true)
    setMovieError(null)

    try {
      const response = await getRecommendations({
        text: sourceText,
        excludeSongIds: Array.from(songSeenIds),
        excludeMovieIds: Array.from(movieSeenIds),
      })
      const nextSeen = new Set(movieSeenKeys)
      const nextMovies = response.movies.filter((movie) => {
        const key = getMovieKey(movie)
        if (!key || nextSeen.has(key)) {
          return false
        }
        nextSeen.add(key)
        return true
      })
      if (nextMovies.length === 0) {
        setMovieError('Unable to load more movies.')
        return
      }
      const nextIndex = moviePages.length
      setMoviePages((prev) => [...prev, nextMovies])
      setMoviePageIndex(nextIndex)
    } catch (error) {
      setMovieError('Unable to load more movies.')
    } finally {
      setIsMoviesLoading(false)
    }
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
            {visibleSongs.length}
          </span>
        </div>
        <SongsTable songs={visibleSongs} />
        {recommendations ? (
          <div className="recommendations-footer">
            {songError ? <p className="section-error">{songError}</p> : null}
            <div className="recommendations-footer__actions">
              {songPages.length > 1 ? (
                <div
                  className="pagination"
                  role="tablist"
                  aria-label="Song pages"
                >
                  {songPages.map((_, index) => (
                    <button
                      key={`song-page-${index + 1}`}
                      type="button"
                      className={`pagination__button${
                        songPageIndex === index
                          ? ' pagination__button--active'
                          : ''
                      }`}
                      onClick={() => setSongPageIndex(index)}
                    >
                      {index + 1}
                    </button>
                  ))}
                </div>
              ) : null}
              {songPages.length < MAX_PAGES ? (
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleLoadMoreSongs}
                  disabled={isSongsLoading}
                >
                  {isSongsLoading ? (
                    <span className="button-content">
                      <span className="spinner" aria-hidden="true" />
                      Loading...
                    </span>
                  ) : (
                    'Load more'
                  )}
                </button>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>

      <div className="recommendations-section">
        <div className="recommendations-header">
          <div>
            <h2>Movies</h2>
            <p className="subtle">A few stories with a similar mood.</p>
          </div>
          <span className="pill">
            {visibleMovies.length}
          </span>
        </div>
        <MoviesTable movies={visibleMovies} />
        {recommendations ? (
          <div className="recommendations-footer">
            {movieError ? <p className="section-error">{movieError}</p> : null}
            <div className="recommendations-footer__actions">
              {moviePages.length > 1 ? (
                <div
                  className="pagination"
                  role="tablist"
                  aria-label="Movie pages"
                >
                  {moviePages.map((_, index) => (
                    <button
                      key={`movie-page-${index + 1}`}
                      type="button"
                      className={`pagination__button${
                        moviePageIndex === index
                          ? ' pagination__button--active'
                          : ''
                      }`}
                      onClick={() => setMoviePageIndex(index)}
                    >
                      {index + 1}
                    </button>
                  ))}
                </div>
              ) : null}
              {moviePages.length < MAX_PAGES ? (
                <button
                  type="button"
                  className="secondary-button"
                  onClick={handleLoadMoreMovies}
                  disabled={isMoviesLoading}
                >
                  {isMoviesLoading ? (
                    <span className="button-content">
                      <span className="spinner" aria-hidden="true" />
                      Loading...
                    </span>
                  ) : (
                    'Load more'
                  )}
                </button>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>
    </section>
  )
}
