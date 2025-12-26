import { request } from './http'
import type { AuthTokenResponse, LoginRequest, RegisterRequest } from './types'

export const login = (payload: LoginRequest): Promise<AuthTokenResponse> =>
  request<AuthTokenResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const register = (payload: RegisterRequest): Promise<AuthTokenResponse> =>
  request<AuthTokenResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
