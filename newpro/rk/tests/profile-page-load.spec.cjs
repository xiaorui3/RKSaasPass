// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }

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

test('member profile page should resolve real user info instead of staying on loading text', async ({ page, request }) => {
  test.setTimeout(90000)
  const login = await loginApi(request, MEMBER)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.profile-page')).toBeVisible()
  await expect(page.locator('.user-name')).not.toHaveText(/鍔犺浇涓?u, { timeout: 30000 })
  await expect.poll(async () => page.title(), { timeout: 30000 }).not.toBe('璇风櫥褰?)
})
