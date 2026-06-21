// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }

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

test('tenant theme config page should render and save current tenant theme presets', async ({ page, request }) => {
  const login = await loginApi(request, ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/system/theme`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/涓婚閰嶇疆/).first()).toBeVisible()
  await expect(page.getByText(/鍓嶅彴涓婚/).first()).toBeVisible()
  await expect(page.getByText(/鍚庡彴涓婚/).first()).toBeVisible()

  await page.locator('.theme-panel').nth(0).locator('.theme-card').filter({ hasText: /鏀垮姟/ }).first().click()
  await page.locator('.theme-panel').nth(1).locator('.theme-card').filter({ hasText: /绉戞妧娣辫壊/ }).first().click()

  const saveResponse = page.waitForResponse((response) =>
    response.url().includes('/api/config/theme/current') &&
    response.request().method() === 'PUT' &&
    response.status() === 200
  )
  await page.getByRole('button', { name: /淇濆瓨涓婚/ }).click()
  await saveResponse
})
