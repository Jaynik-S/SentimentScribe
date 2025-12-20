import { describe, expect, it } from 'vitest'
import { verifyPassword } from '../auth'
import { jsonResponse, mockFetch } from '../../test/mockFetch'

describe('api/auth', () => {
  it('verifies password', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ status: 'Correct Password', entries: [] }),
    )

    await verifyPassword({ password: 'secret' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/auth/verify')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ password: 'secret' }))
  })
})
