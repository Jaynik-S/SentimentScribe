import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { verifyPassword } from '../api/auth'
import { isApiError } from '../api/http'
import { useAuth } from '../state/auth'
import { useUi } from '../state/ui'

export const VerifyPasswordPage = () => {
  const [password, setPassword] = useState('')
  const [inlineError, setInlineError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const { setUnlocked } = useAuth()
  const { setPageError, clearPageError } = useUi()

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    if (!password.trim()) {
      const message = 'Password is required.'
      setInlineError(message)
      setPageError(message)
      inputRef.current?.focus()
      return
    }

    setIsSubmitting(true)
    setInlineError(null)
    clearPageError()

    try {
      const response = await verifyPassword({ password })
      setUnlocked(true, response.status)
      setPassword('')
      navigate('/home')
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to verify password.'
      setInlineError(message)
      setPageError(message)
      setPassword('')
      inputRef.current?.focus()
    } finally {
      setIsSubmitting(false)
    }
  }

  const handlePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
    setPassword(event.target.value)
    if (inlineError) {
      setInlineError(null)
    }
    clearPageError()
  }

  return (
    <section className="page">
      <div className="verify-card">
        <header className="verify-card__header">
          <p className="eyebrow">MoodVerse</p>
          <h1>Unlock your diary</h1>
          <p className="subtle">
            Enter your password to view or create entries.
          </p>
        </header>

        <form className="verify-form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field__label">Password</span>
            <input
              ref={inputRef}
              type="password"
              value={password}
              onChange={handlePasswordChange}
              disabled={isSubmitting}
              autoComplete="current-password"
            />
          </label>

          {inlineError ? <p className="field__error">{inlineError}</p> : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="button-content">
                <span className="spinner" aria-hidden="true" />
                Verifying...
              </span>
            ) : (
              'Submit / Enter'
            )}
          </button>
        </form>
      </div>
    </section>
  )
}
