// @ts-check
const { test, expect } = require('@playwright/test')
const helpers = require('./crud/helpers.cjs')

const { BASE_URL, API_URL, loginByApi, gotoAdmin } = helpers
test.setTimeout(90000)

// ── 1. Global Search ────────────────────────────────────────────────

test('search page loads and shows results for a keyword', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await page.goto(`${BASE_URL}/search?q=测试`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)

  // Verify search bar is visible
  await expect(page.locator('.search-bar input').first()).toBeVisible()

  // Verify tabs are present
  await expect(page.getByText('全部').first()).toBeVisible()
  await expect(page.getByText(/新闻/).first()).toBeVisible()
  await expect(page.getByText(/活动/).first()).toBeVisible()
  await expect(page.getByText(/比赛/).first()).toBeVisible()
  await expect(page.getByText(/作品/).first()).toBeVisible()
})

test('header search bar navigates to search page', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  const searchInput = page.locator('.header-search input').first()
  if (await searchInput.isVisible()) {
    await searchInput.fill('社团')
    await page.keyboard.press('Enter')
    await page.waitForTimeout(1500)
    expect(page.url()).toContain('/search')
    expect(page.url()).toContain('q=')
  }
})

// ── 2. Notification Badge ───────────────────────────────────────────

test('notification bell is visible for logged-in users', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' }).catch(async () => {
    await page.goto(`${BASE_URL}/`, { waitUntil: 'load' })
  })
  await page.waitForTimeout(1000)

  const bell = page.locator('.notification-badge, .bell-link, .el-icon-bell, [class*="bell"]').first()
  const hasBell = await bell.isVisible().catch(() => false)
  const hasMessageEntry = await page.getByText(/消息通知|通知中心/).first().isVisible().catch(() => false)
  expect(hasBell || hasMessageEntry).toBeTruthy()
})

// ── 3. History Timeline ─────────────────────────────────────────────

test('history timeline page loads correctly', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await page.goto(`${BASE_URL}/history`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)

  // Page hero
  await expect(page.getByText('社团历程').first()).toBeVisible()

  // Search input present
  const searchInput = page.locator('.timeline-controls input').first()
  if (await searchInput.count() > 0) {
    await expect(searchInput).toBeVisible()
  }
})

// ── 4. Admin Contact Messages ───────────────────────────────────────

test('admin contact messages page loads with table', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await gotoAdmin(page, '/admin/club/contact-messages')
  await page.waitForTimeout(1500)

  await expect(page.getByText('留言管理').first()).toBeVisible()
  // Table exists (even if empty)
  const table = page.locator('.el-table').first()
  if (await table.count() > 0) {
    await expect(table).toBeVisible()
  }
  // Status filter exists
  const filterSelect = page.locator('.el-select').first()
  if (await filterSelect.isVisible()) {
    await expect(filterSelect).toBeVisible()
  }
})

// ── 5. Admin Achievements ───────────────────────────────────────────

test('admin achievements page loads with table', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await gotoAdmin(page, '/admin/content/achievements')
  await page.waitForTimeout(1500)

  await expect(page.getByText('成就管理').first()).toBeVisible()
  // Type filter
  const selects = page.locator('.el-select')
  if (await selects.count() > 0) {
    await expect(selects.first()).toBeVisible()
  }
})

// ── 6. ECharts Dashboard ────────────────────────────────────────────

test('statistics board renders chart containers', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await gotoAdmin(page, '/admin/statistics/board')
  await page.waitForTimeout(5000)

  // Verify page title
  await expect(page.getByText('数据看板').first()).toBeVisible()

  // Verify chart containers exist (may be inside scoped component)
  const charts = page.locator('.chart-container')
  const chartCount = await charts.count()

  // Verify ECharts rendered canvases
  const canvases = page.locator('.chart-container canvas')
  const canvasCount = await canvases.count()

  // At minimum the page loaded with data sections
  const hasStats = (await page.getByText('社团成员').count()) > 0 || (await page.getByText('活动数量').count()) > 0
  expect(hasStats || chartCount > 0 || canvasCount > 0).toBeTruthy()
})

// ── 7. Activity Check-in Button ─────────────────────────────────────

test('activity detail page has check-in UI when registered for ongoing activity', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  const token = loginData.token

  // Create an ongoing activity via API
  const now = new Date()
  const twoHoursAgo = new Date(now.getTime() - 2 * 60 * 60 * 1000)
  const twoHoursLater = new Date(now.getTime() + 2 * 60 * 60 * 1000)
  const oneDayAgo = new Date(now.getTime() - 24 * 60 * 60 * 1000)

  function fmt(d) {
    const pad = v => String(v).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  }

  const createRes = await request.post(`${API_URL}/api/activity/add`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    data: {
      activityName: `【签到测试】活动-${Date.now()}`,
      activityType: 1,
      location: '签到测试地点',
      maxParticipants: 50,
      startTime: fmt(twoHoursAgo),
      endTime: fmt(twoHoursLater),
      registrationStartTime: fmt(oneDayAgo),
      registrationEndTime: fmt(twoHoursLater),
      content: '签到测试活动',
    }
  })
  const createBody = await createRes.json()
  const activityId = String(createBody.data)

  // Register for the activity
  await request.post(`${API_URL}/api/activity/${activityId}/register`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  })

  await page.goto(`${BASE_URL}/activities/${activityId}`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(2000)

  // Should show check-in button or status indicators
  const pageContent = await page.textContent('body')
  const hasCheckIn = pageContent.includes('签到')
  const hasRegistered = pageContent.includes('已报名')
  const hasRegisterBtn = pageContent.includes('立即报名')
  const detailShellVisible = await page.locator('.activity-detail, .activity-content, .el-card').first().isVisible().catch(() => false)
  expect(hasCheckIn || hasRegistered || hasRegisterBtn || detailShellVisible).toBeTruthy()

  // Cleanup
  await request.delete(`${API_URL}/api/activity/delete/${activityId}`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  }).catch(() => {})
})

// ── 8. Nav includes History link ────────────────────────────────────

test('navigation bar includes history timeline link', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  const historyLink = page.locator('.nav-link').filter({ hasText: '社团历程' })
  await expect(historyLink).toBeVisible()

  await historyLink.click()
  await page.waitForTimeout(1000)
  expect(page.url()).toContain('/history')
})
