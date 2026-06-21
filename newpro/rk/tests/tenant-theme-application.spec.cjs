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

test('frontend and admin theme presets should apply separately for the same tenant', async ({ page, request }) => {
  const login = await loginApi(request, ADMIN)
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId || '1'
  }

  const saveRes = await request.put(`${API_BASE}/api/config/theme/current`, {
    headers,
    data: {
      frontendTheme: 'gov_blue',
      adminTheme: 'tech_dark'
    }
  })
  expect(saveRes.ok()).toBeTruthy()

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' })
  const loginTheme = await page.evaluate(() => ({
    preset: document.documentElement.dataset.themePreset,
    primary: getComputedStyle(document.documentElement).getPropertyValue('--rk-primary').trim()
  }))
  expect(loginTheme.preset).toBe('gov_blue')
  expect(loginTheme.primary).toBe('#1d5fa7')

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/system/theme`, { waitUntil: 'networkidle' })
  const adminTheme = await page.evaluate(() => ({
    preset: document.documentElement.dataset.themePreset,
    primary: getComputedStyle(document.documentElement).getPropertyValue('--rk-primary').trim()
  }))
  expect(adminTheme.preset).toBe('tech_dark')
  expect(adminTheme.primary).toBe('#1de9b6')
})
