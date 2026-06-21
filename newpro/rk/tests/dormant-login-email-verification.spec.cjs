// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ADMIN,
  ensureStableAdmissionConfig,
  loginApi
} = require('./helpers/admission-form-helpers.cjs')
const { cleanupRegisterFixture } = require('./helpers/live-fixture-cleanup.cjs')
const { execRemoteMysql } = require('./helpers/remote-mysql.cjs')

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

async function redisRaw(parts) {
  return new Promise((resolve, reject) => {
    const client = net.createConnection({ host: REDIS_HOST, port: REDIS_PORT }, () => {
      client.write(buildRedisCommand(['AUTH', REDIS_PASSWORD]))
      client.write(buildRedisCommand(parts))
    })

    let buffer = ''
    let authDone = false

    client.on('data', (chunk) => {
      buffer += chunk.toString()
      if (!authDone && buffer.includes('+OK\r\n')) {
        authDone = true
        buffer = buffer.replace('+OK\r\n', '')
      }
      if (authDone && buffer.includes('\r\n')) {
        client.end()
        resolve(buffer)
      }
    })

    client.on('error', reject)
  })
}

async function redisGet(key) {
  return parseBulkString(await redisRaw(['GET', key]))
}

async function waitForEmailCode(tenantId, email, scene) {
  const key = `email:verify:code:${scene}:${tenantId}:${email.trim().toLowerCase()}`
  const deadline = Date.now() + 15000
  while (Date.now() < deadline) {
    const code = await redisGet(key)
    if (code) return code
    await new Promise((resolve) => setTimeout(resolve, 500))
  }
  throw new Error(`Timed out waiting for email code in Redis key ${key}`)
}

async function setLatestLoginOlderThanDays(userId, days) {
  await execRemoteMysql(
    `UPDATE rk_auth.login_record SET login_time = DATE_SUB(NOW(), INTERVAL ${Number(days)} DAY) WHERE user_id = ${Number(userId)}`
  )
}

test('dormant account should require email verification code after three days without login', async ({ request }) => {
  test.setTimeout(120000)

  const seed = Date.now()
  const username = `dormant_${seed}`
  const email = `dormant_${seed}@example.com`
  const password = 'Passw0rd!'
  const studentId = `2026${String(seed).slice(-6)}`

  const adminLogin = await loginApi(request, API_BASE, ADMIN)
  const headers = {
    Authorization: `Bearer ${adminLogin.token}`,
    'X-Tenant-Id': adminLogin.organizationId || '1'
  }

  const currentRes = await request.get(`${API_BASE}/api/workflow/config/current`, { headers })
  const currentBody = await currentRes.json()
  expect(currentBody.code).toBe(200)
  const originalConfig = JSON.parse(JSON.stringify(currentBody.data || {}))

  try {
    await ensureStableAdmissionConfig(request, API_BASE, '1')

    const openConfig = JSON.parse(JSON.stringify(originalConfig))
    openConfig.registration = {
      ...(openConfig.registration || {}),
      openRegistration: true,
      requireApproval: false,
      notifyAdmins: false,
      notifyApplicantOnFailure: false,
      notifyOnSuccess: true
    }

    const saveWorkflowRes = await request.put(`${API_BASE}/api/workflow/config/current`, {
      headers,
      data: openConfig
    })
    const saveWorkflowBody = await saveWorkflowRes.json()
    expect(saveWorkflowBody.code).toBe(200)

    const sendRegisterCodeRes = await request.post(`${API_BASE}/api/email-verification/send`, {
      data: {
        email,
        tenantId: 1,
        scene: 'REGISTER'
      }
    })
    const sendRegisterCodeBody = await sendRegisterCodeRes.json()
    expect(sendRegisterCodeBody.code).toBe(200)

    const registerCode = await waitForEmailCode(1, email, 'REGISTER')
    const registerRes = await request.post(`${API_BASE}/auth/register`, {
      data: {
        username,
        password,
        email,
        organizationId: '1',
        name: `浼戠湢娴嬭瘯${seed}`,
        emailCode: registerCode,
        formPayload: {
          name: `浼戠湢娴嬭瘯${seed}`,
          studentId,
          major: '杞欢宸ョ▼',
          grade: '1',
          interests: ['frontend'],
          intro: 'dormant login smoke'
        }
      }
    })
    const registerBody = await registerRes.json()
    expect(registerBody.code).toBe(200)

    const firstLoginRes = await request.post(`${API_BASE}/auth/login`, {
      data: {
        username,
        password,
        organizationId: '1'
      }
    })
    const firstLoginBody = await firstLoginRes.json()
    expect(firstLoginBody.code).toBe(200)
    expect(firstLoginBody.data?.userId).toBeTruthy()

    await setLatestLoginOlderThanDays(Number(firstLoginBody.data.userId), 4)

    const dormantLoginRes = await request.post(`${API_BASE}/auth/login`, {
      data: {
        username,
        password,
        organizationId: '1'
      }
    })
    const dormantLoginBody = await dormantLoginRes.json()
    expect(dormantLoginBody.code).toBe(400)
    expect(String(dormantLoginBody.msg || '')).toContain('Dormant login requires email verification')

    const sendLoginCodeRes = await request.post(`${API_BASE}/api/email-verification/send`, {
      data: {
        email,
        tenantId: 1,
        scene: 'LOGIN'
      }
    })
    const sendLoginCodeBody = await sendLoginCodeRes.json()
    expect(sendLoginCodeBody.code).toBe(200)

    const loginCode = await waitForEmailCode(1, email, 'LOGIN')
    const verifiedLoginRes = await request.post(`${API_BASE}/auth/login`, {
      data: {
        username,
        password,
        organizationId: '1',
        email,
        emailCode: loginCode
      }
    })
    const verifiedLoginBody = await verifiedLoginRes.json()
    expect(verifiedLoginBody.code).toBe(200)
    expect(verifiedLoginBody.data?.token).toBeTruthy()
  } finally {
    await request.put(`${API_BASE}/api/workflow/config/current`, {
      headers,
      data: originalConfig
    }).catch(() => {})

    await cleanupRegisterFixture({
      tenantId: 1,
      username,
      email,
      studentId
    }).catch(() => {})
  }
})
