import { base64ToBytes, bytesToBase64 } from './base64'

const textEncoder = new TextEncoder()
const textDecoder = new TextDecoder()

export type AesGcmCiphertext = {
  ciphertext: string
  iv: string
}

const toArrayBuffer = (bytes: Uint8Array): ArrayBuffer => {
  const buffer = new ArrayBuffer(bytes.byteLength)
  new Uint8Array(buffer).set(bytes)
  return buffer
}

export const encryptAesGcm = async (
  plaintext: string,
  key: CryptoKey,
  ivBytes?: Uint8Array<ArrayBuffer>,
): Promise<AesGcmCiphertext> => {
  const iv = ivBytes ?? crypto.getRandomValues(new Uint8Array(new ArrayBuffer(12)))
  const encrypted = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    toArrayBuffer(textEncoder.encode(plaintext)),
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
  const ivBytes = base64ToBytes(ivBase64)
  const ciphertextBytes = base64ToBytes(ciphertextBase64)
  const decrypted = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: ivBytes },
    key,
    ciphertextBytes,
  )
  return textDecoder.decode(new Uint8Array(decrypted))
}
