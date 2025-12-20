import { describe, expect, it } from 'vitest'
import { getRecommendations } from '../recommendations'
import { jsonResponse, mockFetch } from '../../test/mockFetch'

describe('api/recommendations', () => {
  it('requests recommendations', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ keywords: [], songs: [], movies: [] }),
    )

    await getRecommendations({ text: 'Hello' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/recommendations')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ text: 'Hello' }))
  })
})
