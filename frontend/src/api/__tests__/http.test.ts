import { afterEach, describe, expect, it } from 'vitest'
import { ApiError, request } from '../http'
import { jsonResponse, mockFetch, textResponse } from '../../test/mockFetch'

describe('api/http', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('attaches bearer token when available', async () => {
    sessionStorage.setItem('sentimentscribe.auth', JSON.stringify({
      accessToken: 'token',
      user: { id: 'user-id', username: 'demo' },
      e2eeParams: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
    }))
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(jsonResponse({ ok: true }))

    await request('/api/entries')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/entries')
    expect(new Headers(options?.headers).get('Authorization')).toBe('Bearer token')
  })

  it('throws ApiError with parsed error response', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ error: 'Incorrect Password' }, { status: 400 }),
    )

    const error = await request('/api/auth/login').catch((err) => err)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      data: { error: 'Incorrect Password' },
    })
  })

  it('falls back to text error responses', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(textResponse('Server down', { status: 500 }))

    await expect(request('/api/entries')).rejects.toMatchObject({
      status: 500,
      data: { error: 'Server down' },
    })
  })
})
