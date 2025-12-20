import { describe, expect, it } from 'vitest'
import { analyzeText } from '../analysis'
import { jsonResponse, mockFetch } from '../../test/mockFetch'

describe('api/analysis', () => {
  it('requests keyword analysis', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(jsonResponse({ keywords: ['hope'] }))

    await analyzeText({ text: 'Hello' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/analysis')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ text: 'Hello' }))
  })
})
