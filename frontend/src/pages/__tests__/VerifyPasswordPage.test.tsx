import { describe, expect, it, vi } from 'vitest'
import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { VerifyPasswordPage } from '../VerifyPasswordPage'
import { renderWithRouter } from '../../test/renderWithRouter'
import { verifyPassword } from '../../api/auth'
import { ApiError } from '../../api/http'

vi.mock('../../api/auth', () => ({
  verifyPassword: vi.fn(),
}))

describe('VerifyPasswordPage', () => {
  it('submits password and navigates to home', async () => {
    const verifyPasswordMock = vi.mocked(verifyPassword)
    verifyPasswordMock.mockResolvedValue({ status: 'Correct Password', entries: [] })

    const user = userEvent.setup()
    renderWithRouter({
      routes: [
        { path: '/', element: <VerifyPasswordPage /> },
        { path: '/home', element: <div>Home</div> },
      ],
    })

    await user.type(screen.getByLabelText(/password/i), 'secret')
    await user.click(screen.getByRole('button', { name: /submit/i }))

    expect(verifyPasswordMock).toHaveBeenCalledWith({ password: 'secret' })
    expect(await screen.findByText('Home')).toBeInTheDocument()
  })

  it('shows backend error when password is incorrect', async () => {
    const verifyPasswordMock = vi.mocked(verifyPassword)
    verifyPasswordMock.mockRejectedValue(new ApiError(400, 'Incorrect Password'))

    const user = userEvent.setup()
    renderWithRouter({
      routes: [{ path: '/', element: <VerifyPasswordPage /> }],
    })

    await user.type(screen.getByLabelText(/password/i), 'bad')
    await user.click(screen.getByRole('button', { name: /submit/i }))

    expect(
      await screen.findAllByText('Incorrect Password'),
    ).toHaveLength(2)
  })
})
