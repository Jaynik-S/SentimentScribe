import { beforeEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useLocation } from 'react-router-dom'
import { HomeMenuPage } from '../HomeMenuPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { deleteEntry, listEntries } from '../../api/entries'
import type { EntrySummaryResponse } from '../../api/types'
import { ApiError } from '../../api/http'

vi.mock('../../api/entries', () => ({
  listEntries: vi.fn(),
  deleteEntry: vi.fn(),
}))

const LocationSpy = () => {
  const location = useLocation()
  return <div data-testid="location">{location.pathname + location.search}</div>
}

const entryFixture: EntrySummaryResponse = {
  title: 'Morning Thoughts',
  storagePath: 'entries/morning.txt',
  createdAt: '2025-01-01T10:00:00',
  updatedAt: null,
  keywords: ['calm'],
}

describe('HomeMenuPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loads entries and navigates on row click', async () => {
    const listEntriesMock = vi.mocked(listEntries)
    listEntriesMock.mockResolvedValue([entryFixture])

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
    const deleteEntryMock = vi.mocked(deleteEntry)
    listEntriesMock.mockResolvedValueOnce([entryFixture])
    listEntriesMock.mockResolvedValueOnce([entryFixture])
    deleteEntryMock.mockResolvedValue({
      deleted: true,
      storagePath: entryFixture.storagePath,
    })

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

    expect(deleteEntryMock).toHaveBeenCalledWith(entryFixture.storagePath)
    expect(listEntriesMock).toHaveBeenCalledTimes(2)
  })

  it('shows retry action when list fails', async () => {
    const listEntriesMock = vi.mocked(listEntries)
    listEntriesMock
      .mockRejectedValueOnce(new ApiError(500, 'Server down'))
      .mockResolvedValueOnce([entryFixture])

    const user = userEvent.setup()
    renderWithRouter({
      initialEntries: ['/home'],
      routes: [{ path: '/home', element: <HomeMenuPage /> }],
    })

    expect(await screen.findByText('Server down')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: /retry/i }))

    const entries = await screen.findAllByText('Morning Thoughts')
    expect(entries.length).toBeGreaterThan(0)
  })
})
