import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { webcrypto } from 'crypto'
import { afterEach } from 'vitest'

if (!globalThis.crypto) {
  globalThis.crypto = webcrypto as Crypto
}

afterEach(() => {
  cleanup()
  sessionStorage.clear()
})
