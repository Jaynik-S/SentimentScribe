import { describe, expect, it } from 'vitest'
import { ApiError, request } from '../http'
import { jsonResponse, mockFetch, textResponse } from '../../test/mockFetch'

describe('api/http', () => {
  it('throws ApiError with parsed error response', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ error: 'Incorrect Password' }, { status: 400 }),
    )

    await expect(request('/api/auth/verify')).rejects.toBeInstanceOf(ApiError)
    await expect(request('/api/auth/verify')).rejects.toMatchObject({
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
