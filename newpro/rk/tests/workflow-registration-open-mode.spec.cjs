// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ADMIN,
  ensureStableAdmissionConfig,
  loginApi
} = require('./helpers/admission-form-helpers.cjs')
const { cleanupRegisterFixture } = require('./helpers/live-fixture-cleanup.cjs')

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

test('registration should allow immediate login when approval workflow is disabled', async ({ request }) => {
  test.setTimeout(120000)

  const seed = Date.now()
  const username = `open_reg_${seed}`
  const email = `open_reg_${seed}@example.com`
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
    expect(saveWorkflowBody.data?.registration?.requireApproval).toBe(false)

    const sendCodeRes = await request.post(`${API_BASE}/api/email-verification/send`, {
      data: {
        email,
        tenantId: 1,
        scene: 'REGISTER'
      }
    })
    const sendCodeBody = await sendCodeRes.json()
    expect(sendCodeBody.code).toBe(200)

    const emailCode = await waitForEmailCode(1, email, 'REGISTER')
    const registerRes = await request.post(`${API_BASE}/auth/register`, {
      data: {
        username,
        password,
        email,
        organizationId: '1',
        name: `寮€鏀炬敞鍐?{seed}`,
        emailCode,
        formPayload: {
          name: `寮€鏀炬敞鍐?{seed}`,
          studentId,
          major: '杞欢宸ョ▼',
          grade: '1',
          interests: ['frontend'],
          intro: 'workflow open registration smoke'
        }
      }
    })
    const registerBody = await registerRes.json()
    expect(registerBody.code).toBe(200)

    const immediateLoginRes = await request.post(`${API_BASE}/auth/login`, {
      data: {
        username,
        password,
        organizationId: '1'
      }
    })
    const immediateLoginBody = await immediateLoginRes.json()
    expect(immediateLoginBody.code).toBe(200)
    expect(immediateLoginBody.data?.token).toBeTruthy()
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
