// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const { TENANT1_MEMBER, TENANT2_ADMIN } = require('./helpers/test-users.cjs')
const { execRemoteMysql } = require('./helpers/remote-mysql.cjs')

const BASE_URL = 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const REDIS_HOST = '127.0.0.1'
const REDIS_PORT = 6379
const REDIS_PASSWORD = 'change-me'
const TARGET_TENANT_ID = '2'

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
  const deadline = Date.now() + 15000
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

async function touchRecentLogin(username) {
  await execRemoteMysql(
    `
UPDATE rk_auth.login_record lr
JOIN rk_auth.user au ON au.id = lr.user_id
SET lr.login_time = NOW()
WHERE au.username = '${String(username).replace(/'/g, "''")}';
`.trim()
  )
}

async function getTenantById(request, tenantId) {
  const res = await request.get(`${API_BASE}/tenants/list`)
  const body = await res.json()
  if (body.code !== 200 || !Array.isArray(body.data)) {
    throw new Error(`Failed to load tenant list: ${JSON.stringify(body)}`)
  }
  const tenant = body.data.find((item) => String(item.id) === String(tenantId))
  if (!tenant) {
    throw new Error(`Tenant ${tenantId} not found in tenant list`)
  }
  return tenant
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

function buildApprovalQuery({ studentId, email }) {
  return `
SELECT
  jr.review_status,
  jr.auth_user_id,
  u.username,
  u.email,
  u.student_id,
  cm.position,
  cm.is_deleted
FROM rk_user.join_requests jr
LEFT JOIN rk_user.rk_user u
  ON u.tenant_id = jr.tenant_id
  AND u.auth_user_id = jr.auth_user_id
LEFT JOIN rk_user.club_members cm
  ON cm.tenant_id = jr.tenant_id
  AND cm.student_id = jr.student_id
WHERE jr.tenant_id = ${TARGET_TENANT_ID}
  AND jr.student_id = '${studentId}'
  AND jr.email = '${email}'
ORDER BY jr.id DESC
LIMIT 1;
`.trim()
}

test('logged-in member should submit cross-tenant join and tenant2 admin approval should sync user/member records', async ({ browser, request }) => {
  test.setTimeout(120000)

  await touchRecentLogin(TENANT1_MEMBER.username)
  await touchRecentLogin(TENANT2_ADMIN.username)

  const memberLogin = await loginApi(request, TENANT1_MEMBER)
  const targetTenant = await getTenantById(request, TARGET_TENANT_ID)
  const adminLogin = await loginApi(request, TENANT2_ADMIN)

  const seed = Date.now()
  const applicantName = `璺ㄧ鎴峰凡鐧诲綍鐢宠${seed}`
  const studentId = `CT${seed}`
  const email = `cross_join_${seed}@example.com`
  const phone = `138${String(seed).slice(-8)}`

  const sendCodeRes = await request.post(`${API_BASE}/api/email-verification/send`, {
    data: {
      email,
      tenantId: Number(targetTenant.id),
      scene: 'JOIN'
    }
  })
  const sendCodeBody = await sendCodeRes.json()
  expect(sendCodeBody.code).toBe(200)

  const emailCode = await waitForEmailCode(Number(targetTenant.id), email, 'JOIN')
  const submitRes = await request.post(`${API_BASE}/api/admission/submit`, {
    headers: {
      Authorization: `Bearer ${memberLogin.token}`,
      'X-Tenant-Id': String(targetTenant.id),
      'Content-Type': 'application/json'
    },
    data: {
      tenantId: Number(targetTenant.id),
      name: applicantName,
      studentId,
      username: TENANT1_MEMBER.username,
      sourceAuthUserId: Number(memberLogin.userId),
      sourceTenantId: Number(TENANT1_MEMBER.organizationId),
      sourceRoleName: '鎴愬憳',
      major: '杞欢宸ョ▼',
      grade: '1',
      phone,
      email,
      emailCode,
      interest: 'frontend',
      experience: '璺ㄧ鎴峰凡鐧诲綍鎴愬憳鐢宠鍔犲叆鐩爣绉熸埛',
      formPayload: {
        name: applicantName,
        studentId,
        college: '璁＄畻鏈哄闄?,
        major: '杞欢宸ョ▼',
        grade: '1',
        phone,
        email,
        interests: ['frontend'],
        intro: '璺ㄧ鎴峰凡鐧诲綍鎴愬憳鐢宠鍔犲叆鐩爣绉熸埛'
      }
    }
  })
  const submitBody = await submitRes.json()
  expect(submitBody.code).toBe(200)

  const adminContext = await browser.newContext()
  const adminPage = await adminContext.newPage()
  await applyLogin(adminPage, adminLogin)
  await adminPage.goto(`${BASE_URL}/admin/club/applications`, { waitUntil: 'networkidle' })
  await expect(adminPage.locator('.admin-page, main').first()).toBeVisible()

  const listRes = await request.get(`${API_BASE}/api/admission/list`, {
    headers: {
      Authorization: `Bearer ${adminLogin.token}`,
      'X-Tenant-Id': String(targetTenant.id)
    }
  })
  const listBody = await listRes.json()
  expect(listBody.code).toBe(200)
  const application = (listBody.data || []).find((item) => item.studentId === studentId && item.email === email)
  expect(application).toBeTruthy()

  const reviewRes = await request.post(`${API_BASE}/api/admission/review`, {
    headers: {
      Authorization: `Bearer ${adminLogin.token}`,
      'X-Tenant-Id': String(targetTenant.id),
      'Content-Type': 'application/json'
    },
    data: {
      id: application.id,
      reviewStatus: '閫氳繃',
      reviewComment: '鑷姩鍖栧鎵归€氳繃',
      reviewerId: adminLogin.userId
    }
  })
  const reviewBody = await reviewRes.json()
  expect(reviewBody.code).toBe(200)

  const scopedUsersRes = await request.get(`${API_BASE}/users/page/scoped?pageNo=1&size=20&username=${encodeURIComponent(TENANT1_MEMBER.username)}`, {
    headers: {
      Authorization: `Bearer ${adminLogin.token}`,
      'X-Tenant-Id': String(targetTenant.id)
    }
  })
  const scopedUsersBody = await scopedUsersRes.json()
  expect(scopedUsersBody.code).toBe(200)
  const syncedUser = (scopedUsersBody.data?.records || []).find((item) => item.studentId === studentId && item.email === email)
  expect(syncedUser).toBeTruthy()

  const mysqlResult = await execRemoteMysql(buildApprovalQuery({ studentId, email }), { database: 'mysql' })
  const normalized = mysqlResult.stdout.trim().replace(/\r/g, '')
  expect(normalized).toContain('閫氳繃')
  expect(normalized).toContain('member_a')
  expect(normalized).toContain(email)
  expect(normalized).toContain(studentId)
  expect(normalized).toContain('鎴愬憳')

  await adminContext.close()
})