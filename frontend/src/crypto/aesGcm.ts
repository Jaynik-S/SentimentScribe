import { base64ToBytes, bytesToBase64 } from './base64'

const textEncoder = new TextEncoder()
const textDecoder = new TextDecoder()

export type AesGcmCiphertext = {
  ciphertext: string
  iv: string
}

export const encryptAesGcm = async (
  plaintext: string,
  key: CryptoKey,
  ivBytes?: Uint8Array,
): Promise<AesGcmCiphertext> => {
  const iv = ivBytes ?? crypto.getRandomValues(new Uint8Array(12))
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    textEncoder.encode(plaintext),
  )
  return {
    ciphertext: bytesToBase64(new Uint8Array(encrypted)),
    iv: bytesToBase64(iv),
  }
}

export const decryptAesGcm = async (
  ciphertextBase64: string,
  ivBase64: string,
  key: CryptoKey,
): Promise<string> => {
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: base64ToBytes(ivBase64) },
    key,
    base64ToBytes(ciphertextBase64),
  )
  return textDecoder.decode(new Uint8Array(decrypted))
}
