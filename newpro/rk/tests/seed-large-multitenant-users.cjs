const API_BASE = process.env.API_BASE || process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = {
  username: process.env.SEED_ADMIN_USERNAME || 'admin_a',
  password: process.env.SEED_ADMIN_PASSWORD || '123456',
  organizationId: process.env.SEED_ADMIN_ORG || '1'
}

const TARGET_PER_TENANT = Number(process.env.SEED_TARGET_PER_TENANT || 200)
const TOTAL_IDENTITIES = Number(process.env.SEED_TOTAL_IDENTITIES || 2000)
const MIN_MULTI_TENANT_IDENTITIES = Number(process.env.SEED_MIN_MULTI_IDENTITIES || 1800)
const USERNAME_PREFIX = process.env.SEED_USERNAME_PREFIX || 'mtu'
const PASSWORD = process.env.SEED_PASSWORD || '123456'
const RNG_SEED = Number(process.env.SEED_RANDOM_SEED || 20260415)
const USE_BASELINE_DEFICITS = process.env.SEED_USE_BASELINE_DEFICITS === '1'
const TENANT_FILTER = (process.env.SEED_TENANT_FILTER || '')
  .split(',')
  .map((item) => item.trim())
  .filter(Boolean)

const BASELINE_DEFICITS = {
  '1': 0,
  '2': 121,
  '3': 192,
  '4': 197,
  '5': 197,
  '6': 197,
  '7': 197,
  '8': 197,
  '9': 197,
  '10': 197,
  '11': 197,
  '12': 197,
  '13': 197,
  '14': 197,
  '15': 197,
  '16': 197,
  '17': 197,
  '19': 197,
  '20': 197,
  '21': 197,
  '22': 197,
  '23': 197,
  '24': 197,
  '25': 197,
  '26': 197,
  '27': 197,
  '28': 197
}

async function requestJson(url, options = {}) {
  const res = await fetch(url, options)
  const body = await res.json()
  if (!res.ok || (body.code !== undefined && body.code !== 200)) {
    throw new Error(`${url} -> ${res.status} ${JSON.stringify(body)}`)
  }
  return body
}

function mulberry32(seed) {
  let t = seed >>> 0
  return () => {
    t += 0x6d2b79f5
    let r = Math.imul(t ^ (t >>> 15), 1 | t)
    r ^= r + Math.imul(r ^ (r >>> 7), 61 | r)
    return ((r ^ (r >>> 14)) >>> 0) / 4294967296
  }
}

function pickDistinctTenants(remainingEntries, count, random) {
  const picked = []
  const available = remainingEntries.filter((item) => item.remaining > 0)
  if (available.length < count) {
    throw new Error(`Not enough tenants with remaining capacity to pick ${count}, only ${available.length}`)
  }
  while (picked.length < count) {
    const candidates = available.filter((item) => !picked.includes(item.tenantId) && item.remaining > 0)
    const totalWeight = candidates.reduce((sum, item) => sum + item.remaining, 0)
    let target = random() * totalWeight
    for (const candidate of candidates) {
      target -= candidate.remaining
      if (target <= 0) {
        picked.push(candidate.tenantId)
        break
      }
    }
  }
  return picked
}

function buildIdentity(index) {
  const id = String(index + 1).padStart(4, '0')
  return {
    username: `${USERNAME_PREFIX}_${id}`,
    name: `澶氱鎴风敤?{id}`,
    cellPhone: String(16600000000n + BigInt(index + 1)),
    email: `${USERNAME_PREFIX}_${id}@example.com`
  }
}

async function loginAs(user) {
  const body = await requestJson(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(user)
  })
  return body.data
}

async function currentTenantCount(tenantId) {
  const adminLogin = await loginAs({
    username: `t${String(tenantId).padStart(2, '0')}_admin`,
    password: PASSWORD,
    organizationId: String(tenantId)
  })
  const page = await requestJson(`${API_BASE}/users/page?pageNo=1&size=1`, {
    headers: { Authorization: `Bearer ${adminLogin.token}` }
  })
  return Number(page.data?.total || 0)
}

async function main() {
  const random = mulberry32(RNG_SEED)
  const superLogin = await loginAs(ADMIN)
  const superHeaders = {
    Authorization: `Bearer ${superLogin.token}`,
    'Content-Type': 'application/json'
  }

  const tenantBody = await requestJson(`${API_BASE}/tenants/list`)
  const tenants = (tenantBody.data || []).filter((tenant) => tenant.status === 1)

  const roleMap = new Map()
  const remainingEntries = []
  let totalDeficit = 0

  for (const tenant of tenants) {
    const count = USE_BASELINE_DEFICITS ? Math.max(0, TARGET_PER_TENANT - (BASELINE_DEFICITS[String(tenant.id)] ?? TARGET_PER_TENANT)) : await currentTenantCount(tenant.id)
    const deficit = USE_BASELINE_DEFICITS
      ? Math.max(0, BASELINE_DEFICITS[String(tenant.id)] ?? 0)
      : Math.max(0, TARGET_PER_TENANT - count)
    if (deficit > 0) {
      const rolesBody = await requestJson(`${API_BASE}/roles/list?tenantId=${tenant.id}`, { headers: superHeaders })
      const userRole = (rolesBody.data || []).find((role) => role.code === 'USER')
      if (!userRole) {
        throw new Error(`USER role missing for tenant ${tenant.id}`)
      }
      roleMap.set(String(tenant.id), Number(userRole.id))
      remainingEntries.push({
        tenantId: String(tenant.id),
        tenantName: tenant.tenantName,
        remaining: deficit
      })
      totalDeficit += deficit
    }
  }

  const singleTenantUsers = TOTAL_IDENTITIES - MIN_MULTI_TENANT_IDENTITIES
  const threeTenantUsers = totalDeficit - (singleTenantUsers + MIN_MULTI_TENANT_IDENTITIES * 2)
  const twoTenantUsers = MIN_MULTI_TENANT_IDENTITIES - threeTenantUsers

  if (threeTenantUsers < 0 || twoTenantUsers < 0) {
    throw new Error(`Cannot satisfy requested distribution with totalDeficit=${totalDeficit}`)
  }

  const membershipCounts = [
    ...Array(threeTenantUsers).fill(3),
    ...Array(twoTenantUsers).fill(2),
    ...Array(singleTenantUsers).fill(1)
  ]

  if (membershipCounts.length !== TOTAL_IDENTITIES) {
    throw new Error(`Unexpected identity count ${membershipCounts.length}, expected ${TOTAL_IDENTITIES}`)
  }

  const rowsByTenant = new Map()
  const identitySummary = []

  membershipCounts.forEach((membershipCount, index) => {
    const identity = buildIdentity(index)
    const tenantIds = pickDistinctTenants(remainingEntries, membershipCount, random)
    tenantIds.forEach((tenantId) => {
      const entry = remainingEntries.find((item) => item.tenantId === tenantId)
      entry.remaining -= 1
      const rows = rowsByTenant.get(tenantId) || []
      rows.push({
        tenantId: Number(tenantId),
        username: identity.username,
        password: PASSWORD,
        name: identity.name,
        cellPhone: identity.cellPhone,
        email: identity.email,
        type: 2,
        roleId: roleMap.get(tenantId),
        status: 1
      })
      rowsByTenant.set(tenantId, rows)
    })
    identitySummary.push({
      username: identity.username,
      tenantCount: membershipCount,
      tenantIds
    })
  })

  const unresolved = remainingEntries.filter((item) => item.remaining !== 0)
  if (unresolved.length > 0) {
    throw new Error(`Unresolved tenant deficits: ${JSON.stringify(unresolved)}`)
  }

  for (const [tenantId, rows] of rowsByTenant.entries()) {
    if (TENANT_FILTER.length > 0 && !TENANT_FILTER.includes(String(tenantId))) {
      continue
    }
    await requestJson(`${API_BASE}/users/import`, {
      method: 'POST',
      headers: {
        ...superHeaders,
        'X-Tenant-Id': tenantId
      },
      body: JSON.stringify(rows)
    })
  }

  const finalCounts = []
  for (const tenant of tenants) {
    finalCounts.push({
      tenantId: tenant.id,
      tenantName: tenant.tenantName,
      total: await currentTenantCount(tenant.id)
    })
  }

  const summary = {
    targetPerTenant: TARGET_PER_TENANT,
    totalIdentitiesCreated: TOTAL_IDENTITIES,
    singleTenantUsers,
    twoTenantUsers,
    threeTenantUsers,
    totalMembershipsCreated: membershipCounts.reduce((sum, item) => sum + item, 0),
    tenantFilter: TENANT_FILTER,
    useBaselineDeficits: USE_BASELINE_DEFICITS,
    finalCountsSample: finalCounts.slice(0, 10),
    sampleMultiTenantUsers: identitySummary.filter((item) => item.tenantCount >= 2).slice(0, 10)
  }

  console.log(JSON.stringify(summary, null, 2))
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
