// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN, TENANT1_LIVE_TEACHER } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

test.describe.configure({ timeout: 90000 })

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function applyLogin(page, loginData) {
  await page.addInitScript((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
  }, loginData)
}

test('super admin members page should expose cross-tenant member rows for tenant-1 global view', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  let membersPayload = null
  page.on('response', async (response) => {
    if (response.url().includes('/api/members/list') && response.request().method() === 'GET') {
      membersPayload = await response.json()
    }
  })

  await page.goto(`${BASE_URL}/admin/club/members`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鎴愬憳绠＄悊/).first()).toBeVisible()

  expect(membersPayload).not.toBeNull()
  const tenantIds = [...new Set((membersPayload.data || []).map((item) => String(item.tenantId)))]
  expect(tenantIds).toContain('1')
  expect(tenantIds.length).toBeGreaterThan(1)
})

test('teacher news approval page should load pending news without server error', async ({ page, request }) => {
  const login = await loginApi(request, TENANT1_LIVE_TEACHER)
  await applyLogin(page, login)

  const pendingResponse = page.waitForResponse((response) =>
    response.url().includes('/api/news/pending') &&
    response.request().method() === 'GET'
  )

  await page.goto(`${BASE_URL}/admin/content/news-approval`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鏂伴椈瀹℃牳/).first()).toBeVisible()

  const response = await pendingResponse
  expect(response.status()).toBe(200)
})
