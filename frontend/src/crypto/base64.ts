const hasBuffer = typeof Buffer !== 'undefined'

export const bytesToBase64 = (bytes: Uint8Array): string => {
  if (typeof btoa === 'function') {
    let binary = ''
    bytes.forEach((value) => {
      binary += String.fromCharCode(value)
    })
    return btoa(binary)
  }

  if (hasBuffer) {
    return Buffer.from(bytes).toString('base64')
  }

  throw new Error('Base64 encoding is not supported in this environment.')
}

export const base64ToBytes = (base64: string): Uint8Array<ArrayBuffer> => {
  if (typeof atob === 'function') {
    const binary = atob(base64)
    const bytes = new Uint8Array(new ArrayBuffer(binary.length))
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i)
    }
    return bytes
  }

  if (hasBuffer) {
    const buf = Buffer.from(base64, 'base64')
    return new Uint8Array(buf.buffer.slice(buf.byteOffset, buf.byteOffset + buf.byteLength))
  }

  throw new Error('Base64 decoding is not supported in this environment.')
}
