import { afterEach, describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { RegisterPage } from '../RegisterPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { register } from '../../api/auth'
import { ApiError } from '../../api/http'

vi.mock('../../api/auth', () => ({
  register: vi.fn(),
}))

const authResponse = {
  accessToken: 'token',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: { id: 'user-id', username: 'new-user' },
  e2ee: { kdf: 'PBKDF2-SHA256', salt: 'salt', iterations: 1 },
}

describe('RegisterPage', () => {
  afterEach(() => {
    sessionStorage.clear()
  })

  it('submits registration and navigates to home', async () => {
    const registerMock = vi.mocked(register)
    registerMock.mockResolvedValue(authResponse)

    const user = userEvent.setup()
    renderWithRouter({
      routes: [
        { path: '/register', element: <RegisterPage /> },
        { path: '/home', element: <div>Home</div> },
      ],
      initialEntries: ['/register'],
    })

    await user.type(screen.getByLabelText(/username/i), 'new-user')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(registerMock).toHaveBeenCalledWith({ username: 'new-user', password: 'secret' })
    expect(await screen.findByText('Home')).toBeInTheDocument()
  })

  it('shows backend error when registration fails', async () => {
    const registerMock = vi.mocked(register)
    registerMock.mockRejectedValue(new ApiError(400, 'Username already exists'))

    const user = userEvent.setup()
    renderWithRouter({
      routes: [{ path: '/register', element: <RegisterPage /> }],
      initialEntries: ['/register'],
    })

    await user.type(screen.getByLabelText(/username/i), 'taken')
    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    expect(
      await screen.findAllByText('Username already exists'),
    ).toHaveLength(2)
  })
})
