import { resolveMediaUrl } from './mediaUrl.js'
import { repairLegacyMojibake } from './exportEncoding.js'

const CJK_MOJIBAKE_MARKERS = new RegExp([
  [0x7ec0, 0x60e7],
  [0x6942, 0x6a3b],
  [0x8930, 0x64b3],
  [0x9352, 0x56e8],
  [0x93bc, 0x6ec5],
  [0x6d93, 0xe15f],
  [0x68e3, 0x682d],
  [0x935a, 0x5c7d],
  [0x93c8, 0xe048],
  [0x7035, 0x8270],
  [0x7f02, 0x64b3],
  [0x701b, 0x6a3a],
  [0x93ba, 0x0443],
  [0x95bf],
  [0xfffd]
].map((codes) => String.fromCodePoint(...codes)).join('|'))

export function cleanDisplayText(value, fallback = '') {
  const repaired = repairLegacyMojibake(String(value || '').trim())
  if (!repaired || CJK_MOJIBAKE_MARKERS.test(repaired)) {
    return fallback
  }
  return repaired
}


function normalizeDisplayOrder(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

export function sortTenantDirectory(items) {
  return [...(Array.isArray(items) ? items : [])].sort((left, right) => {
    const leftOrder = normalizeDisplayOrder(left?.displayOrder)
    const rightOrder = normalizeDisplayOrder(right?.displayOrder)
    if (leftOrder !== rightOrder) {
      return leftOrder - rightOrder
    }
    return Number(left?.id || 0) - Number(right?.id || 0)
  })
}

export function normalizeTenantDirectory(items) {
  return sortTenantDirectory(items).map((item) => ({
    ...item,
    logoUrl: item?.logoUrl || '',
    resolvedLogoUrl: item?.logoUrl ? resolveMediaUrl(item.logoUrl) : ''
  }))
}

function buildInitials(name) {
  const trimmed = String(name || '').trim()
  if (!trimmed) {
    return '社团'
  }
  if (/[\u4e00-\u9fa5]/.test(trimmed)) {
    return trimmed.slice(0, 2)
  }
  return trimmed.slice(0, 2).toUpperCase()
}

export function resolveCurrentTenantBranding({ tenantId, tenantDirectory }) {
  const currentTenant = Array.isArray(tenantDirectory)
    ? tenantDirectory.find((item) => String(item?.id) === String(tenantId))
    : null
  const tenantName = cleanDisplayText(currentTenant?.tenantName, '')
  const logoUrl = String(currentTenant?.logoUrl || '').trim()

  return {
    tenantName,
    logoUrl: String(currentTenant?.resolvedLogoUrl || logoUrl).trim(),
    initials: buildInitials(tenantName)
  }
}
