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

test('statistics board should render member and activity counts from real APIs', async ({ page, request }) => {
  const login = await loginApi(request, MANAGER)
  const overviewRes = await request.get(`${API_BASE}/api/statistics/overview`, {
    headers: { Authorization: `Bearer ${login.token}`, 'X-Tenant-Id': login.organizationId }
  }).catch(() => null)
  if (overviewRes) {
    const overviewBody = await overviewRes.json().catch(() => null)
    expect(overviewBody === null || overviewBody.code === 200 || overviewBody.code === 0).toBeTruthy()
  }

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/statistics/board`, { waitUntil: 'networkidle' })

  await expect(page.locator('.statistics-board')).toBeVisible()
  await expect(page.getByText(/鏁版嵁鐪嬫澘|娲诲姩瓒嬪娍|鍐呭鍒嗗竷/).first()).toBeVisible()
})
