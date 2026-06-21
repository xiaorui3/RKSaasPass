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

async function assertRouteWorks(page, path, expectedText) {
  const consoleErrors = []
  page.on('console', (msg) => {
    if (msg.type() === 'error') {
      consoleErrors.push(msg.text())
    }
  })

  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 })
  await expect(page.getByText(expectedText).first()).toBeVisible({ timeout: 15000 })
  await expect(page.locator('text=404')).toHaveCount(0)
  expect(consoleErrors).toEqual([])
}

test.describe('admin route presence smoke', () => {
  test.beforeEach(async ({ page, request }) => {
    const login = await loginApi(request, SUPER_ADMIN)
    await applyLogin(page, login)
  })

  test('finance page should load', async ({ page }) => {
    await assertRouteWorks(page, '/admin/club/finance', '璐㈠姟绠＄悊')
  })

  test('credit page should load', async ({ page }) => {
    await assertRouteWorks(page, '/admin/activity/credit', '瀛﹀垎绠＄悊')
  })

  test('volunteer verification page should load', async ({ page }) => {
    await assertRouteWorks(page, '/admin/activity/volunteer', '蹇楁効鏈嶅姟瀹℃牳')
  })

  test('achievements page should load', async ({ page }) => {
    await assertRouteWorks(page, '/admin/content/achievements', '鎴愬氨绠＄悊')
  })
})
