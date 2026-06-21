// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN, TENANT2_ADMIN } = require('./helpers/test-users.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function buildHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': loginData.organizationId || '1'
  }
}

test('legacy /admin/tenants/page alias should stay available for super admin and tenant admin', async ({ request }) => {
  const cases = [
    { user: SUPER_ADMIN },
    { user: TENANT2_ADMIN }
  ]

  for (const item of cases) {
    const login = await loginApi(request, item.user)
    const headers = buildHeaders(login)
    const expectedRes = await request.get(`${API_BASE}/admin/tenants?pageNum=1&pageSize=5`, {
      headers
    })
    expect(expectedRes.status()).toBe(200)
    const expectedBody = await expectedRes.json()
    expect(expectedBody.code).toBe(200)

    const res = await request.get(`${API_BASE}/admin/tenants/page?pageNum=1&pageSize=5`, {
      headers
    })

    expect(res.status()).toBe(200)
    const body = await res.json()
    expect(body.code).toBe(200)
    expect(body.data?.total).toBe(expectedBody.data?.total)
    const tenantIds = [...new Set((body.data?.records || []).map((row) => String(row.id)))]
    const expectedTenantIds = [...new Set((expectedBody.data?.records || []).map((row) => String(row.id)))]
    expect(tenantIds).toEqual(expectedTenantIds)
  }
})
