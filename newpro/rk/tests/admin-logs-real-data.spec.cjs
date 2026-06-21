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

function buildHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': loginData.organizationId || '1'
  }
}

test('admin logs should contain real login and operation records', async ({ request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const headers = buildHeaders(login)

  const configRes = await request.get(`${API_BASE}/admin/config/list`, { headers })
  expect(configRes.ok()).toBeTruthy()

  const loginLogRes = await request.post(`${API_BASE}/admin/logs/logininfor/list?page=1&size=20`, { headers })
  expect(loginLogRes.ok()).toBeTruthy()
  const loginLogBody = await loginLogRes.json()

  expect(loginLogBody.data?.total || 0).toBeGreaterThan(0)
  expect(
    (loginLogBody.data?.records || []).some((item) =>
      item.loginName === SUPER_ADMIN.username &&
      String(item.msg || '').includes('鐧诲綍鎴愬姛')
    )
  ).toBeTruthy()

  const operLogRes = await request.post(`${API_BASE}/admin/logs/operlog/list?page=1&size=50`, { headers })
  expect(operLogRes.ok()).toBeTruthy()
  const operLogBody = await operLogRes.json()

  expect(operLogBody.data?.total || 0).toBeGreaterThan(0)
  expect(
    (operLogBody.data?.records || []).some((item) =>
      String(item.operUrl || '').includes('/admin/config/list')
    )
  ).toBeTruthy()
})
