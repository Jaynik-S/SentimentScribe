import { useEffect } from 'react'
import { describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { RecommendationPage } from '../RecommendationPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { useRecommendations } from '../../state/recommendations'
import type { RecommendationResponse } from '../../api/types'

const RecommendationsSeed = ({
  data,
}: {
  data: RecommendationResponse | null
}) => {
  const { setRecommendations } = useRecommendations()

  useEffect(() => {
    setRecommendations(data)
  }, [data, setRecommendations])

  return <RecommendationPage />
}

describe('RecommendationPage', () => {
  it('shows a banner when recommendations are missing', async () => {
    renderWithRouter({
      initialEntries: ['/recommendations'],
      routes: [{ path: '/recommendations', element: <RecommendationPage /> }],
    })

    expect(
      await screen.findByText(
        /Recommendations not available\. Return to the entry/i,
      ),
    ).toBeInTheDocument()
  })

  it('renders song and movie lists', async () => {
    const data: RecommendationResponse = {
      keywords: [],
      songs: [
        {
          releaseYear: '2024',
          imageUrl: 'https://example.com/song.jpg',
          songName: 'Quiet Storm',
          artistName: 'Dawn',
          popularityScore: '88',
          externalUrl: 'https://spotify.com',
        },
      ],
      movies: [
        {
          releaseYear: '2022',
          imageUrl: 'https://example.com/movie.jpg',
          movieTitle: 'Night Walk',
          movieRating: 'PG-13',
          overview: 'A reflective journey.',
        },
      ],
    }

    renderWithRouter({
      initialEntries: ['/recommendations'],
      routes: [
        {
          path: '/recommendations',
          element: <RecommendationsSeed data={data} />,
        },
      ],
    })

    expect(await screen.findByText('Quiet Storm')).toBeInTheDocument()
    expect(screen.getByText('Night Walk')).toBeInTheDocument()
  })
})
