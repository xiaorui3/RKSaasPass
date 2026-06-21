// @ts-check
const { test, expect } = require('@playwright/test')
const net = require('net')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const REDIS_HOST = '127.0.0.1'
const REDIS_PORT = 6379
const REDIS_PASSWORD = 'change-me'

const MEMBER = { username: 'member_a', password: '123456', organizationId: '1' }

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

async function querySwitchableTenants(request, loginData) {
  const res = await request.get(`${API_BASE}/auth/switchable-tenants`, {
    headers: {
      Authorization: `Bearer ${loginData.token}`
    }
  })
  const body = await res.json()
  if (body.code !== 200) {
    throw new Error(`switchable-tenants failed: ${JSON.stringify(body)}`)
  }
  return body.data || []
}

async function waitForSwitchableTenants(request, loginData, expectedCount = 2, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const tenants = await querySwitchableTenants(request, loginData)
    if (tenants.length >= expectedCount) {
      return tenants
    }
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
  return querySwitchableTenants(request, loginData)
}

async function ensureLinkedTenantAccount(browser, request, memberLogin) {
  const current = await querySwitchableTenants(request, memberLogin)
  if (current.length > 1) {
    return current
  }

  const tenantListRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantListBody = await tenantListRes.json()
  const targetTenant = (tenantListBody.data || []).find((tenant) => String(tenant.id) !== String(memberLogin.organizationId))
  expect(targetTenant).toBeTruthy()

  const seed = Date.now()
  const applicantName = `缁熶竴鍒囨崲琛ラ摼${seed}`
  const studentId = `SW${seed}`
  const username = `switch_seed_${seed}`
  const password = 'Passw0rd!'
  const email = `switch-seed-${seed}@example.com`

  const memberContext = await browser.newContext()
  const memberPage = await memberContext.newPage()
  await applyLogin(memberPage, memberLogin)
  await memberPage.goto(`${BASE_URL}/join`, { waitUntil: 'networkidle' })

  await memberPage.locator('.el-select').nth(0).click()
  await memberPage.locator('.el-select-dropdown:visible .el-select-dropdown__item').filter({ hasText: targetTenant.tenantName }).first().click()

  await memberPage.getByPlaceholder('璇疯緭鍏ュ').fill(applicantName)
  await memberPage.getByPlaceholder('璇疯緭鍏ュ').fill(studentId)
  await memberPage.getByPlaceholder('璇疯緭鍏ュ鏍搁€氳繃鍚庣敤浜庣櫥褰曠殑璐﹀彿').fill(username)
  await memberPage.getByPlaceholder('璇疯緭鍏ョ櫥褰曞瘑').fill(password)
  await memberPage.getByPlaceholder('璇峰啀娆¤緭鍏ョ櫥褰曞瘑').fill(password)
  await memberPage.getByPlaceholder('璇疯緭鍏ヤ笓').fill('杞欢宸ョ▼')

  await memberPage.locator('.el-select').nth(1).click()
  await memberPage.locator('.el-select-dropdown:visible .el-select-dropdown__item').first().click()

  await memberPage.getByPlaceholder('璇疯緭鍏ヨ仈绯荤數').fill('13800138111')
  await memberPage.getByPlaceholder('璇疯緭鍏ュ父鐢ㄩ偖').fill(email)
  await memberPage.getByRole('button', { name: /鍙戦€侀獙璇佺爜/ }).click()

  const emailCode = await waitForEmailCode(Number(targetTenant.id), email, 'JOIN')
  await memberPage.getByPlaceholder('璇疯緭鍏ラ偖绠遍獙璇佺爜').fill(emailCode)

  await memberPage.locator('.el-checkbox').first().click()
  await memberPage.locator('textarea').fill('涓虹粺涓€璐﹀彿鍒囨崲鍔熻兘琛ラ綈璺ㄧ鎴峰鎵瑰叧鑱?)
  await memberPage.getByRole('button').filter({ hasText: /鎻愪氦鐢宠/ }).click()
  await expect(memberPage.locator('.el-message').last()).toContainText(/鎴愬姛|绛夊緟璐熻矗浜哄/)

  const adminLogin = await loginApi(request, SUPER_ADMIN)
  const adminContext = await browser.newContext()
  const adminPage = await adminContext.newPage()
  await applyLogin(adminPage, {
    ...adminLogin,
    organizationId: String(targetTenant.id)
  })
  await adminPage.goto(`${BASE_URL}/admin/club/applications`, { waitUntil: 'networkidle' })
  await adminPage.waitForTimeout(1500)

  const row = adminPage.locator('.el-table__row').filter({ hasText: applicantName }).first()
  await expect(row).toBeVisible()
  await row.getByText('閫氳繃').click()
  const confirmButton = adminPage.getByRole('button').filter({ hasText: /^纭畾$/ }).last()
  if (await confirmButton.isVisible().catch(() => false)) {
    await confirmButton.click()
  }
  await adminPage.waitForTimeout(2000)

  await memberContext.close()
  await adminContext.close()

  return waitForSwitchableTenants(request, memberLogin)
}

test('linked account should be able to switch tenant from user menu after cross-tenant approval', async ({ browser, page, request }) => {
  test.setTimeout(120000)

  const login = await loginApi(request, MEMBER)
  const switchableTenants = await ensureLinkedTenantAccount(browser, request, login)
  expect(switchableTenants.length).toBeGreaterThan(1)
  const tenantsRes = await request.get(`${API_BASE}/tenants/list`)
  const tenantsBody = await tenantsRes.json()
  const tenantNameMap = new Map((tenantsBody.data || []).map((item) => [String(item.id), item.tenantName]))
  const targetTenantName = tenantNameMap.get('2') || '绉熸埛 2'

  await applyLogin(page, login)

  const switchableTenantsResponse = page.waitForResponse((response) =>
    response.url().includes('/auth/switchable-tenants') && response.status() === 200
  )

  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
  await switchableTenantsResponse
  await expect(page.getByText(/鍒囨崲绉熸埛/)).toBeVisible()
  const switchTenantResponse = page.waitForResponse((response) =>
    response.url().includes('/auth/switch-tenant/2') && response.status() === 200
  )
  await page.getByText(/鍒囨崲绉熸埛/).click()
  await page.getByRole('menuitem').filter({ hasText: targetTenantName }).first().click()

  await switchTenantResponse
  await expect.poll(async () => {
    try {
      return await page.evaluate(() => localStorage.getItem('tenantId'))
    } catch (error) {
      return null
    }
  }, { timeout: 30000 }).toBe('2')
  await page.waitForLoadState('networkidle')
  await expect(page).toHaveURL(/localhost:5173\/?$/)
})
