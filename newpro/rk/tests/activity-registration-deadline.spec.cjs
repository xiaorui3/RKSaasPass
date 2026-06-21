// @ts-check
const { test, expect } = require('@playwright/test')
const { TENANT1_LIVE_TEACHER } = require('./helpers/test-users.cjs')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }
const MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
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

test('expired activity should reject registration even if stale activityStatus still says registering', async ({ request }) => {
  const manager = await loginApi(request, MANAGER)
  const member = await loginApi(request, MEMBER)
  const now = new Date()
  const seed = Date.now()

  const createRes = await request.post(`${API_BASE}/api/activity/add`, {
    headers: {
      Authorization: `Bearer ${manager.token}`,
      'X-Tenant-Id': manager.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      activityName: `expired-activity-${seed}`,
      activityType: 1,
      organizer: 'tenant1',
      location: 'room-101',
      content: 'expired registration activity test',
      maxParticipants: 20,
      startTime: formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)),
      endTime: formatDateTime(new Date(now.getTime() + 26 * 60 * 60 * 1000)),
      registrationStartTime: formatDateTime(new Date(now.getTime() - 48 * 60 * 60 * 1000)),
      registrationEndTime: formatDateTime(new Date(now.getTime() - 24 * 60 * 60 * 1000)),
      activityStatus: 2
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  const activityId = createBody.data

  const registerRes = await request.post(`${API_BASE}/api/activity/${activityId}/register`, {
    headers: {
      Authorization: `Bearer ${member.token}`,
      'X-Tenant-Id': member.organizationId
    }
  })
  const registerBody = await registerRes.json()

  expect(registerBody.code).not.toBe(200)
  expect(registerBody.msg || '').toMatch(/鎴|鎶ュ悕|鏈熼棿/)
})

test('expired competition should reject registration', async ({ request }) => {
  const manager = await loginApi(request, MANAGER)
  const member = await loginApi(request, MEMBER)
  const now = new Date()
  const seed = Date.now()

  const createRes = await request.post(`${API_BASE}/api/competition`, {
    headers: {
      Authorization: `Bearer ${manager.token}`,
      'X-Tenant-Id': manager.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      title: `expired-competition-${seed}`,
      description: 'expired competition registration test',
      competitionType: 'coding',
      level: 'school',
      organizer: 'tenant1',
      registrationStart: formatDateTime(new Date(now.getTime() - 48 * 60 * 60 * 1000)),
      registrationEnd: formatDateTime(new Date(now.getTime() - 24 * 60 * 60 * 1000)),
      competitionStart: formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)),
      competitionEnd: formatDateTime(new Date(now.getTime() + 48 * 60 * 60 * 1000)),
      maxParticipants: 10,
      isPublished: true
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  const competitionId = createBody.data

  const teacher = await loginApi(request, TENANT1_LIVE_TEACHER)
  const approveRes = await request.post(`${API_BASE}/api/competition/${competitionId}/review/teacher`, {
    headers: {
      Authorization: `Bearer ${teacher.token}`,
      'X-Tenant-Id': teacher.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      approved: true,
      reviewComment: 'expired competition approval'
    }
  })
  const approveBody = await approveRes.json()
  expect(approveBody.code).toBe(200)

  const registerRes = await request.post(`${API_BASE}/api/competition/${competitionId}/register`, {
    headers: {
      Authorization: `Bearer ${member.token}`,
      'X-Tenant-Id': member.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      name: 'member_a',
      studentId: `cmp-${seed}`,
      phone: '13800138000',
      email: `cmp-${seed}@example.com`
    }
  })
  const registerBody = await registerRes.json()

  expect(registerBody.code).not.toBe(200)
  expect(registerBody.msg || '').toContain('鎴')
})

test('expired activity detail page should not show register button', async ({ page, request }) => {
  const manager = await loginApi(request, MANAGER)
  const member = await loginApi(request, MEMBER)
  const now = new Date()
  const seed = Date.now()

  const createRes = await request.post(`${API_BASE}/api/activity/add`, {
    headers: {
      Authorization: `Bearer ${manager.token}`,
      'X-Tenant-Id': manager.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      activityName: `expired-activity-page-${seed}`,
      activityType: 1,
      organizer: 'tenant1',
      location: 'room-202',
      content: 'expired activity detail page test',
      maxParticipants: 20,
      startTime: formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)),
      endTime: formatDateTime(new Date(now.getTime() + 26 * 60 * 60 * 1000)),
      registrationStartTime: formatDateTime(new Date(now.getTime() - 48 * 60 * 60 * 1000)),
      registrationEndTime: formatDateTime(new Date(now.getTime() - 24 * 60 * 60 * 1000)),
      activityStatus: 2
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)

  await applyLogin(page, member)
  await page.goto(`${BASE_URL}/activities/${createBody.data}`, { waitUntil: 'networkidle' })
  await expect(page.getByRole('button', { name: '绔嬪嵆鎶ュ悕' })).toHaveCount(0)
})
