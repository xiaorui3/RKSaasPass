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

function authHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': String(loginData.organizationId)
  }
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

test('super admin task center should load Jenkins overview for tenant 1', async ({ page, request }) => {
  test.slow()
  const login = await loginApi(request, SUPER_ADMIN)

  const apiRes = await request.get(`${API_BASE}/admin/ops/jenkins/overview`, {
    headers: authHeaders(login)
  })
  const apiBody = await apiRes.json()
  expect(apiBody.code).toBe(200)
  expect(Array.isArray(apiBody.data?.jobs)).toBeTruthy()

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/operation/tasks`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.task-center')).toBeVisible()
  await expect(page.getByText(/Jenkins/i).first()).toBeVisible()
  await expect(page.locator('.task-section').nth(1)).toBeVisible()
})

test('super admin api workbench should load resources and docs', async ({ page, request }) => {
  test.slow()
  const login = await loginApi(request, SUPER_ADMIN)

  const resourcesRes = await request.get(`${API_BASE}/admin/ops/api-workbench/resources`, {
    headers: authHeaders(login)
  })
  const resourcesBody = await resourcesRes.json()
  expect(resourcesBody.code).toBe(200)
  expect(Array.isArray(resourcesBody.data)).toBeTruthy()
  expect(resourcesBody.data.length).toBeGreaterThan(0)

  const firstResourceUrl = resourcesBody.data[0]?.url
  expect(typeof firstResourceUrl).toBe('string')
  expect(firstResourceUrl.length).toBeGreaterThan(0)

  const docsRes = await request.get(`${API_BASE}/admin/ops/api-workbench/docs`, {
    headers: authHeaders(login),
    params: { url: firstResourceUrl }
  })
  const docsBody = await docsRes.json()
  expect(docsBody.code).toBe(200)
  expect(Array.isArray(docsBody.data?.endpoints)).toBeTruthy()
  expect(docsBody.data.endpoints.length).toBeGreaterThan(0)

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/admin/operation/api-workbench`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.api-workbench-page')).toBeVisible()
  await expect(page.locator('.resource-item').first()).toBeVisible()
})

test('language switch should persist into login page', async ({ page }) => {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => localStorage.clear())
  await page.reload({ waitUntil: 'domcontentloaded' })

  await expect(page.locator('.lang-switch')).toBeVisible()
  await page.locator('.lang-switch .lang-link', { hasText: 'EN' }).click()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.login-card')).toBeVisible()
  await expect(page.locator('html')).toHaveAttribute('lang', 'en')
})
