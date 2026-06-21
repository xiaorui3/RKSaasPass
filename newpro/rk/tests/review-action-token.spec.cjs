// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const {
  ADMIN,
  ensureStableAdmissionConfig,
  fillStableRegisterFields
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

function parseArrayStrings(response) {
  if (!response || response.startsWith('*0')) return []
  const segments = response.split('\r\n').filter(Boolean)
  const result = []
  for (let i = 2; i < segments.length; i += 2) {
    result.push(segments[i])
  }
  return result
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

async function redisKeys(pattern) {
  return parseArrayStrings(await redisRaw(['KEYS', pattern]))
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

async function waitForReviewToken(beforeKeys) {
  const known = new Set(beforeKeys)
  const deadline = Date.now() + 20000

  while (Date.now() < deadline) {
    const keys = await redisKeys('admission:review:token:*')
    const newKeys = keys.filter((key) => !known.has(key))
    for (const key of newKeys) {
      const raw = await redisGet(key)
      if (!raw) continue
      const dto = JSON.parse(raw)
      if (dto.targetType === 'REGISTER' && dto.action === 'APPROVE') {
        return dto.token
      }
    }
    await new Promise((resolve) => setTimeout(resolve, 500))
  }

  throw new Error('Timed out waiting for register review token')
}

test('review action page should redirect to login and allow admin to approve register request', async ({ page, request }) => {
  test.setTimeout(120000)
  await ensureStableAdmissionConfig(request, API_BASE, '1')

  const beforeKeys = await redisKeys('admission:review:token:*')
  const seed = Date.now()
  const username = `review_token_${seed}`
  const email = `review_token_${seed}@example.com`
  const password = 'Passw0rd!'

  await page.goto(`${BASE_URL}/register`, { waitUntil: 'networkidle' })
  await page.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()
  await page.getByPlaceholder('璇疯緭鍏ョ敤鎴峰悕').fill(username)
  await page.locator('input[placeholder="璇疯緭鍏ュ瘑鐮?]').first().fill(password)
  await page.getByPlaceholder('璇峰啀娆¤緭鍏ュ瘑鐮?).fill(password)
  await page.getByPlaceholder('璇疯緭鍏ュ父鐢ㄩ偖绠?).fill(email)
  await fillStableRegisterFields(page, seed)

  await page.getByRole('button', { name: '鍙戦€侀獙璇佺爜' }).click()
  const code = await waitForEmailCode(1, email, 'REGISTER')
  await page.getByPlaceholder('璇疯緭鍏ラ偖绠遍獙璇佺爜').fill(code)
  const registerResponse = page.waitForResponse((response) =>
    response.url().includes('/auth/register') && response.request().method() === 'POST'
  )
  await page.getByRole('button', { name: '娉ㄥ唽' }).click()
  const registerBody = await (await registerResponse).json()
  expect(registerBody.code).toBe(200)
  await expect(page).toHaveURL(/\/login/, { timeout: 15000 })

  const reviewToken = await waitForReviewToken(beforeKeys)
  await page.goto(`${BASE_URL}/review/admission?token=${reviewToken}`, { waitUntil: 'networkidle' })

  await expect(page).toHaveURL(/\/login\?redirect=/)
  const adminLogin = await request.post(`${API_BASE}/auth/login`, { data: ADMIN })
  const adminLoginBody = await adminLogin.json()
  expect(adminLoginBody.code).toBe(200)
  expect(adminLoginBody.data?.token).toBeTruthy()

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
  }, adminLoginBody.data)

  await page.goto(`${BASE_URL}/review/admission?token=${reviewToken}`, { waitUntil: 'networkidle' })
  await page.waitForURL((url) => url.pathname === '/review/admission' && url.searchParams.get('token') === reviewToken)
  await expect(page.locator('.review-card')).toBeVisible()

  const executeResponse = page.waitForResponse((response) =>
    response.url().includes('/api/admission/review-action/execute') && response.request().method() === 'POST'
  )
  await page.locator('.review-actions .el-button--primary').click()
  await page.locator('.el-message-box:visible .el-button--primary').click()
  const executeBody = await (await executeResponse).json()
  expect(executeBody.code).toBe(200)

  const approvedLogin = await request.post(`${API_BASE}/auth/login`, {
    data: { username, password, organizationId: '1' }
  })
  const approvedLoginBody = await approvedLogin.json()
  expect(approvedLoginBody.code).toBe(200)
  expect(approvedLoginBody.data?.token).toBeTruthy()
})
