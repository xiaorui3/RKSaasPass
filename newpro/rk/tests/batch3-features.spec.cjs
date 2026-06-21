// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = {
  username: 'admin_a',
  password: 'change-me',
  organizationId: '1'
}

async function loginByApi(page, request, user = ADMIN) {
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

  await page.addInitScript((loginData) => {
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

async function openAdminRoute(page, path, selector) {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  if (selector) {
    const specific = page.locator(selector)
    if (await specific.count()) {
      await expect(specific.first()).toBeVisible({ timeout: 15000 })
      await expect(page.locator('text=404')).toHaveCount(0)
      return
    }
  }
  await expect(page.locator('main')).toBeVisible({ timeout: 15000 })
  await expect(page.locator('text=404')).toHaveCount(0)
}

test.describe('batch3 smoke - admin routes', () => {
  test.beforeEach(async ({ page, request }) => {
    await loginByApi(page, request)
  })

  test('wiki page should load', async ({ page }) => {
    await openAdminRoute(page, '/admin/content/wiki', '.wiki-page')
  })

  test('photo gallery page should load', async ({ page }) => {
    await openAdminRoute(page, '/admin/activity/photo-gallery', '.gallery-page')
  })

  test('member graph page should load', async ({ page }) => {
    await openAdminRoute(page, '/admin/club/member-graph', '.member-graph-page')
  })

  test('vote page should load and open create dialog', async ({ page }) => {
    await openAdminRoute(page, '/admin/activity/vote', '.vote-page')
    const createBtn = page.getByRole('button', { name: /鍒涘缓|Create/i }).first()
    if (await createBtn.isVisible().catch(() => false)) {
      await createBtn.click()
      await expect(page.locator('.el-dialog').last()).toBeVisible()
    }
  })

  test('resource booking page should load', async ({ page }) => {
    await openAdminRoute(page, '/admin/club/resources', '.resource-booking-page')
  })

  test('data diff page should load', async ({ page }) => {
    await openAdminRoute(page, '/admin/content/data-diff', '.data-diff-page')
  })

  test('dashboard and core admin pages should load', async ({ page }) => {
    await openAdminRoute(page, '/admin', '.dashboard')
    await openAdminRoute(page, '/admin/content/news', '.admin-page')
    await openAdminRoute(page, '/admin/club/members', '.admin-page')
    await openAdminRoute(page, '/admin/activity/list', '.admin-page')
    await openAdminRoute(page, '/admin/system/tenants', '.admin-page')
    await openAdminRoute(page, '/admin/statistics/board', '.admin-page')
    await openAdminRoute(page, '/admin/operation/service-monitor', '.admin-page')
    await openAdminRoute(page, '/admin/operation/data-export', '.admin-page')
  })

  test('admin layout should render menu entries', async ({ page }) => {
    await openAdminRoute(page, '/admin', '.dashboard')
    const menuItems = page.locator('.el-menu-item, .el-sub-menu__title')
    expect(await menuItems.count()).toBeGreaterThan(5)
  })
})

test.describe('batch3 smoke - public routes', () => {
  test('public pages should render without crash', async ({ page }) => {
    for (const path of ['/search?q=e2e', '/history', '/calendar']) {
      await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' })
      await expect(page.locator('body')).toBeVisible()
      await expect(page.locator('vite-error-overlay')).toHaveCount(0)
    }
  })
})
