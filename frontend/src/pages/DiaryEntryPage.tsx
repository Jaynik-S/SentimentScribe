import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { analyzeText } from '../api/analysis'
import { getEntryByPath } from '../api/entries'
import { isApiError } from '../api/http'
import { getRecommendations } from '../api/recommendations'
import { formatLocalDateTime } from '../api/localDateTime'
import type { EntryRequest } from '../api/types'
import { KeywordsDropdown } from '../components/KeywordsDropdown'
import { decryptEntry, encryptEntry } from '../crypto/diaryCrypto'
import { getEntry, upsertEntry } from '../offline/entriesRepo'
import { flushSyncQueue } from '../offline/syncEngine'
import { enqueueSyncItem } from '../offline/syncQueueRepo'
import { useAuth } from '../state/auth'
import { useEntryDraft } from '../state/entryDraft'
import { useE2ee } from '../state/e2ee'
import { useOffline } from '../state/offline'
import { useRecommendations } from '../state/recommendations'
import { useUi } from '../state/ui'

const TOAST_DURATION_MS = 3000

const formatDisplayDate = (value: string | null): string => {
  if (!value) {
    return '—'
  }

  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }

  return parsed.toLocaleString()
}

const validateTitle = (value: string): string | null => {
  const trimmed = value.trim()
  if (!trimmed) {
    return 'Title is required.'
  }
  if (trimmed.length > 30) {
    return 'Title must be 30 characters or fewer.'
  }
  return null
}

const validateText = (value: string): string | null => {
  const trimmed = value.trim()
  if (!trimmed) {
    return 'Text is required.'
  }
  if (trimmed.length < 100) {
    return 'Text must be at least 100 characters.'
  }
  if (trimmed.length > 5000) {
    return 'Text must be 5000 characters or fewer.'
  }
  return null
}

const createStoragePath = (): string => {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return `entries/${crypto.randomUUID()}.json`
  }

  const fallback = `${Date.now()}-${Math.random().toString(16).slice(2)}`
  return `entries/${fallback}.json`
}

export const DiaryEntryPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const {
    draft,
    updateDraft,
    setDraft,
    keywordsVisible,
    setKeywordsVisible,
    clearDraft,
  } = useEntryDraft()
  const { setRecommendations } = useRecommendations()
  const { setPageError, clearPageError, setPageLoading } = useUi()
  const { key, clear } = useE2ee()
  const { auth } = useAuth()
  const { isOffline, refreshPendingCount } = useOffline()
  const [titleError, setTitleError] = useState<string | null>(null)
  const [textError, setTextError] = useState<string | null>(null)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isRecommending, setIsRecommending] = useState(false)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const [lastSavedAt, setLastSavedAt] = useState<string | null>(null)
  const [lastAnalyzedAt, setLastAnalyzedAt] = useState<string | null>(null)
  const savedSnapshotRef = useRef<{
    title: string
    text: string
    storagePath: string | null
  } | null>(null)
  const createdAtRef = useRef(draft.createdAt)

  const entryPath = searchParams.get('path')
  const userId = auth?.user.id ?? null

  useEffect(() => {
    createdAtRef.current = draft.createdAt
  }, [draft.createdAt])

  useEffect(() => {
    if (!entryPath) {
      savedSnapshotRef.current = null
      setLastSavedAt(null)
      setLastAnalyzedAt(null)
    }
  }, [entryPath])

  useEffect(() => {
    if (!entryPath) {
      return
    }
    if (!key) {
      setPageError('Unlock your diary to view entries.')
      return
    }
    if (!userId) {
      setPageError('Unable to load entry.')
      return
    }

    const loadEntry = async () => {
      setPageLoading(true)
      clearPageError()

      try {
        const cached = await getEntry(userId, entryPath)
        if (
          cached &&
          !cached.deletedAt &&
          cached.bodyCiphertext &&
          cached.bodyIv
        ) {
          try {
            const decrypted = await decryptEntry(
              {
                titleCiphertext: cached.titleCiphertext,
                titleIv: cached.titleIv,
                bodyCiphertext: cached.bodyCiphertext,
                bodyIv: cached.bodyIv,
                algo: cached.algo,
                version: cached.version,
              },
              key,
            )
            setDraft({
              title: decrypted.title,
              text: decrypted.body,
              storagePath: cached.storagePath,
              createdAt: cached.createdAt ?? createdAtRef.current,
              keywords: [],
            })
            savedSnapshotRef.current = {
              title: decrypted.title,
              text: decrypted.body,
              storagePath: cached.storagePath,
            }
            setLastSavedAt(cached.updatedAt ?? cached.createdAt ?? null)
            setLastAnalyzedAt(null)
            setKeywordsVisible(false)
            return
          } catch {
            // Fall back to API when cached payload cannot be decrypted.
          }
        }

        if (isOffline) {
          throw new Error('Entry not available offline yet.')
        }

        const response = await getEntryByPath(entryPath)
        await upsertEntry({
          userId,
          storagePath: response.storagePath,
          createdAt: response.createdAt ?? createdAtRef.current ?? null,
          updatedAt: response.updatedAt ?? null,
          titleCiphertext: response.titleCiphertext,
          titleIv: response.titleIv,
          bodyCiphertext: response.bodyCiphertext,
          bodyIv: response.bodyIv,
          algo: response.algo,
          version: response.version,
          dirty: false,
          deletedAt: null,
        })

        const decrypted = await decryptEntry(
          {
            titleCiphertext: response.titleCiphertext,
            titleIv: response.titleIv,
            bodyCiphertext: response.bodyCiphertext,
            bodyIv: response.bodyIv,
            algo: response.algo,
            version: response.version,
          },
          key,
        )
        setDraft({
          title: decrypted.title,
          text: decrypted.body,
          storagePath: response.storagePath,
          createdAt: response.createdAt ?? createdAtRef.current,
          keywords: [],
        })
        savedSnapshotRef.current = {
          title: decrypted.title,
          text: decrypted.body,
          storagePath: response.storagePath,
        }
        setLastSavedAt(response.updatedAt ?? response.createdAt ?? null)
        setLastAnalyzedAt(null)
        setKeywordsVisible(false)
      } catch (error) {
        const message =
          error instanceof Error
            ? error.message
            : isApiError(error)
              ? error.data.error
              : 'Unable to load entry.'
        setPageError(message, {
          label: 'Back to Home',
          onClick: () => navigate('/home'),
        })
      } finally {
        setPageLoading(false)
      }
    }

    void loadEntry()
  }, [
    clearPageError,
    entryPath,
    isOffline,
    key,
    navigate,
    setDraft,
    setKeywordsVisible,
    setPageError,
    setPageLoading,
    userId,
  ])

  useEffect(() => {
    if (!toastMessage) {
      return
    }

    const timeout = window.setTimeout(() => {
      setToastMessage(null)
    }, TOAST_DURATION_MS)

    return () => {
      window.clearTimeout(timeout)
    }
  }, [toastMessage])

  const handleTitleChange = (event: ChangeEvent<HTMLInputElement>) => {
    updateDraft({ title: event.target.value })
    if (titleError) {
      setTitleError(null)
    }
    clearPageError()
  }

  const handleTextChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    updateDraft({ text: event.target.value })
    if (textError) {
      setTextError(null)
    }
    clearPageError()
  }

  const handleKeywordsToggle = async () => {
    if (isAnalyzing) {
      return
    }

    if (keywordsVisible) {
      setKeywordsVisible(false)
      return
    }

    setIsAnalyzing(true)
    clearPageError()

    try {
      const response = await analyzeText({
        text: `${draft.title}\n\n${draft.text}`,
      })
      updateDraft({ keywords: response.keywords })
      setKeywordsVisible(true)
      setLastAnalyzedAt(formatLocalDateTime(new Date()))
      setToastMessage('Keywords updated.')
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to analyze keywords.'
      setPageError(message)
    } finally {
      setIsAnalyzing(false)
    }
  }

  const handleSave = async () => {
    if (isSaving) {
      return
    }
    if (!key) {
      setPageError('Unlock your diary to save entries.')
      return
    }
    if (!userId) {
      setPageError('Unable to save entry.')
      return
    }

    const nextTitleError = validateTitle(draft.title)
    const nextTextError = validateText(draft.text)

    setTitleError(nextTitleError)
    setTextError(nextTextError)

    if (nextTitleError || nextTextError) {
      return
    }

    setIsSaving(true)
    clearPageError()

    const createdAt = draft.createdAt ?? formatLocalDateTime(new Date())
    if (!draft.createdAt) {
      updateDraft({ createdAt })
    }

    try {
      const storagePath = draft.storagePath ?? createStoragePath()
      if (!draft.storagePath) {
        updateDraft({ storagePath })
      }

      const encrypted = await encryptEntry(draft.title, draft.text, key)
      const payload: EntryRequest = {
        storagePath,
        createdAt,
        titleCiphertext: encrypted.titleCiphertext,
        titleIv: encrypted.titleIv,
        bodyCiphertext: encrypted.bodyCiphertext,
        bodyIv: encrypted.bodyIv,
        algo: encrypted.algo,
        version: encrypted.version,
      }
      await upsertEntry({
        userId,
        storagePath,
        createdAt,
        updatedAt: null,
        titleCiphertext: payload.titleCiphertext,
        titleIv: payload.titleIv,
        bodyCiphertext: payload.bodyCiphertext,
        bodyIv: payload.bodyIv,
        algo: payload.algo,
        version: payload.version,
        dirty: true,
        deletedAt: null,
      })
      await enqueueSyncItem({
        userId,
        op: 'upsert',
        storagePath,
        payload,
        enqueuedAt: formatLocalDateTime(new Date()),
        retryCount: 0,
        lastAttemptAt: null,
        lastError: null,
      })
      if (!isOffline) {
        await flushSyncQueue(userId)
      }
      await refreshPendingCount()
      setDraft({
        title: draft.title,
        text: draft.text,
        storagePath,
        createdAt,
        keywords: draft.keywords,
      })
      savedSnapshotRef.current = {
        title: draft.title,
        text: draft.text,
        storagePath,
      }
      const savedAt = formatLocalDateTime(new Date())
      setLastSavedAt(savedAt)
      setToastMessage('Saved safely.')
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isApiError(error)
            ? error.data.error
            : 'Unable to save entry.'
      const lowered = message.toLowerCase()
      if (lowered.startsWith('title')) {
        setTitleError(message)
      }
      if (lowered.startsWith('text') || lowered.startsWith('body')) {
        setTextError(message)
      }
      setPageError(message)
    } finally {
      setIsSaving(false)
    }
  }

  const handleRecommendations = async () => {
    if (isRecommending) {
      return
    }

    setIsRecommending(true)
    clearPageError()

    try {
      const requestText = `${draft.title}\n\n${draft.text}`
      const response = await getRecommendations({
        text: requestText,
      })
      setRecommendations(response, requestText)
      const trimmedTitle = draft.title.trim()
      navigate('/recommendations', {
        state: {
          entryTitle: trimmedTitle || 'Untitled entry',
          hasUnsavedChanges,
          toast: 'Recommendations ready.',
        },
      })
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to get recommendations.'
      setPageError(message)
    } finally {
      setIsRecommending(false)
    }
  }

  const handleLock = () => {
    clearDraft()
    clear()
    navigate('/unlock')
  }

  const hasUnsavedChanges = useMemo(() => {
    const snapshot = savedSnapshotRef.current
    if (!snapshot) {
      return Boolean(draft.title.trim() || draft.text.trim())
    }
    return snapshot.title !== draft.title || snapshot.text !== draft.text
  }, [draft.text, draft.title])

  const keywordsLabel = keywordsVisible ? 'Hide Keywords' : 'Show Keywords'
  const updatedDetail = formatDisplayDate(lastSavedAt)
  const analyzedDetail = lastAnalyzedAt
    ? formatDisplayDate(lastAnalyzedAt)
    : 'Not analyzed yet'

  const metadata = useMemo(
    () => [{ label: 'Created', value: formatDisplayDate(draft.createdAt) }],
    [draft.createdAt],
  )

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">SentimentScribe</p>
          <h1>Diary Entry</h1>
        </div>
        <div className="page-header__actions">
          <div className="page-header__nav">
            <button
              className="ghost-button"
              type="button"
              onClick={() => navigate('/home')}
            >
              Home
            </button>
            <button
              className="ghost-button danger"
              type="button"
              onClick={handleLock}
            >
              Lock
            </button>
          </div>
        </div>
      </header>

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}

      <div className="entry-layout">
        <div className="entry-form">
          <div className="entry-form__header">
            <label className="field field--title">
              <span className="field__label">Title</span>
              <input
                type="text"
                value={draft.title}
                onChange={handleTitleChange}
              />
            </label>
            {titleError ? <p className="field__error">{titleError}</p> : null}

            <div className="keywords-row">
              <button
                type="button"
                className="secondary-button"
                onClick={handleKeywordsToggle}
                disabled={isAnalyzing}
              >
                {isAnalyzing ? (
                  <span className="button-content">
                    <span className="spinner" aria-hidden="true" />
                    Reflecting...
                  </span>
                ) : (
                  <span className="button-content">
                    <svg
                      viewBox="0 0 20 20"
                      className="icon"
                      aria-hidden="true"
                    >
                      <path
                        d="M4.5 6.5h6.5a3 3 0 0 1 0 6H9l-2.5 3V12.5H4.5a3 3 0 0 1 0-6Z"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="1.5"
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </svg>
                    {keywordsLabel}
                  </span>
                )}
              </button>
              <span className="keywords-hint">
                Use keywords to guide recommendations.
              </span>
            </div>

            {keywordsVisible ? (
              <KeywordsDropdown keywords={draft.keywords} />
            ) : null}
          </div>

          <label className="field field--text">
            <span className="field__label">Entry Text</span>
            <textarea
              value={draft.text}
              onChange={handleTextChange}
              rows={12}
            />
          </label>
          {textError ? <p className="field__error">{textError}</p> : null}

          <div className="entry-actions">
            <button
              type="button"
              className="primary-button"
              onClick={handleSave}
              disabled={isSaving}
            >
              {isSaving ? (
                <span className="button-content">
                  <span className="spinner" aria-hidden="true" />
                  Saving...
                </span>
              ) : (
                'Save Entry'
              )}
            </button>
            <button
              type="button"
              className="secondary-button"
              onClick={handleRecommendations}
              disabled={isRecommending}
            >
              {isRecommending ? (
                <span className="button-content">
                  <span className="spinner" aria-hidden="true" />
                  Gathering...
                </span>
              ) : (
                'Get Media Recommendations'
              )}
            </button>
          </div>
        </div>

        <aside className="entry-meta">
          <h2>Entry Details</h2>
          <dl>
            <div className="entry-meta__row">
              <dt>Status</dt>
              <dd>
                <span
                  className={`status-pill ${
                    hasUnsavedChanges
                      ? 'status-pill--draft'
                      : 'status-pill--saved'
                  }`}
                >
                  {hasUnsavedChanges ? 'Unsaved changes' : 'Saved'}
                </span>
              </dd>
            </div>
            <div className="entry-meta__row">
              <dt>Updated</dt>
              <dd>{updatedDetail}</dd>
            </div>
            <div className="entry-meta__row">
              <dt>Analyzed</dt>
              <dd>{analyzedDetail}</dd>
            </div>
            {metadata.map((item) => (
              <div key={item.label} className="entry-meta__row">
                <dt>{item.label}</dt>
                <dd>{item.value}</dd>
              </div>
            ))}
          </dl>
        </aside>
      </div>
    </section>
  )
}
