// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

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

test('super admin tasks page should call real tasks api and render job rows', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  const seen = []

  page.on('response', async (response) => {
    if (response.url().includes('/admin/ops/tasks/overview')) {
      seen.push({ url: response.url(), status: response.status() })
    }
  })

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/operation/tasks`, { waitUntil: 'networkidle' })

  await expect(page.getByText(/瀹氭椂浠诲姟/).first()).toBeVisible()
  await expect(page.getByRole('button', { name: /鍒锋柊/ }).first()).toBeVisible()
  await expect(page.locator('table tbody tr').first()).toBeVisible()
  expect(seen.some((item) => item.status === 200)).toBeTruthy()
})
