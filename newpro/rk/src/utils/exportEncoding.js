const LATIN1_MOJIBAKE_HINT = /[\u0080-\u009f\u00c0-\u00ff]/
const CJK_TEXT = /[\u3400-\u9fff]/
const REPLACEMENT_TEXT = /\ufffd/
const COMMON_LATIN1_MOJIBAKE_CODES = new Set([0x00c3, 0x00c2, 0x00e2, 0x00e4, 0x00e5, 0x00e6, 0x00e7, 0x00e8, 0x00e9])

function mojibakeScore(value) {
  const text = String(value || '')
  let score = 0
  for (const char of text) {
    const code = char.charCodeAt(0)
    if ((code >= 0x80 && code <= 0x9f) || (code >= 0xc0 && code <= 0xff)) {
      score += 2
    }
    if (COMMON_LATIN1_MOJIBAKE_CODES.has(code)) {
      score += 1
    }
  }
  score += (text.match(REPLACEMENT_TEXT) || []).length * 4
  return score
}

function decodeLatin1Utf8(value) {
  const bytes = []
  for (const char of value) {
    const code = char.charCodeAt(0)
    if (code > 0xff) {
      return value
    }
    bytes.push(code)
  }
  return new TextDecoder('utf-8', { fatal: false }).decode(Uint8Array.from(bytes))
}

export function repairLegacyMojibake(value) {
  if (typeof value !== 'string' || !LATIN1_MOJIBAKE_HINT.test(value)) {
    return value
  }

  const repaired = decodeLatin1Utf8(value)
  if (!CJK_TEXT.test(repaired)) {
    return value
  }
  if (mojibakeScore(repaired) >= mojibakeScore(value)) {
    return value
  }
  return repaired
}

export function normalizeExportRow(row) {
  return Object.entries(row || {}).reduce((result, [key, value]) => {
    result[key] = typeof value === 'string' ? repairLegacyMojibake(value) : value
    return result
  }, {})
}
