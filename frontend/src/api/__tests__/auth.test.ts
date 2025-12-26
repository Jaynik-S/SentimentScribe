import { describe, expect, it } from 'vitest'
import { login, register } from '../auth'
import { jsonResponse, mockFetch } from '../../test/mockFetch'

describe('api/auth', () => {
  it('posts login payload', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(jsonResponse({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 'user-id', username: 'demo' },
      e2ee: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
    }))

    await login({ username: 'demo', password: 'secret' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/auth/login')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ username: 'demo', password: 'secret' }))
  })

  it('posts register payload', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(jsonResponse({
      accessToken: 'token',
      tokenType: 'Bearer',
      expiresIn: 3600,
      user: { id: 'user-id', username: 'demo' },
      e2ee: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
    }))

    await register({ username: 'demo', password: 'secret' })

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/auth/register')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify({ username: 'demo', password: 'secret' }))
  })
})
