import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { register } from '../api/auth'
import { isApiError } from '../api/http'
import { useAuth } from '../state/auth'
import { useUi } from '../state/ui'

export const RegisterPage = () => {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [inlineError, setInlineError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const usernameRef = useRef<HTMLInputElement>(null)
  const passwordRef = useRef<HTMLInputElement>(null)
  const navigate = useNavigate()
  const { setAuth } = useAuth()
  const { setPageError, clearPageError } = useUi()

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()

    if (isSubmitting) {
      return
    }

    if (!username.trim()) {
      const message = 'Username is required.'
      setInlineError(message)
      setPageError(message)
      usernameRef.current?.focus()
      return
    }

    if (!password.trim()) {
      const message = 'Password is required.'
      setInlineError(message)
      setPageError(message)
      passwordRef.current?.focus()
      return
    }

    setIsSubmitting(true)
    setInlineError(null)
    clearPageError()

    try {
      const response = await register({ username: username.trim(), password })
      setAuth({
        accessToken: response.accessToken,
        user: response.user,
        e2eeParams: response.e2ee,
      })
      setPassword('')
      navigate('/home')
    } catch (error) {
      const message = isApiError(error)
        ? error.data.error
        : 'Unable to register.'
      setInlineError(message)
      setPageError(message)
      setPassword('')
      passwordRef.current?.focus()
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleUsernameChange = (event: ChangeEvent<HTMLInputElement>) => {
    setUsername(event.target.value)
    if (inlineError) {
      setInlineError(null)
    }
    clearPageError()
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
          <p className="eyebrow">SentimentScribe</p>
          <h1>Create your account</h1>
          <p className="subtle">Start a private, encrypted diary.</p>
        </header>

        <form className="verify-form" onSubmit={handleSubmit}>
          <label className="field">
            <span className="field__label">Username</span>
            <input
              ref={usernameRef}
              type="text"
              value={username}
              onChange={handleUsernameChange}
              disabled={isSubmitting}
              autoComplete="username"
            />
          </label>

          <label className="field">
            <span className="field__label">Password</span>
            <input
              ref={passwordRef}
              type="password"
              value={password}
              onChange={handlePasswordChange}
              disabled={isSubmitting}
              autoComplete="new-password"
            />
          </label>

          {inlineError ? <p className="field__error">{inlineError}</p> : null}

          <button className="primary-button" type="submit" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="button-content">
                <span className="spinner" aria-hidden="true" />
                Creating account...
              </span>
            ) : (
              'Create account'
            )}
          </button>
        </form>

        <p className="subtle verify-card__footer">
          Already have an account? <Link to="/">Sign in.</Link>
        </p>
      </div>
    </section>
  )
}
