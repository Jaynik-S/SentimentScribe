import { useEffect } from 'react'
import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { AuthProvider } from '../auth'
import { E2eeProvider, useE2ee } from '../e2ee'
import type { E2eeParams } from '../../api/types'

type HarnessProps = {
  passphrase: string
  params: E2eeParams
}

const Harness = ({ passphrase, params }: HarnessProps) => {
  const { isUnlocked, unlock } = useE2ee()

  useEffect(() => {
    void unlock(passphrase, params)
  }, [params, passphrase, unlock])

  return <div>{isUnlocked ? 'Unlocked' : 'Locked'}</div>
}

describe('E2eeProvider', () => {
  it('unlocks with a passphrase and params', async () => {
    const params: E2eeParams = {
      kdf: 'PBKDF2-SHA256',
      salt: 'c2FsdA==',
      iterations: 1,
    }

    sessionStorage.setItem('sentimentscribe.auth', JSON.stringify({
      accessToken: 'token',
      user: { id: 'user-id', username: 'demo' },
      e2eeParams: params,
    }))

    render(
      <AuthProvider>
        <E2eeProvider>
          <Harness passphrase="correct horse" params={params} />
        </E2eeProvider>
      </AuthProvider>,
    )

    expect(await screen.findByText('Unlocked')).toBeInTheDocument()
  })
})
