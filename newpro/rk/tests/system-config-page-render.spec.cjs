// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const SUPER_ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }

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

test('system config page should render real config key and value columns', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/system/config`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/绯荤粺閰嶇疆/).first()).toBeVisible()
  await expect(page.getByText('admission.form.config')).toBeVisible()
  await expect(page.getByText(/鍚敤|鍋滅敤/).first()).toBeVisible()
})
