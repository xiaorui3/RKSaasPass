// @ts-check
const { test, expect } = require('@playwright/test')
const { TENANT2_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN_B = TENANT2_ADMIN

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function applyLogin(page, loginData) {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((data) => {
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

test('tenant admin should have backend entry and profile/about should work for tenant 2', async ({ page, request }) => {
  const login = await loginApi(request, ADMIN_B)
  const tenantTitle = `tenant2-profile-${Date.now()}`
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  await page.locator('.user-name').click()
  await expect(page.getByText(/鍚庡彴绠＄悊/)).toBeVisible()
  await page.keyboard.press('Escape')

  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  await expect(page).toHaveURL(/\/admin/)

  await page.goto(`${BASE_URL}/admin/club/profile-config`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.club-profile-config-page')).toBeVisible()
  const headers = {
    Authorization: `Bearer ${login.token}`,
    'X-Tenant-Id': login.organizationId
  }
  const currentRes = await request.get(`${API_BASE}/api/config/club-profile/current`, { headers })
  const currentBody = await currentRes.json()
  expect(currentBody.code).toBe(200)

  const saveRes = await request.put(`${API_BASE}/api/config/club-profile/current`, {
    headers: {
      ...headers,
      'Content-Type': 'application/json'
    },
    data: {
      ...(currentBody.data || {}),
      pageTitle: tenantTitle
    }
  })
  const saveBody = await saveRes.json()
  expect(saveBody.code).toBe(200)

  const deadline = Date.now() + 15000
  let saved = false
  while (Date.now() < deadline) {
    const verifyRes = await request.get(`${API_BASE}/api/config/club-profile/current`, { headers })
    const verifyBody = await verifyRes.json()
    if (verifyBody.code === 200 && verifyBody.data?.pageTitle === tenantTitle) {
      saved = true
      break
    }
    await page.waitForTimeout(500)
  }
  expect(saved).toBeTruthy()

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  await expect(page.locator('.profile-page')).toBeVisible()
  await expect(page.locator('.el-message').filter({ hasText: /鐢ㄦ埛id涓嶅瓨/ })).toHaveCount(0)

  await page.goto(`${BASE_URL}/about`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.about-page')).toBeVisible()
  await expect(page.getByText(tenantTitle)).toBeVisible()
})
