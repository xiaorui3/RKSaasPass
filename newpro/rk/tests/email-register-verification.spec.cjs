// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ensureStableAdmissionConfig,
  fillStableRegisterFields
} = require('./helpers/admission-form-helpers.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
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

test('register should require and accept email verification code', async ({ page }) => {
  const seed = Date.now()
  const username = `mail_user_${seed}`
  const password = 'Passw0rd!'
  const email = `mail_${seed}@example.com`
  await ensureStableAdmissionConfig(page.request, API_BASE, '1')

  const tenantRes = await page.request.get(`${API_BASE}/tenants/list`)
  const tenantBody = await tenantRes.json()
  const tenant = (tenantBody.data || [])[0]
  expect(tenant).toBeTruthy()
  const tenantId = String(tenant.id)

  await page.goto(`${BASE_URL}/register`, { waitUntil: 'domcontentloaded' })

  const tenantSelect = page.locator('.el-select').first()
  await tenantSelect.click()
  const tenantOption = page.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: tenant.tenantName }).first()
  await tenantOption.click()

  await page.getByLabel(/鐢ㄦ埛/).fill(username)
  await page.getByLabel(/瀵嗙爜/).first().fill(password)
  await page.getByLabel(/纭瀵嗙爜/).fill(password)
  await page.getByLabel(/閭/).fill(email)
  await fillStableRegisterFields(page, seed)

  await page.getByRole('button', { name: /鍙戦€侀獙璇佺爜/ }).click()
  await expect(page.locator('.el-message').last()).toContainText(/楠岃瘉鐮佸凡鍙?)

  const code = await waitForEmailCode(Number(tenantId), email)
  await page.getByLabel(/楠岃瘉/).fill(code)

  const registerResponse = page.waitForResponse((response) =>
    response.url().includes('/auth/register') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '娉ㄥ唽' }).click()
  const registerBody = await (await registerResponse).json()
  expect(registerBody.code).toBe(200)
  expect(String(registerBody.msg || '')).toMatch(/鎿嶄綔鎴愬姛|娉ㄥ唽鎴愬姛|鐢宠宸叉彁浜绛夊緟瀹℃牳/)

  const currentUrl = page.url()
  if (!currentUrl.includes('/login')) {
    await expect(page.locator('.el-message').last()).toContainText(/鐢宠宸叉彁浜绛夊緟瀹℃牳|娉ㄥ唽鎴愬姛/)
  }
})
