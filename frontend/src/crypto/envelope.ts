import { decryptAesGcm, encryptAesGcm } from './aesGcm'

export const ENVELOPE_ALGO = 'AES-GCM'
export const ENVELOPE_VERSION = 1

export type EncryptedEnvelope = {
  titleCiphertext: string
  titleIv: string
  bodyCiphertext: string
  bodyIv: string
  algo: string
  version: number
}

export type DecryptedEnvelope = {
  title: string
  body: string
}

export const encryptEnvelope = async (
  title: string,
  body: string,
  key: CryptoKey,
): Promise<EncryptedEnvelope> => {
  const titleResult = await encryptAesGcm(title, key)
  const bodyResult = await encryptAesGcm(body, key)

  return {
    titleCiphertext: titleResult.ciphertext,
    titleIv: titleResult.iv,
    bodyCiphertext: bodyResult.ciphertext,
    bodyIv: bodyResult.iv,
    algo: ENVELOPE_ALGO,
    version: ENVELOPE_VERSION,
  }
}

export const decryptEnvelope = async (
  envelope: EncryptedEnvelope,
  key: CryptoKey,
): Promise<DecryptedEnvelope> => {
  const title = await decryptAesGcm(
    envelope.titleCiphertext,
    envelope.titleIv,
    key,
  )
  const body = await decryptAesGcm(
    envelope.bodyCiphertext,
    envelope.bodyIv,
    key,
  )

  return { title, body }
}
