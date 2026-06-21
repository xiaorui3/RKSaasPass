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

test('manager finance page should call real finance apis and render account cards', async ({ page, request }) => {
  const login = await loginApi(request, MANAGER)
  const seen = []

  page.on('response', async (response) => {
    const url = response.url()
    if (url.includes('/api/finance/account') || url.includes('/api/finance/records')) {
      seen.push({ url, status: response.status() })
    }
  })

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/club/finance`, { waitUntil: 'networkidle' })

  await expect(page.locator('.finance-page')).toBeVisible()
  await expect(page.getByText(/褰撳墠浣欓|鎬绘敹鍏鎬绘敮鍑?).first()).toBeVisible()
  await expect(page.getByRole('button', { name: /鏂板鏀跺叆|鏂板鏀嚭/ }).first()).toBeVisible()
  expect(seen.some((item) => item.url.includes('/api/finance/account') && item.status === 200)).toBeTruthy()
  expect(seen.some((item) => item.url.includes('/api/finance/records') && item.status === 200)).toBeTruthy()
})
