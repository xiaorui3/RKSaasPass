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

test('credit and volunteer admin routes should render instead of 404', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/activity/credit`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/瀛﹀垎绠＄悊/).first()).toBeVisible()
  await expect(page.locator('body')).not.toContainText('404')

  await page.goto(`${BASE_URL}/admin/activity/volunteer`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/蹇楁効鏈嶅姟瀹℃牳/).first()).toBeVisible()
  await expect(page.locator('body')).not.toContainText('404')
})
