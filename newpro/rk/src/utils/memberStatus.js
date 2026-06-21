function normalizeText(value) {
  if (typeof value !== 'string') {
    return ''
  }
  return value.trim().toLowerCase()
}

const ACTIVE_MEMBER_STATUSES = new Set([
  '正常',
  '活跃',
  'active',
  // Keep reading historical mojibake values until live data is fully cleaned.
  '姝ｅ父',
  '娲昏穬'
].map((item) => item.toLowerCase()))

export function isActiveMemberStatus(value) {
  if (value === 1 || value === true) {
    return true
  }

  const normalized = normalizeText(value)
  if (!normalized) {
    return true
  }
  return ACTIVE_MEMBER_STATUSES.has(normalized)
}

export function normalizeMemberStatusValue(value) {
  return isActiveMemberStatus(value) ? 1 : 0
}
