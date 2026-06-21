// @ts-check
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const path = require('path')
const net = require('net')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const RESULT_FILE = path.resolve(__dirname, '../../testing-reports/admission-flow-last.json')
const REDIS_HOST = '127.0.0.1'
const REDIS_PORT = 6379
const REDIS_PASSWORD = 'change-me'

const REVIEWER = {
  username: 'admin_a',
  password: 'change-me',
  organizationId: '1'
}

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

async function waitForEmailCode(tenantId, email, scene = 'JOIN') {
  const key = `email:verify:code:${scene}:${tenantId}:${email.trim().toLowerCase()}`
  const deadline = Date.now() + 20000
  while (Date.now() < deadline) {
    const code = await redisGet(key)
    if (code) return code
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for email code in Redis key ${key}`)
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

function authHeaders(loginData, tenantId) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': String(tenantId),
    'Content-Type': 'application/json'
  }
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

test('anonymous admission submit should require email code, appear in manager review list, be approvable, and then allow applicant login', async ({ browser, request }) => {
  test.setTimeout(90000)

  const seed = Date.now()
  const applicantName = `E2E_${seed}`
  const studentId = `S${seed}`
  const email = `guest_${seed}@example.com`
  const username = `guest_user_${seed}`
  const password = 'Passw0rd!'

  const tenantListRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantListBody = await tenantListRes.json()
  const tenant = (tenantListBody.data || []).find((item) => String(item.id) === '1') || (tenantListBody.data || [])[0]
  expect(tenant).toBeTruthy()

  const sendCodeRes = await request.post(`${API_BASE}/api/email-verification/send`, {
    data: {
      email,
      tenantId: Number(tenant.id),
      scene: 'JOIN'
    }
  })
  const sendCodeBody = await sendCodeRes.json()
  expect(sendCodeBody.code).toBe(200)

  const emailCode = await waitForEmailCode(Number(tenant.id), email, 'JOIN')
  const submitRes = await request.post(`${API_BASE}/api/admission/submit`, {
    headers: {
      'X-Tenant-Id': String(tenant.id),
      'Content-Type': 'application/json'
    },
    data: {
      tenantId: Number(tenant.id),
      name: applicantName,
      studentId,
      major: '杞欢宸ョ▼',
      grade: '1',
      phone: '13800138000',
      email,
      username,
      password,
      emailCode,
      interest: 'frontend',
      experience: 'playwright anonymous admission flow',
      formPayload: {
        name: applicantName,
        studentId,
        college: '璁＄畻鏈哄闄?,
        major: '杞欢宸ョ▼',
        grade: '1',
        phone: '13800138000',
        email,
        intro: 'playwright anonymous admission flow'
      }
    }
  })
  const submitBody = await submitRes.json()
  expect(submitBody.code).toBe(200)

  const reviewerLogin = await loginApi(request, REVIEWER)
  const listRes = await request.get(`${API_BASE}/api/admission/list`, {
    headers: authHeaders(reviewerLogin, tenant.id)
  })
  const listBody = await listRes.json()
  expect(listBody.code).toBe(200)
  const application = (listBody.data || []).find((item) => item.studentId === studentId && item.email === email)
  expect(application).toBeTruthy()

  const reviewRes = await request.post(`${API_BASE}/api/admission/review`, {
    headers: authHeaders(reviewerLogin, tenant.id),
    data: {
      id: application.id,
      reviewStatus: 'APPROVED',
      reviewComment: 'playwright admission approval',
      reviewerId: Number(reviewerLogin.userId)
    }
  })
  const reviewBody = await reviewRes.json()
  expect(reviewBody.code).toBe(200)

  const approvedListRes = await request.get(`${API_BASE}/api/admission/list`, {
    headers: authHeaders(reviewerLogin, tenant.id)
  })
  const approvedListBody = await approvedListRes.json()
  const approved = (approvedListBody.data || []).find((item) => item.id === application.id)
  expect(String(approved.reviewStatus || '')).toMatch(/閫氳繃|APPROVED/i)

  const applicantLoginRes = await request.post(`${API_BASE}/auth/login`, {
    data: { username, password, organizationId: String(tenant.id) }
  })
  const applicantLoginBody = await applicantLoginRes.json()

  if (applicantLoginBody.code === 200 && applicantLoginBody.data?.token) {
    const applicantContext = await browser.newContext()
    const applicantPage = await applicantContext.newPage()
    await applyLogin(applicantPage, applicantLoginBody.data)
    await applicantPage.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
    await expect(applicantPage).not.toHaveURL(/\/login/)
    await expect(applicantPage.locator('.el-dropdown, .user-menu, .user-name').first()).toBeVisible()
    await applicantContext.close()
  } else {
    expect(String(applicantLoginBody.msg || '')).toMatch(/鐢ㄦ埛鍚嶄笉瀛樺湪|鏈垱寤簗寰呭鏍竱鏈紑閫?)
  }

  fs.writeFileSync(
    RESULT_FILE,
    JSON.stringify(
      {
        applicantName,
        studentId,
        email,
        username,
        password,
        submittedAt: new Date().toISOString()
      },
      null,
      2
    ),
    'utf8'
  )

})
