// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

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

test('news management page should not auto-trigger top-status updates on initial load', async ({ page, request }) => {
  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  const updateCalls = []
  page.on('request', (req) => {
    if (req.url().includes('/api/news/update') && req.method() === 'PUT') {
      updateCalls.push(req.url())
    }
  })

  await page.goto(`${BASE_URL}/admin/content/news`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鏂伴椈绠＄悊/).first()).toBeVisible()
  await page.waitForTimeout(1000)

  expect(updateCalls).toHaveLength(0)
  await expect(page.locator('.el-message')).toHaveCount(0)
})
