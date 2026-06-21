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

async function querySwitchableTenants(request, loginData) {
  const res = await request.get(`${API_BASE}/auth/switchable-tenants`, {
    headers: {
      Authorization: `Bearer ${loginData.token}`,
      'X-Tenant-Id': loginData.organizationId
    }
  })
  const body = await res.json()
  if (body.code !== 200) {
    throw new Error(`switchable-tenants failed: ${JSON.stringify(body)}`)
  }
  return body.data || []
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

test('multi-tenant UX should show tenant names in switch menu and memberships in profile', async ({ page, request }) => {
  test.setTimeout(60000)
  const login = await loginApi(request, MEMBER)
  const tenantsRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantsBody = await tenantsRes.json()
  const tenants = tenantsBody.data || []
  const switchableTenants = await querySwitchableTenants(request, login)

  expect(switchableTenants.length).toBeGreaterThan(1)

  const tenantNameMap = new Map(tenants.map((item) => [String(item.id), item.tenantName]))
  const firstTenantName = tenantNameMap.get(String(switchableTenants[0].tenantId))
  const secondTenantName = tenantNameMap.get(String(switchableTenants[1].tenantId))

  await applyLogin(page, login)
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })

  await page.locator('.tenant-switch').first().click()
  await expect(page.getByRole('menuitem').filter({ hasText: firstTenantName }).first()).toBeVisible()
  await expect(page.getByRole('menuitem').filter({ hasText: secondTenantName }).first()).toBeVisible()

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.tenant-membership-list')).toBeVisible()
  await expect(page.locator('.tenant-membership-name').filter({ hasText: firstTenantName }).first()).toBeVisible()
  await expect(page.locator('.tenant-membership-name').filter({ hasText: secondTenantName }).first()).toBeVisible()
})
