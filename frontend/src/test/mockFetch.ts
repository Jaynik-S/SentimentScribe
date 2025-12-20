import { vi } from 'vitest'

export const jsonResponse = (data: unknown, init?: ResponseInit): Response => {
  const headers = new Headers(init?.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return new Response(JSON.stringify(data), {
    ...init,
    headers,
  })
}

export const textResponse = (data: string, init?: ResponseInit): Response => {
  return new Response(data, init)
}

export const mockFetch = () => {
  const fetchMock = vi.fn()
  globalThis.fetch = fetchMock as typeof fetch
  return fetchMock
}
