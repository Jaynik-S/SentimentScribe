import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../state/auth'
import { useE2ee } from '../state/e2ee'
import { useUi } from '../state/ui'

export const UnlockPage = () => {
  const [passphrase, setPassphrase] = useState('')
  const [inlineError, setInlineError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const { auth } = useAuth()
  const { unlock } = useE2ee()
  const { setPageError, clearPageError } = useUi()

  if (!auth) {
    return <Navigate to="/" replace />
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    if (!passphrase.trim()) {
      const message = 'Passphrase is required.'
      setInlineError(message)
      setPageError(message)
      inputRef.current?.focus()
      return
    }

    setIsSubmitting(true)
    setInlineError(null)
    clearPageError()

    try {
      await unlock(passphrase, auth.e2eeParams)
      setPassphrase('')
      navigate('/home')
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Unable to unlock.'
      setInlineError(message)
      setPageError(message)
      inputRef.current?.focus()
    } finally {
      setIsSubmitting(false)
    }
  }

  const handlePassphraseChange = (event: ChangeEvent<HTMLInputElement>) => {
    setPassphrase(event.target.value)
    if (inlineError) {
      setInlineError(null)
    }
    clearPageError()
  }

  return (
    <section className="page">
      <div className="verify-card">
        <header className="verify-card__header">
          <p className="eyebrow">SentimentScribe</p>
          <h1>Unlock your diary</h1>
          <p className="subtle">
            Enter your passphrase to decrypt your entries locally.
          </p>
        </header>

        <form className="verify-form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field__label">Passphrase</span>
            <input
              ref={inputRef}
              type="password"
              value={passphrase}
              onChange={handlePassphraseChange}
              disabled={isSubmitting}
              autoComplete="current-password"
            />
          </label>

          {inlineError ? <p className="field__error">{inlineError}</p> : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="button-content">
                <span className="spinner" aria-hidden="true" />
                Unlocking...
              </span>
            ) : (
              'Unlock'
            )}
          </button>
        </form>
      </div>
    </section>
  )
}
