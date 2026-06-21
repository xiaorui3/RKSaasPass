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

test('super admin config page should call real config api', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  const configResponse = page.waitForResponse((response) =>
    response.url().includes('/admin/config') &&
    response.request().method() === 'GET' &&
    response.status() === 200
  )

  await page.goto(`${BASE_URL}/admin/system/config`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/绯荤粺閰嶇疆/).first()).toBeVisible()
  await configResponse
})

test('super admin logs page should call real logs api', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  const logsResponse = page.waitForResponse((response) =>
    (response.url().includes('/admin/logs/operlog/list') || response.url().includes('/admin/logs/logininfor/list')) &&
    response.request().method() === 'POST' &&
    response.status() === 200
  )

  await page.goto(`${BASE_URL}/admin/operation/logs`, { waitUntil: 'networkidle' })
  await expect(page.getByText(/鏃ュ織绠＄悊/).first()).toBeVisible()
  await logsResponse
})
