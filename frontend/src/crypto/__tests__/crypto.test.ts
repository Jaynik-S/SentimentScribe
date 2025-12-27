import { describe, expect, it } from 'vitest'
import { base64ToBytes, bytesToBase64 } from '../base64'
import {
  decrypt,
  decryptEntry,
  deriveKey,
  encrypt,
  encryptEntry,
} from '../diaryCrypto'

const PASS_PHRASE = 'correct horse battery staple'
const SALT_BASE64 = 'AAAAAAAAAAAAAAAAAAAAAA=='
const ITERATIONS = 1000
const PARAMS = { kdf: 'PBKDF2-SHA256', salt: SALT_BASE64, iterations: ITERATIONS }

describe('crypto utilities', () => {
  it('round-trips base64 encoding', () => {
    const bytes = new Uint8Array([1, 2, 3, 4, 5, 255])
    const encoded = bytesToBase64(bytes)
    const decoded = base64ToBytes(encoded)
    expect(Array.from(decoded)).toEqual(Array.from(bytes))
  })

  it('derives a usable AES-GCM key', async () => {
    const key = await deriveKey(PASS_PHRASE, PARAMS)
    expect(key.type).toBe('secret')
    expect(key.algorithm.name).toBe('AES-GCM')
  })

  it('encrypts and decrypts a single field envelope', async () => {
    const key = await deriveKey(PASS_PHRASE, PARAMS)
    const plaintext = 'Hello from SentimentScribe'
    const encrypted = await encrypt(plaintext, key)
    const decrypted = await decrypt(encrypted, key)
    expect(encrypted.algo).toBe('AES-GCM')
    expect(encrypted.version).toBe(1)
    expect(base64ToBytes(encrypted.iv)).toHaveLength(12)
    expect(decrypted).toBe(plaintext)
  })

  it('round-trips encrypted envelopes', async () => {
    const key = await deriveKey(PASS_PHRASE, PARAMS)
    const title = 'Encrypted title'
    const body = 'Encrypted body'
    const envelope = await encryptEntry(title, body, key)
    const decrypted = await decryptEntry(envelope, key)
    expect(decrypted).toEqual({ title, body })
  })
})
