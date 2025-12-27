import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router-dom'
import { HomeMenuPage } from '../HomeMenuPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { listEntries } from '../../api/entries'
import type { EntrySummaryResponse } from '../../api/types'
import { ApiError } from '../../api/http'
import { deriveKey, encrypt } from '../../crypto/diaryCrypto'
import { getEntry, listEntriesByUser, markEntryDeleted } from '../../offline/entriesRepo'
import { flushSyncQueue } from '../../offline/syncEngine'
import { enqueueDelete } from '../../offline/syncQueueRepo'

vi.mock('../../api/entries', () => ({
  listEntries: vi.fn(),
}))

vi.mock('../../offline/entriesRepo', () => ({
  getEntry: vi.fn(),
  listEntriesByUser: vi.fn(),
  markEntryDeleted: vi.fn(),
  upsertEntry: vi.fn(),
}))

vi.mock('../../offline/syncEngine', () => ({
  flushSyncQueue: vi.fn(),
}))

vi.mock('../../offline/syncQueueRepo', () => ({
  enqueueDelete: vi.fn(),
}))

vi.mock('../../state/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../state/auth')>()
  return {
    ...actual,
    useAuth: () => ({
      auth: {
        accessToken: 'token',
        user: { id: 'user-1', username: 'tester' },
        e2eeParams: { kdf: 'PBKDF2-SHA256', salt: 'c2FsdA==', iterations: 1 },
      },
      isAuthenticated: true,
      setAuth: vi.fn(),
      clear: vi.fn(),
    }),
  }
})

const LocationSpy = () => {
  const location = useLocation()
  return <div data-testid="location">{location.pathname + location.search}</div>
}

const passphrase = 'correct horse'
const params = { kdf: 'PBKDF2-SHA256', salt: 'c2FsdA==', iterations: 1 }

let mockKey: CryptoKey | null = null

vi.mock('../../state/e2ee', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../state/e2ee')>()
  return {
    ...actual,
    useE2ee: () => ({
      key: mockKey,
      isUnlocked: Boolean(mockKey),
      unlock: vi.fn(),
      clear: vi.fn(),
    }),
  }
})

vi.mock('../../state/offline', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../state/offline')>()
  return {
    ...actual,
    useOffline: () => ({
      isOffline: false,
      pendingCount: 0,
      isSyncing: false,
      syncNow: vi.fn(),
      refreshPendingCount: vi.fn(),
    }),
  }
})

let entryFixture: EntrySummaryResponse

describe('HomeMenuPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    mockKey = await deriveKey(passphrase, params)
    const encryptedTitle = await encrypt('Morning Thoughts', mockKey!)
    entryFixture = {
      storagePath: 'entries/morning.txt',
      createdAt: '2025-01-01T10:00:00',
      updatedAt: null,
      titleCiphertext: encryptedTitle.ciphertext,
      titleIv: encryptedTitle.iv,
      algo: encryptedTitle.algo,
      version: encryptedTitle.version,
    }
  })

  it('loads entries and navigates on row click', async () => {
    const listEntriesMock = vi.mocked(listEntries)
    const listEntriesByUserMock = vi.mocked(listEntriesByUser)
    listEntriesByUserMock.mockResolvedValue([
      {
        userId: 'user-1',
        storagePath: entryFixture.storagePath,
        createdAt: entryFixture.createdAt,
        updatedAt: entryFixture.updatedAt,
        titleCiphertext: entryFixture.titleCiphertext,
        titleIv: entryFixture.titleIv,
        bodyCiphertext: null,
        bodyIv: null,
        algo: entryFixture.algo,
        version: entryFixture.version,
        dirty: false,
        deletedAt: null,
      },
    ])
    listEntriesMock.mockResolvedValue([])

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/home'],
      routes: [
        { path: '/home', element: <HomeMenuPage /> },
        { path: '/entry', element: <LocationSpy /> },
      ],
    })

    expect(await screen.findByText('Morning Thoughts')).toBeInTheDocument()

    const titleCell = screen.getByText('Morning Thoughts')
    await user.click(titleCell)

    expect(screen.getByTestId('location').textContent).toBe(
      '/entry?path=entries%2Fmorning.txt',
    )
  })

  it('opens delete modal and confirms delete', async () => {
    const listEntriesMock = vi.mocked(listEntries)
    const getEntryMock = vi.mocked(getEntry)
    const markEntryDeletedMock = vi.mocked(markEntryDeleted)
    const enqueueDeleteMock = vi.mocked(enqueueDelete)
    const flushSyncQueueMock = vi.mocked(flushSyncQueue)
    const listEntriesByUserMock = vi.mocked(listEntriesByUser)
    listEntriesByUserMock.mockResolvedValue([
      {
        userId: 'user-1',
        storagePath: entryFixture.storagePath,
        createdAt: entryFixture.createdAt,
        updatedAt: entryFixture.updatedAt,
        titleCiphertext: entryFixture.titleCiphertext,
        titleIv: entryFixture.titleIv,
        bodyCiphertext: null,
        bodyIv: null,
        algo: entryFixture.algo,
        version: entryFixture.version,
        dirty: false,
        deletedAt: null,
      },
    ])
    listEntriesMock.mockResolvedValue([])
    getEntryMock.mockResolvedValue({
      userId: 'user-1',
      storagePath: entryFixture.storagePath,
      createdAt: entryFixture.createdAt,
      updatedAt: entryFixture.updatedAt,
      titleCiphertext: entryFixture.titleCiphertext,
      titleIv: entryFixture.titleIv,
      bodyCiphertext: null,
      bodyIv: null,
      algo: entryFixture.algo,
      version: entryFixture.version,
      dirty: false,
      deletedAt: null,
    })
    markEntryDeletedMock.mockResolvedValue()
    enqueueDeleteMock.mockResolvedValue(1)
    flushSyncQueueMock.mockResolvedValue()

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/home'],
      routes: [{ path: '/home', element: <HomeMenuPage /> }],
    })

    await screen.findByText('Morning Thoughts')

    await user.click(screen.getByRole('button', { name: /^delete$/i }))
    expect(
      screen.getByRole('button', { name: /confirm delete/i }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /confirm delete/i }))

    expect(markEntryDeletedMock).toHaveBeenCalled()
    expect(enqueueDeleteMock).toHaveBeenCalledWith(
      'user-1',
      entryFixture.storagePath,
      expect.any(String),
    )
    expect(flushSyncQueueMock).toHaveBeenCalledWith('user-1')
    expect(listEntriesByUserMock).toHaveBeenCalled()
  })

  it('shows retry action when list fails', async () => {
    const listEntriesMock = vi.mocked(listEntries)
    const listEntriesByUserMock = vi.mocked(listEntriesByUser)
    listEntriesByUserMock.mockResolvedValue([
      {
        userId: 'user-1',
        storagePath: entryFixture.storagePath,
        createdAt: entryFixture.createdAt,
        updatedAt: entryFixture.updatedAt,
        titleCiphertext: entryFixture.titleCiphertext,
        titleIv: entryFixture.titleIv,
        bodyCiphertext: null,
        bodyIv: null,
        algo: entryFixture.algo,
        version: entryFixture.version,
        dirty: false,
        deletedAt: null,
      },
    ])
    listEntriesMock
      .mockRejectedValueOnce(new ApiError(500, 'Server down'))
      .mockResolvedValueOnce([])

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/home'],
      routes: [{ path: '/home', element: <HomeMenuPage /> }],
    })

    expect(await screen.findByText('Server down')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /retry/i }))

    expect(listEntriesMock).toHaveBeenCalledTimes(2)
  })
})
