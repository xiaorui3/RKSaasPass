// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ADMIN,
  ensureStableAdmissionConfig,
  fillStableRegisterFields,
  loginApi
} = require('./helpers/admission-form-helpers.cjs')

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

test('anonymous register should create tenant admin notification task', async ({ page, request }) => {
  test.setTimeout(120000)
  await ensureStableAdmissionConfig(request, API_BASE, '1')

  const tenantRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantBody = await tenantRes.json()
  expect(tenantBody.code).toBe(200)
  const tenant = tenantBody.data.find((item) => String(item.id) === '1')
  expect(tenant).toBeTruthy()

  const adminLogin = await loginApi(request, API_BASE, ADMIN)
  const beforeTasks = await listEmailTasks(request, adminLogin.token, tenant.id)
  const beforeTopId = beforeTasks[0]?.id || null

  const seed = Date.now()
  const username = `register_notify_${seed}`
  const email = `register_notify_${seed}@example.com`
  const password = 'Passw0rd!'

  await page.goto(`${BASE_URL}/register`, { waitUntil: 'networkidle' })
  await page.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant.tenantName }).first().click()
  await page.getByLabel(/鐢ㄦ埛/).fill(username)
  await page.getByLabel(/瀵嗙爜/).first().fill(password)
  await page.getByLabel(/纭瀵嗙爜/).fill(password)
  await page.getByLabel(/閭/).fill(email)
  await fillStableRegisterFields(page, seed)

  await page.getByRole('button', { name: /鍙戦€侀獙璇佺爜/ }).click()
  const code = await waitForEmailCode(Number(tenant.id), email, 'REGISTER')
  await page.getByLabel(/楠岃瘉/).fill(code)
  const registerResponse = page.waitForResponse((response) =>
    response.url().includes('/auth/register') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '娉ㄥ唽' }).click()
  const registerBody = await (await registerResponse).json()
  expect(registerBody.code).toBe(200)
  await expect(page).toHaveURL(/\/login/)

  const deadline = Date.now() + 20000
  let afterTasks = beforeTasks
  while (Date.now() < deadline) {
    afterTasks = await listEmailTasks(request, adminLogin.token, tenant.id)
    if ((afterTasks[0]?.id || null) !== beforeTopId) {
      break
    }
    await page.waitForTimeout(1000)
  }

  expect(afterTasks[0]?.id || null).not.toBe(beforeTopId)
  expect(afterTasks[0]?.subject || '').toContain(tenant.tenantName)
})
