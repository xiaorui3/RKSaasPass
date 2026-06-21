// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ADMIN,
  ensureStableAdmissionConfig,
  fillStableRegisterFields,
  loginApi
} = require('./helpers/admission-form-helpers.cjs')
const { cleanupRegisterFixture } = require('./helpers/live-fixture-cleanup.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const REDIS_HOST = '127.0.0.1'
const REDIS_PORT = 6379
const REDIS_PASSWORD = 'change-me'

function buildRedisCommand(parts) {
  const payload = [`*${parts.length}`]
  for (const part of parts) {
    const value = String(part)
    payload.push(`$${Buffer.byteLength(value)}`)
    payload.push(value)
  }
  return `${payload.join('\r\n')}\r\n`
}

function parseBulkString(response) {
  if (!response || response.startsWith('$-1')) return null
  const segments = response.split('\r\n')
  return segments[1] || null
}

async function redisGet(key) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: REDIS_HOST, port: REDIS_PORT }, () => {
      client.write(buildRedisCommand(['AUTH', REDIS_PASSWORD]))
      client.write(buildRedisCommand(['GET', key]))
    })

    let buffer = ''
    let authDone = false

    client.on('data', (chunk) => {
      buffer += chunk.toString()
      if (!authDone && buffer.includes('+OK\r\n')) {
        authDone = true
        buffer = buffer.replace('+OK\r\n', '')
      }
      if (authDone && (buffer.includes('\r\n') || buffer.startsWith('$-1'))) {
        client.end()
        resolve(parseBulkString(buffer))
      }
    })

    client.on('error', reject)
  })
}

async function waitForEmailCode(tenantId, email, scene = 'REGISTER') {
  const key = `email:verify:code:${scene}:${tenantId}:${email.trim().toLowerCase()}`
  const deadline = Date.now() + 15000
  while (Date.now() < deadline) {
    const code = await redisGet(key)
    if (code) return code
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for email code in Redis key ${key}`)
}

async function listEmailTasks(request, token, tenantId) {
  const res = await request.get(`${API_BASE}/api/email-center/tasks`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': String(tenantId)
    }
  })
  const body = await res.json()
  expect(body.code).toBe(200)
  return body.data || []
}

async function listEmailRecipients(request, token, tenantId, taskId) {
  const res = await request.get(`${API_BASE}/api/email-center/tasks/${taskId}/recipients`, {
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Tenant-Id': String(tenantId)
    }
  })
  const body = await res.json()
  expect(body.code).toBe(200)
  return body.data || []
}

async function findRegisterReview(request, token, tenantId, email) {
  const deadline = Date.now() + 20000
  while (Date.now() < deadline) {
    const res = await request.get(`${API_BASE}/api/admission/register-review/list`, {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Tenant-Id': String(tenantId)
      }
    })
    const body = await res.json()
    expect(body.code).toBe(200)
    const match = (body.data || []).find((item) => String(item.email || '').toLowerCase() === email.toLowerCase())
    if (match) {
      return match
    }
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
  throw new Error(`Timed out waiting for register review request for ${email}`)
}

async function waitForApprovalResultEmailTask(request, token, tenantId, beforeTaskIds, targetEmail) {
  const deadline = Date.now() + 20000
  const normalizedEmail = targetEmail.trim().toLowerCase()

  while (Date.now() < deadline) {
    const tasks = await listEmailTasks(request, token, tenantId)
    for (const task of tasks) {
      if (beforeTaskIds.has(task.id)) {
        continue
      }
      const recipients = await listEmailRecipients(request, token, tenantId, task.id)
      if (recipients.some((item) => String(item.recipientEmail || '').toLowerCase() === normalizedEmail)) {
        return { task, recipients }
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }

  throw new Error(`Timed out waiting for approval result email task for ${targetEmail}`)
}

test('register review approval should queue result email to the applicant', async ({ page, request }) => {
  test.setTimeout(120000)
  const adminLogin = await ensureStableAdmissionConfig(request, API_BASE, '1')
  const beforeTasks = await listEmailTasks(request, adminLogin.token, 1)
  const beforeTaskIds = new Set(beforeTasks.map((item) => item.id))

  const seed = Date.now()
  const username = `review_result_${seed}`
  const email = `review_result_${seed}@example.com`
  const password = 'Passw0rd!'
  const studentId = `2026${String(seed).slice(-6)}`

  try {
    await page.goto(`${BASE_URL}/register`, { waitUntil: 'networkidle' })
    await page.locator('.el-select').first().click()
    await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()
    const formItems = page.locator('.register-card .el-form-item')

    await formItems.nth(1).locator('input').fill(username)
    await formItems.nth(2).locator('input').fill(password)
    await formItems.nth(3).locator('input').fill(password)
    await formItems.nth(8).locator('input').fill(email)
    await fillStableRegisterFields(page, seed)

    await formItems.nth(12).getByRole('button').click()
    const code = await waitForEmailCode(1, email, 'REGISTER')
    await formItems.nth(12).locator('input').fill(code)

    const registerResponse = page.waitForResponse((response) =>
      response.url().includes('/auth/register') && response.request().method() === 'POST'
    )
    await formItems.nth(13).locator('button').click()
    const registerBody = await (await registerResponse).json()
    expect(registerBody.code).toBe(200)
    await expect(page).toHaveURL(/\/login/)

    const reviewRequest = await findRegisterReview(request, adminLogin.token, 1, email)
    const approveRes = await request.post(`${API_BASE}/api/admission/register-review/review`, {
      headers: {
        Authorization: `Bearer ${adminLogin.token}`,
        'X-Tenant-Id': '1'
      },
      data: {
        id: reviewRequest.id,
        reviewStatus: 'APPROVED',
        reviewComment: 'playwright approval result email smoke',
        reviewerId: 1
      }
    })
    const approveBody = await approveRes.json()
    expect(approveBody.code).toBe(200)

    const { task, recipients } = await waitForApprovalResultEmailTask(
      request,
      adminLogin.token,
      1,
      beforeTaskIds,
      email
    )

    expect(String(task.subject || '').trim().length).toBeGreaterThan(0)
    expect(recipients.some((item) => String(item.recipientEmail || '').toLowerCase() === email.toLowerCase())).toBeTruthy()
  } finally {
    await cleanupRegisterFixture({
      tenantId: 1,
      username,
      email,
      studentId
    }).catch(() => {})
  }
})
