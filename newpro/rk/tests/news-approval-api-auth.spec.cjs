// @ts-check
const { test, expect } = require('@playwright/test')
const { TENANT1_LIVE_TEACHER } = require('./helpers/test-users.cjs')
const { loginApi } = require('./helpers/live-login.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

test('teacher should fetch pending news through gateway without 500', async ({ request }) => {
  const login = await loginApi(request, TENANT1_LIVE_TEACHER, API_BASE)
  const res = await request.get(`${API_BASE}/api/news/pending`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': login.organizationId
    }
  })

  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body.code).toBe(200)
})
