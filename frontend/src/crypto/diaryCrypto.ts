import type { E2eeParams } from '../api/types'
import { decryptAesGcm, encryptAesGcm } from './aesGcm'
import { decryptEnvelope, encryptEnvelope, ENVELOPE_ALGO, ENVELOPE_VERSION } from './envelope'
import { deriveAesKey } from './kdf'

export type EncryptedField = {
  version: number
  algo: string
  iv: string
  ciphertext: string
}

export const deriveKey = async (
  passphrase: string,
  params: E2eeParams,
): Promise<CryptoKey> => {
  if (params.kdf !== 'PBKDF2-SHA256') {
    throw new Error(`Unsupported KDF: ${params.kdf}`)
  }

  return deriveAesKey(passphrase, params.salt, params.iterations)
}

export const encrypt = async (
  plaintext: string,
  key: CryptoKey,
): Promise<EncryptedField> => {
  const encrypted = await encryptAesGcm(plaintext, key)
  return {
    version: ENVELOPE_VERSION,
    algo: ENVELOPE_ALGO,
    iv: encrypted.iv,
    ciphertext: encrypted.ciphertext,
  }
}

export const decrypt = async (
  envelope: EncryptedField,
  key: CryptoKey,
): Promise<string> => {
  if (envelope.algo !== ENVELOPE_ALGO) {
    throw new Error(`Unsupported algorithm: ${envelope.algo}`)
  }
  if (envelope.version !== ENVELOPE_VERSION) {
    throw new Error(`Unsupported envelope version: ${envelope.version}`)
  }

  return decryptAesGcm(envelope.ciphertext, envelope.iv, key)
}

export const encryptEntry = encryptEnvelope
export const decryptEntry = decryptEnvelope
