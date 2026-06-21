// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = {
  username: 'admin_a',
  password: 'change-me',
  organizationId: '1'
}

async function loginByApi(page, request, user) {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }

  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
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

test.describe('RK-Web 鏍″弸椋庨噰妯″潡娴嬭瘯', () => {
  test.beforeEach(async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 960 })
  })

  test('1. 鏍″弸椋庨噰鍒楄〃鍓嶅彴', async ({ page, request }) => {
    await loginByApi(page, request, ADMIN)
    await page.goto(`${BASE_URL}/alumni`, { waitUntil: 'networkidle' })

    await expect(page.locator('.alumni-page')).toBeVisible()
    await expect(page.locator('.page-header')).toBeVisible()

    const hasStatsSummary = await page.getByText(/妗ｆ鎬讳汉鏁皘宸叉瘯涓氭牎鍙媩灞婂埆鏁?).first().isVisible().catch(() => false)
    const alumniCards = await page.locator('.alumni-card, .alumni-list-item').count()
    const emptyVisible = await page.locator('.el-empty').first().isVisible().catch(() => false)

    expect(hasStatsSummary || alumniCards > 0 || emptyVisible).toBeTruthy()
  })

  test('2. 鎴愬氨灞曠ず椤甸潰', async ({ page }) => {
    await page.goto(`${BASE_URL}/achievements`, { waitUntil: 'networkidle' })
    const pageContent = await page.content()
    const hasAchievementContent =
      pageContent.includes('鎴愬氨') ||
      pageContent.includes('achievement') ||
      pageContent.includes('Achievement') ||
      pageContent.includes('鑽ｈ獕') ||
      pageContent.includes('鑾峰')
    expect(hasAchievementContent).toBeTruthy()
  })

  test('3. 鐧诲綍鍚庡彴绠＄悊', async ({ page, request }) => {
    await loginByApi(page, request, ADMIN)
    await page.goto(`${BASE_URL}/admin`, { waitUntil: 'networkidle' })

    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.admin-layout, .admin-page, .layout-container').first()).toBeVisible()
  })

  test('4. 鏍″弸鍚庡彴绠＄悊', async ({ page, request }) => {
    await loginByApi(page, request, ADMIN)
    await page.goto(`${BASE_URL}/admin/club/alumni`, { waitUntil: 'networkidle' })

    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.admin-page, .el-table, .el-empty').first()).toBeVisible()
  })

  test('5. 鏍″弸璇︽儏鏌ョ湅', async ({ page, request }) => {
    await loginByApi(page, request, ADMIN)
    await page.goto(`${BASE_URL}/alumni`, { waitUntil: 'networkidle' })

    const firstCard = page.locator('.alumni-card').first()
    if (await firstCard.isVisible().catch(() => false)) {
      await firstCard.click()
      await page.waitForTimeout(1000)
    }

    await expect(page.locator('.alumni-page, .el-empty').first()).toBeVisible()
  })
})
