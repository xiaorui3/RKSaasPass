// @ts-check
const { test, expect } = require('@playwright/test')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const SUPER_ADMIN = {
  username: 'admin_a',
  password: process.env.PLAYWRIGHT_ADMIN_PASSWORD || 'change-me',
  organizationId: '1'
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

test('super admin admission list should stay within current tenant context', async ({ request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const response = await request.get(`${API_BASE}/api/admission/list`, { headers })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()

  const tenantIds = [...new Set((body.data || []).map((item) => String(item.tenantId)))].sort()
  expect(tenantIds.length).toBeGreaterThan(0)
  expect(tenantIds).toContain('1')
  expect(tenantIds).not.toContain('undefined')
  expect(tenantIds).not.toContain('null')
})
