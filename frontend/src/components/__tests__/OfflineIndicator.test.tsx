import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { OfflineIndicator } from '../OfflineIndicator'

const authState = {
  isAuthenticated: true,
}

const offlineState = {
  isOffline: false,
  pendingCount: 0,
  isSyncing: false,
  syncNow: vi.fn(),
  refreshPendingCount: vi.fn(),
}

vi.mock('../../state/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../state/auth')>()
  return {
    ...actual,
    useAuth: () => ({
      auth: authState.isAuthenticated
        ? {
            accessToken: 'token',
            user: { id: 'user-1', username: 'tester' },
            e2eeParams: { kdf: 'PBKDF2-SHA256', salt: 'c2FsdA==', iterations: 1 },
          }
        : null,
      isAuthenticated: authState.isAuthenticated,
      setAuth: vi.fn(),
      clear: vi.fn(),
    }),
  }
})

vi.mock('../../state/offline', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../state/offline')>()
  return {
    ...actual,
    useOffline: () => offlineState,
  }
})

describe('OfflineIndicator', () => {
  beforeEach(() => {
    authState.isAuthenticated = true
    offlineState.isOffline = false
    offlineState.pendingCount = 0
    offlineState.isSyncing = false
  })

  it('renders nothing when unauthenticated', () => {
    authState.isAuthenticated = false
    render(<OfflineIndicator />)
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('shows offline badge when offline', () => {
    offlineState.isOffline = true
    render(<OfflineIndicator />)
    expect(screen.getByText('Offline')).toBeInTheDocument()
    expect(screen.queryByText(/sync pending/i)).toBeNull()
  })

  it('shows pending count and enables sync when online', () => {
    offlineState.pendingCount = 2
    render(<OfflineIndicator />)
    expect(screen.getByText('Sync pending: 2')).toBeInTheDocument()
    const button = screen.getByRole('button', { name: /sync now/i })
    expect(button).toBeEnabled()
  })

  it('disables sync button while syncing', () => {
    offlineState.pendingCount = 1
    offlineState.isSyncing = true
    render(<OfflineIndicator />)
    const button = screen.getByRole('button', { name: /syncing/i })
    expect(button).toBeDisabled()
  })
})
