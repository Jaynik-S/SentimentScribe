import type { ErrorResponse } from './types'
import { clearStoredAuth, getAccessToken } from '../state/auth'

const rawBaseUrl = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'
const API_BASE_URL = rawBaseUrl.replace(/\/+$/, '')

export class ApiError extends Error {
  status: number
  data: ErrorResponse

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = { error: message }
  }
}

export const isApiError = (error: unknown): error is ApiError => error instanceof ApiError

export const redirectToLogin = (): void => {
  if (typeof window !== 'undefined') {
    window.location.assign('/')
  }
}

const isJsonResponse = (response: Response): boolean => {
  const contentType = response.headers.get('content-type') ?? ''
  return contentType.includes('application/json')
}

const readErrorMessage = async (response: Response): Promise<string> => {
  if (isJsonResponse(response)) {
    try {
      const data = await response.json()
      if (data && typeof data === 'object' && 'error' in data) {
        const message = (data as ErrorResponse).error
        if (typeof message === 'string' && message.length > 0) {
          return message
        }
      }
      return JSON.stringify(data)
    } catch {
      // Fall through to plain text.
    }
  }

  const text = await response.text()
  if (text.length > 0) {
    return text
  }

  return response.statusText || 'Request failed'
}

const parseJson = async <T>(response: Response): Promise<T> => {
  if (response.status === 204) {
    return undefined as T
  }

  if (isJsonResponse(response)) {
    return (await response.json()) as T
  }

  const text = await response.text()
  return text as unknown as T
}

const withJsonHeaders = (options: RequestInit): RequestInit => {
  if (!options.body) {
    return options
  }

  if (options.body instanceof FormData) {
    return options
  }

  if (options.body instanceof URLSearchParams) {
    return options
  }

  if (options.body instanceof Blob) {
    return options
  }

  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  return { ...options, headers }
}

const buildUrl = (path: string): string => {
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path
  }

  if (path.startsWith('/')) {
    return `${API_BASE_URL}${path}`
  }

  return `${API_BASE_URL}/${path}`
}

const withAuthHeaders = (options: RequestInit): RequestInit => {
  const token = getAccessToken()
  if (!token) {
    return options
  }

  const headers = new Headers(options.headers)
  if (!headers.has('Authorization')) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  return { ...options, headers }
}

export const request = async <T>(path: string, options: RequestInit = {}): Promise<T> => {
  const response = await fetch(buildUrl(path), withAuthHeaders(withJsonHeaders(options)))

  if (!response.ok) {
    const message = await readErrorMessage(response)
    if (response.status === 401) {
      clearStoredAuth()
      redirectToLogin()
    }
    throw new ApiError(response.status, message)
  }

  return parseJson<T>(response)
}

export { API_BASE_URL }
