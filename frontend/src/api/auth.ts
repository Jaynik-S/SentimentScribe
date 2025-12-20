import { request } from './http'
import type { AuthRequest, AuthResponse } from './types'

export const verifyPassword = (payload: AuthRequest): Promise<AuthResponse> =>
  request<AuthResponse>('/api/auth/verify', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
