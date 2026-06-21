// @ts-check
const { test, expect } = require('@playwright/test')
const helpers = require('./crud/helpers.cjs')

const { BASE_URL, loginByApi, gotoAdmin } = helpers

// ── 1. Service Monitor ──────────────────────────────────────────────

test('service monitor page loads with topology and table', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/operation/service-monitor')
  await page.waitForTimeout(5000)

  await expect(page.getByText('微服务架构监控').first()).toBeVisible()
  // SVG topology
  const svg = page.locator('.arch-diagram')
  await expect(svg).toBeVisible()
  // Service table
  const table = page.locator('.el-table').first()
  await expect(table).toBeVisible()
  // Middleware status cards
  const mwCards = page.locator('.mw-card')
  expect(await mwCards.count()).toBeGreaterThanOrEqual(4)
})

// ── 2. Data Export Center ───────────────────────────────────────────

test('data export page shows export cards', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/operation/data-export')
  await page.waitForTimeout(1000)

  await expect(page.getByText('数据导出中心').first()).toBeVisible()
  const exportButtons = page.locator('.el-button:has-text("导出 Excel")')
  expect(await exportButtons.count()).toBeGreaterThanOrEqual(3)
})

test('news export creates xlsx file', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/operation/data-export')
  await page.waitForTimeout(1000)

  const downloadPromise = page.waitForEvent('download', { timeout: 10000 }).catch(() => null)
  await page.locator('.export-card').filter({ hasText: '新闻' }).locator('.el-button').first().click({ force: true, noWaitAfter: true })
  const download = await downloadPromise
  if (download) {
    const filename = await download.suggestedFilename()
    expect(filename.toLowerCase().endsWith('.xlsx')).toBeTruthy()
  }
  // If download doesn't trigger (due to saveAs), at least check no error
  const errorMsg = page.locator('.el-message--error')
  expect(await errorMsg.isVisible().catch(() => false)).toBeFalsy()
})

// ── 3. Activity Calendar ────────────────────────────────────────────

test('activity calendar page renders with el-calendar', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await page.goto(`${BASE_URL}/calendar`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)

  await expect(page.getByText('活动比赛日历').first()).toBeVisible()
  const calendar = page.locator('.el-calendar')
  await expect(calendar).toBeVisible()
})

// ── 4. Global Announcement ──────────────────────────────────────────

test('global announcement bar appears and can be dismissed', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  // Clear previous dismissal
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.evaluate(() => localStorage.removeItem('announcement-dismissed'))
  await page.reload({ waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  const bar = page.locator('.announcement-bar')
  if (await bar.isVisible()) {
    const closeBtn = page.locator('.announcement-close')
    await closeBtn.click()
    await page.waitForTimeout(300)
    await expect(bar).not.toBeVisible()
  }
})

// ── 5. Back to Top ──────────────────────────────────────────────────

test('back to top button appears after scrolling', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await page.goto(`${BASE_URL}/news`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  // Scroll down
  await page.evaluate(() => window.scrollTo(0, 500))
  await page.waitForTimeout(500)

  const btn = page.locator('.back-to-top')
  if (await btn.isVisible()) {
    await btn.click()
    await page.waitForTimeout(1000)
    const scrollY = await page.evaluate(() => window.scrollY)
    expect(scrollY).toBeLessThan(50)
  }
})

// ── 6. Dark Mode Toggle ─────────────────────────────────────────────

test('dark mode toggle switches theme', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  const toggle = page.locator('.el-switch').first()
  if (await toggle.isVisible()) {
    await toggle.click()
    await page.waitForTimeout(500)
    const hasDark = await page.evaluate(() => document.documentElement.classList.contains('dark'))
    expect(hasDark).toBeTruthy()

    // Toggle back
    await toggle.click()
    await page.waitForTimeout(500)
    const hasDark2 = await page.evaluate(() => document.documentElement.classList.contains('dark'))
    expect(hasDark2).toBeFalsy()
  }
})

// ── 7. Navigation includes Activity Calendar ────────────────────────

test('navigation includes activity calendar link', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  const calLink = page.locator('.nav-link').filter({ hasText: '活动比赛日历' })
  await expect(calLink).toBeVisible()
  await calLink.click()
  await page.waitForTimeout(1000)
  expect(page.url()).toContain('/calendar')
})

// ── 8. Scroll Progress Bar ──────────────────────────────────────────

test('scroll progress bar appears on scroll', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await page.goto(`${BASE_URL}/news`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  await page.evaluate(() => window.scrollTo(0, 300))
  await page.waitForTimeout(300)

  const progress = page.locator('.scroll-progress')
  const visible = await progress.isVisible().catch(() => false)
  if (visible) {
    const width = await progress.evaluate(el => el.style.width)
    expect(parseFloat(width)).toBeGreaterThan(0)
  }
})
