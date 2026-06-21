// @ts-check
const { test, expect } = require('@playwright/test')
const {
  TENANT1_MANAGER,
  TENANT1_MEMBER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function applyLogin(page, loginData) {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
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

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

async function waitForLoadingMaskToClear(page) {
  await page.waitForFunction(() => !document.querySelector('.el-loading-mask'))
}

test('manager should cancel one registration and save results for the remaining registration', async ({ page, request }) => {
  test.setTimeout(90000)

  const seed = Date.now()
  const title = `competition-admin-${seed}`
  const managerAuth = await loginApi(request, TENANT1_MANAGER)
  const memberAuth = await loginApi(request, TENANT1_MEMBER)
  const teacherAuth = await loginApi(request, TENANT1_LIVE_TEACHER)

  const now = new Date()
  const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
  const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000)
  const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  const tomorrowPlusTwo = new Date(now.getTime() + 26 * 60 * 60 * 1000)

  const createRes = await request.post(`${API_BASE}/api/competition`, {
    headers: {
      Authorization: `Bearer ${managerAuth.token}`,
      'X-Tenant-Id': managerAuth.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      title,
      description: 'competition admin completion test',
      competitionType: 'coding',
      level: 'school',
      organizer: 'tenant1',
      registrationStart: formatDateTime(oneHourAgo),
      registrationEnd: formatDateTime(oneHourLater),
      competitionStart: formatDateTime(tomorrow),
      competitionEnd: formatDateTime(tomorrowPlusTwo),
      maxParticipants: 30,
      isPublished: true
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  const competitionId = createBody.data

  const approveRes = await request.post(`${API_BASE}/api/competition/${competitionId}/review/teacher`, {
    headers: {
      Authorization: `Bearer ${teacherAuth.token}`,
      'X-Tenant-Id': teacherAuth.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      approved: true,
      reviewComment: 'competition admin completion test approval'
    }
  })
  const approveBody = await approveRes.json()
  expect(approveBody.code).toBe(200)

  for (const participant of [
    {
      auth: memberAuth,
      payload: {
        name: 'member_a',
        studentId: `member-${seed}`,
        phone: '13800138000',
        email: `member-${seed}@example.com`
      }
    },
    {
      auth: teacherAuth,
      payload: {
        name: 't01_teacher',
        studentId: `teacher-${seed}`,
        phone: '13800138001',
        email: `teacher-${seed}@example.com`
      }
    }
  ]) {
    const registerRes = await request.post(`${API_BASE}/api/competition/${competitionId}/register`, {
      headers: {
        Authorization: `Bearer ${participant.auth.token}`,
        'X-Tenant-Id': participant.auth.organizationId,
        'Content-Type': 'application/json'
      },
      data: participant.payload
    })
    const registerBody = await registerRes.json()
    expect(registerBody.code).toBe(200)
  }

  await applyLogin(page, managerAuth)
  await page.goto(`${BASE_URL}/admin/activity/competition`, { waitUntil: 'networkidle' })
  await page.locator('.search-form input').first().fill(title)
  await page.locator('.search-form .el-button--primary').click()
  await page.waitForLoadState('networkidle')
  await waitForLoadingMaskToClear(page)

  const row = page.locator('.el-table__row').filter({ hasText: title }).first()
  await expect(row).toBeVisible()

  await row.locator('.el-button').nth(1).click()
  const teamDialog = page.locator('.el-dialog').last()
  const teamRows = teamDialog.locator('.el-table__row')
  await expect(teamRows).toHaveCount(2, { timeout: 10000 })
  await teamRows.filter({ hasText: `member-${seed}@example.com` }).first().locator('.el-button').last().click()

  const confirmDialog = page.locator('.el-message-box').last()
  await expect(confirmDialog).toBeVisible()
  await confirmDialog.locator('.el-button--primary').click()
  await expect(teamRows).toHaveCount(1, { timeout: 10000 })

  await teamDialog.locator('.el-dialog__headerbtn').click()
  await expect(teamDialog).toBeHidden()

  await row.locator('.el-button').nth(2).click()
  await waitForLoadingMaskToClear(page)
  const resultDialog = page.locator('.el-dialog').last()
  const resultRows = resultDialog.locator('.el-table__row')
  await expect(resultRows).toHaveCount(1, { timeout: 10000 })

  const resultRow = resultRows.first()
  await resultRow.locator('.el-input-number input').first().fill('1')
  await resultRow.locator('.el-input-number input').nth(1).fill('95')
  await resultRow.locator('.el-select').click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()
  await resultDialog.locator('.el-dialog__footer .el-button--primary').first().click()
  await expect(resultDialog).toBeHidden({ timeout: 10000 })

  const resultsRes = await request.get(`${API_BASE}/api/competition/${competitionId}/results`, {
    headers: {
      Authorization: `Bearer ${managerAuth.token}`,
      'X-Tenant-Id': managerAuth.organizationId
    }
  })
  const resultsBody = await resultsRes.json()
  expect(resultsBody.code).toBe(200)
  expect(resultsBody.data).toHaveLength(1)
  expect(resultsBody.data[0].score).toBe(95)
  expect(resultsBody.data[0].ranking).toBe(1)
})
