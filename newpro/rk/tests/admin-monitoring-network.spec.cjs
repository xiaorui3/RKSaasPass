// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

test('monitoring overview should expose a non-placeholder network metric', async ({ request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const response = await request.get(`${API_BASE}/admin/ops/monitoring/overview`, { headers })
  expect(response.ok()).toBeTruthy()
  const body = await response.json()
  expect(body.code).toBe(200)

  const network = body.data?.systemStatus?.network
  expect(String(network)).not.toBe('0')
})
