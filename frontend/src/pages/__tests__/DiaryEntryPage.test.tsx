import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router-dom'
import { DiaryEntryPage } from '../DiaryEntryPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { analyzeText } from '../../api/analysis'
import { createEntry, getEntryByPath } from '../../api/entries'
import { getRecommendations } from '../../api/recommendations'
import { decryptAesGcm } from '../../crypto/aesGcm'
import { encryptEnvelope } from '../../crypto/envelope'
import { deriveAesKey } from '../../crypto/kdf'

vi.mock('../../api/analysis', () => ({
  analyzeText: vi.fn(),
}))

vi.mock('../../api/entries', () => ({
  createEntry: vi.fn(),
  updateEntry: vi.fn(),
  getEntryByPath: vi.fn(),
}))

vi.mock('../../api/recommendations', () => ({
  getRecommendations: vi.fn(),
}))

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

const LocationSpy = () => {
  const location = useLocation()
  return <div data-testid="location">{location.pathname + location.search}</div>
}

const longText = 'a'.repeat(60)
const passphrase = 'correct horse'
const params = { kdf: 'PBKDF2-SHA256', salt: 'c2FsdA==', iterations: 1 }

describe('DiaryEntryPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    mockKey = await deriveAesKey(passphrase, params.salt, params.iterations)
  })

  it('creates a new entry with generated createdAt', async () => {
    const createEntryMock = vi.mocked(createEntry)
    const responseEnvelope = await encryptEnvelope('Today', longText, mockKey!)
    createEntryMock.mockResolvedValue({
      storagePath: 'entry.txt',
      createdAt: '2025-01-01T12:00:00',
      updatedAt: null,
      ...responseEnvelope,
    })

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/entry'],
      routes: [{ path: '/entry', element: <DiaryEntryPage /> }],
    })

    fireEvent.change(await screen.findByLabelText('Title'), {
      target: { value: 'Today' },
    })
    fireEvent.change(screen.getByLabelText('Entry Text'), {
      target: { value: longText },
    })
    await user.click(screen.getByRole('button', { name: /save entry/i }))

    await waitFor(() => expect(createEntryMock).toHaveBeenCalled())

    const payload = createEntryMock.mock.calls[0][0]
    expect(payload.storagePath).toBeNull()
    expect(payload.createdAt).toMatch(/^\d{4}-\d{2}-\d{2}T/)
    expect(payload.algo).toBe('AES-GCM')
    expect(payload.version).toBe(1)

    const decryptedTitle = await decryptAesGcm(
      payload.titleCiphertext,
      payload.titleIv,
      mockKey!,
    )
    const decryptedBody = await decryptAesGcm(
      payload.bodyCiphertext,
      payload.bodyIv,
      mockKey!,
    )
    expect(decryptedTitle).toBe('Today')
    expect(decryptedBody).toBe(longText)
  })

  it('loads entry when path query param is present', async () => {
    const getEntryByPathMock = vi.mocked(getEntryByPath)
    const responseEnvelope = await encryptEnvelope('Loaded', longText, mockKey!)
    getEntryByPathMock.mockResolvedValue({
      storagePath: 'entry.txt',
      createdAt: '2025-01-01T10:00:00',
      updatedAt: null,
      ...responseEnvelope,
    })

    renderWithRouter({
      initialEntries: ['/entry?path=entry.txt'],
      routes: [{ path: '/entry', element: <DiaryEntryPage /> }],
    })

    await waitFor(() =>
      expect(getEntryByPathMock).toHaveBeenCalledWith('entry.txt'),
    )
    expect(await screen.findByDisplayValue('Loaded')).toBeInTheDocument()
  })

  it('requests keyword analysis on show keywords', async () => {
    const analyzeTextMock = vi.mocked(analyzeText)
    analyzeTextMock.mockResolvedValue({ keywords: ['focus'] })

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/entry'],
      routes: [{ path: '/entry', element: <DiaryEntryPage /> }],
    })

    fireEvent.change(await screen.findByLabelText('Title'), {
      target: { value: 'Focus' },
    })
    fireEvent.change(screen.getByLabelText('Entry Text'), {
      target: { value: longText },
    })
    await user.click(screen.getByRole('button', { name: /show keywords/i }))

    await waitFor(() =>
      expect(analyzeTextMock).toHaveBeenCalledWith({
        text: `Focus\n\n${longText}`,
      }),
    )
    expect(await screen.findByText('focus')).toBeInTheDocument()
  })

  it('requests recommendations and navigates', async () => {
    const getRecommendationsMock = vi.mocked(getRecommendations)
    getRecommendationsMock.mockResolvedValue({
      keywords: [],
      songs: [],
      movies: [],
    })

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/entry'],
      routes: [
        { path: '/entry', element: <DiaryEntryPage /> },
        { path: '/recommendations', element: <LocationSpy /> },
      ],
    })

    fireEvent.change(await screen.findByLabelText('Title'), {
      target: { value: 'Mood' },
    })
    fireEvent.change(screen.getByLabelText('Entry Text'), {
      target: { value: longText },
    })
    await user.click(
      screen.getByRole('button', { name: /get media recommendations/i }),
    )

    expect(getRecommendationsMock).toHaveBeenCalledWith({
      text: `Mood\n\n${longText}`,
    })
    expect((await screen.findByTestId('location')).textContent).toBe(
      '/recommendations',
    )
  })
})
