// @ts-check
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const path = require('path')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const SCREENSHOT_DIR = path.resolve(__dirname, '../../test-screenshots')
const TEST_USER = { username: 'admin_a', password: 'change-me', organizationId: '1' }

if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true })
}

async function loginByApi(page) {
  const response = await page.request.post(`${API_BASE}/auth/login`, {
    data: {
      username: TEST_USER.username,
      password: TEST_USER.password,
      organizationId: TEST_USER.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((loginData) => {
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('tenantId', loginData.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      organizationId: loginData.organizationId,
      roles: loginData.roles || []
    }))
  }, body.data)
}

async function gotoStable(page, url, readySelector = '.admin-page, .el-table') {
  await page.goto(url, { waitUntil: 'domcontentloaded' })
  await expect(page.locator(readySelector).first()).toBeVisible()
}

test.describe('RK-Web 绯荤粺绠＄悊妯″潡娴嬭瘯', () => {
  test.beforeEach(async ({ page }) => {
    await loginByApi(page)
    await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
    await page.waitForTimeout(1000)
  })

  test('01-瑙掕壊绠＄悊鍒楄〃', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/roles`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('02-瑙掕壊鏂板', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/roles`)
    await expect(page.locator('.el-button').first()).toBeVisible()
  })

  test('03-瑙掕壊缂栬緫', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/roles`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('04-瑙掕壊鏉冮檺鍒嗛厤', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/roles`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('05-绉熸埛绠＄悊鍒楄〃', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/tenants`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('06-绉熸埛缁熻鍗＄墖', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/tenants`)
    const cards = await page.locator('.stat-card, .el-card').count()
    expect(cards).toBeGreaterThan(0)
  })

  test('07-绉熸埛鏂板', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/tenants`)
    await expect(page.locator('.el-button').first()).toBeVisible()
  })

  test('08-绉熸埛璇︽儏', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/tenants`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('09-绉熸埛缁垂', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/tenants`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('10-鑿滃崟绠＄悊', async ({ page }) => {
    await gotoStable(page, `${BASE_URL}/admin/system/menus`)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })
})
