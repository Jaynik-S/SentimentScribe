import { useEffect, useMemo, useRef, useState } from 'react'
import type { ChangeEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { analyzeText } from '../api/analysis'
import { createEntry, getEntryByPath, updateEntry } from '../api/entries'
import { isApiError } from '../api/http'
import { getRecommendations } from '../api/recommendations'
import { formatLocalDateTime } from '../api/localDateTime'
import type { EntryRequest } from '../api/types'
import { KeywordsDropdown } from '../components/KeywordsDropdown'
import { decryptEnvelope, encryptEnvelope } from '../crypto/envelope'
import { useEntryDraft } from '../state/entryDraft'
import { useE2ee } from '../state/e2ee'
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
  if (trimmed.length < 50) {
    return 'Text must be at least 50 characters.'
  }
  if (trimmed.length > 1000) {
    return 'Text must be 1000 characters or fewer.'
  }
  return null
}

export const DiaryEntryPage = () => {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { draft, updateDraft, setDraft, keywordsVisible, setKeywordsVisible } =
    useEntryDraft()
  const { setRecommendations } = useRecommendations()
  const { setPageError, clearPageError, setPageLoading } = useUi()
  const { key } = useE2ee()
  const [titleError, setTitleError] = useState<string | null>(null)
  const [textError, setTextError] = useState<string | null>(null)
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [isRecommending, setIsRecommending] = useState(false)
  const [toastMessage, setToastMessage] = useState<string | null>(null)
  const createdAtRef = useRef(draft.createdAt)

  const entryPath = searchParams.get('path')

  useEffect(() => {
    createdAtRef.current = draft.createdAt
  }, [draft.createdAt])

  useEffect(() => {
    if (!entryPath) {
      return
    }
    if (!key) {
      setPageError('Unlock your diary to view entries.')
      return
    }

    const loadEntry = async () => {
      setPageLoading(true)
      clearPageError()

      try {
        const response = await getEntryByPath(entryPath)
        const decrypted = await decryptEnvelope(
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
        setKeywordsVisible(false)
      } catch (error) {
        const message = isApiError(error)
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
    key,
    navigate,
    setDraft,
    setKeywordsVisible,
    setPageError,
    setPageLoading,
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
      const encrypted = await encryptEnvelope(draft.title, draft.text, key)
      const payload: EntryRequest = {
        storagePath: draft.storagePath,
        createdAt,
        titleCiphertext: encrypted.titleCiphertext,
        titleIv: encrypted.titleIv,
        bodyCiphertext: encrypted.bodyCiphertext,
        bodyIv: encrypted.bodyIv,
        algo: encrypted.algo,
        version: encrypted.version,
      }
      const response = draft.storagePath
        ? await updateEntry(payload)
        : await createEntry(payload)

      setDraft({
        title: draft.title,
        text: draft.text,
        storagePath: response.storagePath,
        createdAt: response.createdAt ?? createdAt,
        keywords: draft.keywords,
      })
      setToastMessage('Saved')
    } catch (error) {
      const message = isApiError(error)
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
      const response = await getRecommendations({
        text: `${draft.title}\n\n${draft.text}`,
      })
      setRecommendations(response)
      navigate('/recommendations')
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to get recommendations.'
      setPageError(message)
    } finally {
      setIsRecommending(false)
    }
  }

  const keywordsLabel = keywordsVisible ? 'Hide Keywords' : 'Show Keywords'

  const metadata = useMemo(
    () => [
      { label: 'Created At', value: formatDisplayDate(draft.createdAt) },
      { label: 'Storage Path', value: draft.storagePath ?? '—' },
    ],
    [draft.createdAt, draft.storagePath],
  )

  return (
    <section className="page">
      <header className="page-header">
        <div>
          <p className="eyebrow">SentimentScribe</p>
          <h1>Diary Entry</h1>
          <p className="subtle">Draft, analyze, and save your entry.</p>
        </div>
        <button
          className="secondary-button"
          type="button"
          onClick={() => navigate('/home')}
        >
          Back to Home
        </button>
      </header>

      {toastMessage ? <div className="toast">{toastMessage}</div> : null}

      <div className="entry-layout">
        <div className="entry-form">
          <label className="field">
            <span className="field__label">Title</span>
            <input
              type="text"
              value={draft.title}
              onChange={handleTitleChange}
            />
          </label>
          {titleError ? <p className="field__error">{titleError}</p> : null}

          <div className="keywords-section">
            <button
              type="button"
              className="secondary-button"
              onClick={handleKeywordsToggle}
              disabled={isAnalyzing}
            >
              {isAnalyzing ? (
                <span className="button-content">
                  <span className="spinner" aria-hidden="true" />
                  Analyzing...
                </span>
              ) : (
                keywordsLabel
              )}
            </button>

            {keywordsVisible ? (
              <KeywordsDropdown keywords={draft.keywords} />
            ) : null}
          </div>

          <label className="field">
            <span className="field__label">Entry Text</span>
            <textarea
              value={draft.text}
              onChange={handleTextChange}
              rows={10}
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
                  Fetching...
                </span>
              ) : (
                'Get Media Recommendations'
              )}
            </button>
          </div>
        </div>

        <aside className="entry-meta">
          <h2>Entry Metadata</h2>
          <dl>
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
