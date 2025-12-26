import { afterEach, describe, expect, it } from 'vitest'
import { screen } from '@testing-library/react'
import { RequireAuth } from './routes'
import { renderWithRouter } from './test/renderWithRouter'

describe('RequireAuth', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('redirects to login when unauthenticated', async () => {
    renderWithRouter({
      routes: [
        { path: '/', element: <div>Login</div> },
        {
          path: '/protected',
          element: (
            <RequireAuth>
              <div>Secret</div>
            </RequireAuth>
          ),
        },
      ],
      initialEntries: ['/protected'],
    })

    expect(await screen.findByText('Login')).toBeInTheDocument()
  })

  it('renders children when authenticated', async () => {
    sessionStorage.setItem('sentimentscribe.auth', JSON.stringify({
      accessToken: 'token',
      user: { id: 'user-id', username: 'demo' },
      e2eeParams: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
    }))

    renderWithRouter({
      routes: [
        { path: '/', element: <div>Login</div> },
        {
          path: '/protected',
          element: (
            <RequireAuth>
              <div>Secret</div>
            </RequireAuth>
          ),
        },
      ],
      initialEntries: ['/protected'],
    })

    expect(await screen.findByText('Secret')).toBeInTheDocument()
  })
})
