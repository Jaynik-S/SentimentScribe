import { describe, expect, it } from 'vitest'
import {
  createEntry,
  deleteEntry,
  getEntryByPath,
  listEntries,
  updateEntry,
} from '../entries'
import type { EntryRequest } from '../types'
import { jsonResponse, mockFetch } from '../../test/mockFetch'

describe('api/entries', () => {
  it('lists entries', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(jsonResponse([]))

    await listEntries()

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/entries')
    expect(options).toEqual({})
  })

  it('gets entry by path with encoded query', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        title: 'Entry',
        text: 'Text',
        storagePath: 'folder/entry.txt',
        createdAt: null,
        updatedAt: null,
        keywords: [],
      }),
    )

    await getEntryByPath('folder/entry.txt')

    const [url] = fetchMock.mock.calls[0]
    expect(url).toBe(
      'http://localhost:8080/api/entries/by-path?path=folder%2Fentry.txt',
    )
  })

  it('creates an entry', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        title: 'Entry',
        text: 'Text',
        storagePath: 'entry.txt',
        createdAt: null,
        updatedAt: null,
        keywords: [],
      }, { status: 201 }),
    )

    const payload: EntryRequest = {
      title: 'Entry',
      text: 'Text',
      storagePath: null,
      keywords: [],
      createdAt: null,
    }

    await createEntry(payload)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/entries')
    expect(options?.method).toBe('POST')
    expect(options?.body).toBe(JSON.stringify(payload))
    expect(new Headers(options?.headers).get('Content-Type')).toBe(
      'application/json',
    )
  })

  it('updates an entry', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        title: 'Entry',
        text: 'Text',
        storagePath: 'entry.txt',
        createdAt: null,
        updatedAt: null,
        keywords: [],
      }),
    )

    const payload: EntryRequest = {
      title: 'Entry',
      text: 'Text',
      storagePath: 'entry.txt',
      keywords: [],
      createdAt: null,
    }

    await updateEntry(payload)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/entries')
    expect(options?.method).toBe('PUT')
    expect(options?.body).toBe(JSON.stringify(payload))
  })

  it('deletes an entry', async () => {
    const fetchMock = mockFetch()
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ deleted: true, storagePath: 'entry.txt' }),
    )

    await deleteEntry('entry.txt')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('http://localhost:8080/api/entries?path=entry.txt')
    expect(options?.method).toBe('DELETE')
  })
})
