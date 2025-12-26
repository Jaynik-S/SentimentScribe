import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router-dom'
import { DiaryEntryPage } from '../DiaryEntryPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { analyzeText } from '../../api/analysis'
import { createEntry, getEntryByPath } from '../../api/entries'
import { getRecommendations } from '../../api/recommendations'

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

const LocationSpy = () => {
  const location = useLocation()
  return <div data-testid="location">{location.pathname + location.search}</div>
}

const longText = 'a'.repeat(60)

describe('DiaryEntryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('creates a new entry with generated createdAt', async () => {
    const createEntryMock = vi.mocked(createEntry)
    createEntryMock.mockResolvedValue({
      storagePath: 'entry.txt',
      createdAt: '2025-01-01T12:00:00',
      updatedAt: null,
      titleCiphertext: 'Today',
      titleIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      bodyCiphertext: longText,
      bodyIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      algo: 'AES-GCM',
      version: 1,
    })

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/entry'],
      routes: [{ path: '/entry', element: <DiaryEntryPage /> }],
    })

    await user.type(screen.getByLabelText('Title'), 'Today')
    await user.type(screen.getByLabelText('Entry Text'), longText)
    await user.click(screen.getByRole('button', { name: /save entry/i }))

    expect(createEntryMock).toHaveBeenCalledWith({
      storagePath: null,
      createdAt: expect.stringMatching(/^\d{4}-\d{2}-\d{2}T/),
      titleCiphertext: 'Today',
      titleIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      bodyCiphertext: longText,
      bodyIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      algo: 'AES-GCM',
      version: 1,
    })
  })

  it('loads entry when path query param is present', async () => {
    const getEntryByPathMock = vi.mocked(getEntryByPath)
    getEntryByPathMock.mockResolvedValue({
      storagePath: 'entry.txt',
      createdAt: '2025-01-01T10:00:00',
      updatedAt: null,
      titleCiphertext: 'Loaded',
      titleIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      bodyCiphertext: longText,
      bodyIv: 'AAAAAAAAAAAAAAAAAAAAAA==',
      algo: 'AES-GCM',
      version: 1,
    })

    renderWithRouter({
      initialEntries: ['/entry?path=entry.txt'],
      routes: [{ path: '/entry', element: <DiaryEntryPage /> }],
    })

    expect(getEntryByPathMock).toHaveBeenCalledWith('entry.txt')
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

    await user.type(screen.getByLabelText('Title'), 'Focus')
    await user.type(screen.getByLabelText('Entry Text'), longText)
    await user.click(screen.getByRole('button', { name: /show keywords/i }))

    expect(analyzeTextMock).toHaveBeenCalledWith({
      text: `Focus\n\n${longText}`,
    })
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

    await user.type(screen.getByLabelText('Title'), 'Mood')
    await user.type(screen.getByLabelText('Entry Text'), longText)
    await user.click(
      screen.getByRole('button', { name: /get media recommendations/i }),
    )

    expect(getRecommendationsMock).toHaveBeenCalledWith({
      text: `Mood\n\n${longText}`,
    })
    expect(screen.getByTestId('location').textContent).toBe('/recommendations')
  })
})
