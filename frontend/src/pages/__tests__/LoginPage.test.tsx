import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { LoginPage } from '../LoginPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { login } from '../../api/auth'
import { ApiError } from '../../api/http'

vi.mock('../../api/auth', () => ({
  login: vi.fn(),
}))

const authResponse = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 'user-id', username: 'demo' },
  e2ee: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
}

describe('LoginPage', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('submits credentials and navigates to home', async () => {
    const loginMock = vi.mocked(login)
    loginMock.mockResolvedValue(authResponse)

    const user = userEvent.setup()
    renderWithRouter({
      routes: [
        { path: '/', element: <LoginPage /> },
        { path: '/home', element: <div>Home</div> },
      ],
    })

    await user.type(screen.getByLabelText(/username/i), 'demo')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(loginMock).toHaveBeenCalledWith({ username: 'demo', password: 'secret' })
    expect(await screen.findByText('Home')).toBeInTheDocument()
    const stored = sessionStorage.getItem('sentimentscribe.auth')
    expect(stored).not.toBeNull()
    expect(JSON.parse(stored ?? '{}')).toMatchObject({
      accessToken: 'token',
      user: { id: 'user-id', username: 'demo' },
    })
  })

  it('shows backend error when login fails', async () => {
    const loginMock = vi.mocked(login)
    loginMock.mockRejectedValue(new ApiError(400, 'Incorrect Password'))

    const user = userEvent.setup()
    renderWithRouter({
      routes: [{ path: '/', element: <LoginPage /> }],
    })

    await user.type(screen.getByLabelText(/username/i), 'demo')
    await user.type(screen.getByLabelText(/password/i), 'bad')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    expect(
      await screen.findAllByText('Incorrect Password'),
    ).toHaveLength(2)
  })
})
