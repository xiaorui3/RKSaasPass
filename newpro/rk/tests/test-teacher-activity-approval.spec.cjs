const { test, expect } = require('@playwright/test')
const {
  TENANT1_MEMBER,
  TENANT1_MANAGER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MEMBER_ACCOUNT = TENANT1_MEMBER
const MANAGER_ACCOUNT = TENANT1_MANAGER
const TEST_ACCOUNT = TENANT1_LIVE_TEACHER

async function loginApi(request, user) {
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
  return body.data
}

async function loginByApi(page) {
  const loginData = await loginApi(page.request, TEST_ACCOUNT)
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
  }, loginData)
}

async function openApprovalPage(page) {
  await page.goto(`${BASE_URL}/admin/activity/approval`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.admin-page, .el-table').first()).toBeVisible()
}

async function confirmDialogIfVisible(page) {
  const confirmButton = page.locator('.el-message-box__btns .el-button--primary').last()
  if (await confirmButton.isVisible().catch(() => false)) {
    await confirmButton.click()
    await page.waitForTimeout(800)
  }
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function createTeacherPendingActivity(request, seed) {
  const memberLogin = await loginApi(request, MEMBER_ACCOUNT)
  const managerLogin = await loginApi(request, MANAGER_ACCOUNT)
  const title = `teacher-review-activity-${seed}`
  const now = new Date()
  const submitRes = await request.post(`${API_BASE}/api/activity/submit`, {
    headers: {
      Authorization: `Bearer ${memberLogin.token}`,
      'X-Tenant-Id': memberLogin.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      activityName: title,
      activityType: 1,
      location: `QA-ROOM-${seed}`,
      maxParticipants: 20,
      startTime: formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)),
      endTime: formatDateTime(new Date(now.getTime() + 26 * 60 * 60 * 1000)),
      registrationStartTime: formatDateTime(new Date(now.getTime() - 60 * 60 * 1000)),
      registrationEndTime: formatDateTime(new Date(now.getTime() + 60 * 60 * 1000)),
      content: 'teacher activity approval test content'
    }
  })
  const submitBody = await submitRes.json()
  if (submitBody.code !== 200 || !submitBody.data) {
    throw new Error(`Create pending activity failed: ${JSON.stringify(submitBody)}`)
  }
  const activityId = submitBody.data

  const approveByManagerRes = await request.post(`${API_BASE}/api/activity/${activityId}/review/manager`, {
    headers: {
      Authorization: `Bearer ${managerLogin.token}`,
      'X-Tenant-Id': managerLogin.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      approved: true,
      reviewComment: 'manager approved for teacher review test'
    }
  })
  const approveByManagerBody = await approveByManagerRes.json()
  if (approveByManagerBody.code !== 200) {
    throw new Error(`Manager review failed: ${JSON.stringify(approveByManagerBody)}`)
  }

  return { activityId, title }
}

test.describe('Teacher Activity Approval Module Tests', () => {
  test('WU-19-01: Login and Permission Verification', async ({ page }) => {
    await loginByApi(page)
    await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' })
    await expect(page).not.toHaveURL(/\/login/)
    await expect(page.locator('.admin-layout, .admin-page, .layout-container').first()).toBeVisible()
  })

  test('WU-19-02: Read Test - View Pending Activities', async ({ page }) => {
    await loginByApi(page)
    await openApprovalPage(page)
    await expect(page.locator('.el-table, .admin-page').first()).toBeVisible()
  })

  test('WU-19-03: Update Test - Approve Activity', async ({ page }) => {
    const seed = Date.now()
    const pending = await createTeacherPendingActivity(page.request, seed)
    await loginByApi(page)
    await openApprovalPage(page)
    const searchInput = page.getByPlaceholder('璇疯緭鍏ユ椿鍔ㄥ悕')
    await searchInput.fill(pending.title)
    await page.locator('.search-form .el-button--primary').click()
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)

    const row = page.locator('.el-table__row').filter({ hasText: pending.title }).first()
    await expect(row).toBeVisible()

    const approveButton = row.getByText('閫氳繃').first()
    await expect(approveButton).toBeVisible()

    await approveButton.click()
    await confirmDialogIfVisible(page)
    await expect(page.locator('.el-message, .el-table').first()).toBeVisible()
  })

  test('WU-19-04: Update Test - Reject Activity', async ({ page }) => {
    const seed = Date.now()
    const pending = await createTeacherPendingActivity(page.request, seed)
    await loginByApi(page)
    await openApprovalPage(page)
    const searchInput = page.getByPlaceholder('璇疯緭鍏ユ椿鍔ㄥ悕')
    await searchInput.fill(pending.title)
    await page.locator('.search-form .el-button--primary').click()
    await page.waitForLoadState('domcontentloaded')
    await page.waitForTimeout(1000)

    const row = page.locator('.el-table__row').filter({ hasText: pending.title }).first()
    await expect(row).toBeVisible()

    const rejectButton = row.getByText('鎷掔粷').first()
    await expect(rejectButton).toBeVisible()

    await rejectButton.click()
    const reasonInput = page.locator('textarea').first()
    if (await reasonInput.isVisible().catch(() => false)) {
      await reasonInput.fill('鑷姩鍖栨祴璇曟嫆缁濆師')
    }
    await confirmDialogIfVisible(page)
    await expect(page.locator('.el-message, .el-table').first()).toBeVisible()
  })
})
