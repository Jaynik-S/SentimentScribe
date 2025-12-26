import { describe, expect, it } from 'vitest'
import { base64ToBytes, bytesToBase64 } from '../base64'
import { decryptAesGcm, encryptAesGcm } from '../aesGcm'
import { deriveAesKey } from '../kdf'
import { decryptEnvelope, encryptEnvelope } from '../envelope'

const PASS_PHRASE = 'correct horse battery staple'
const SALT_BASE64 = 'AAAAAAAAAAAAAAAAAAAAAA=='
const ITERATIONS = 1000

describe('crypto utilities', () => {
  it('round-trips base64 encoding', () => {
    const bytes = new Uint8Array([1, 2, 3, 4, 5, 255])
    const encoded = bytesToBase64(bytes)
    const decoded = base64ToBytes(encoded)
    expect(Array.from(decoded)).toEqual(Array.from(bytes))
  })

  it('derives a usable AES-GCM key', async () => {
    const key = await deriveAesKey(PASS_PHRASE, SALT_BASE64, ITERATIONS)
    expect(key.type).toBe('secret')
    expect(key.algorithm.name).toBe('AES-GCM')
  })

  it('encrypts and decrypts with AES-GCM', async () => {
    const key = await deriveAesKey(PASS_PHRASE, SALT_BASE64, ITERATIONS)
    const iv = base64ToBytes('AAAAAAAAAAAAAAAAAAAAAA==')
    const plaintext = 'Hello from SentimentScribe'
    const encrypted = await encryptAesGcm(plaintext, key, iv)
    const decrypted = await decryptAesGcm(encrypted.ciphertext, encrypted.iv, key)
    expect(decrypted).toBe(plaintext)
  })

  it('round-trips encrypted envelopes', async () => {
    const key = await deriveAesKey(PASS_PHRASE, SALT_BASE64, ITERATIONS)
    const title = 'Encrypted title'
    const body = 'Encrypted body'
    const envelope = await encryptEnvelope(title, body, key)
    const decrypted = await decryptEnvelope(envelope, key)
    expect(decrypted).toEqual({ title, body })
  })
})
