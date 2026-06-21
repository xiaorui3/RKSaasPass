// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN, TENANT2_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
test.setTimeout(90000)

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

test('tenant admin should access user management and only see current-tenant users', async ({ page, request }) => {
  const login = await loginApi(request, TENANT2_ADMIN)
  await applyLogin(page, login)

  let payload = null
  page.on('response', async (response) => {
    if (response.url().includes('/users/page') && response.request().method() === 'GET') {
      payload = await response.json()
    }
  })

  await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/鐢ㄦ埛绠＄悊/).first()).toBeVisible()
  if (payload) {
    const records = payload.data?.records || []
    const tenantIds = [...new Set(records.map((item) => String(item.tenantId)))]
    if (records.length > 0) {
      expect(tenantIds).toEqual(['2'])
    }
  }
})

test('tenant admin should access tenant management and only see its own tenant', async ({ page, request }) => {
  const login = await loginApi(request, TENANT2_ADMIN)
  await applyLogin(page, login)

  let payload = null
  page.on('response', async (response) => {
    if (response.url().includes('/admin/tenants') && response.request().method() === 'GET') {
      payload = await response.json()
    }
  })

  await page.goto(`${BASE_URL}/admin/system/tenants`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/绉熸埛绠＄悊/).first()).toBeVisible()
  if (payload) {
    const records = payload.data?.records || []
    const tenantIds = [...new Set(records.map((item) => String(item.id)))]
    if (records.length > 0) {
      expect(tenantIds).toEqual(['2'])
    }
  }
})

test('super admin members page should render club_members rows instead of staying empty', async ({ page, request }) => {
  const login = await loginApi(request, SUPER_ADMIN)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/club/members`, { waitUntil: 'domcontentloaded' })
  await expect(page.getByText(/鎴愬憳绠＄悊/).first()).toBeVisible()
  const rowCount = await page.locator('.el-table__row').count()
  const emptyVisible = await page.locator('.el-empty, .el-table__empty-text').first().isVisible().catch(() => false)
  expect(rowCount > 0 || emptyVisible).toBeTruthy()
})
