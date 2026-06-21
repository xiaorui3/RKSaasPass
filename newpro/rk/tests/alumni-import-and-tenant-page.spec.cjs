// @ts-check
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { cleanupAlumniFixture } = require('./helpers/live-fixture-cleanup.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const ADMIN = {
  username: 'admin_a',
  password: 'change-me',
  organizationId: '1'
}

async function loginByApi(page, request, user) {
  const response = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }

  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
  await page.evaluate((loginData) => {
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('tenantId', loginData.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      organizationId: loginData.organizationId,
      roles: loginData.roles || []
    }))
  }, body.data)
}

test('tenant page api should return paged structure', async ({ request }) => {
  const loginResponse = await request.post(`${API_BASE}/auth/login`, {
    data: ADMIN
  })
  const loginBody = await loginResponse.json()
  expect(loginBody.code).toBe(200)
  const response = await request.get(`${API_BASE}/tenants/page?pageNum=1&pageSize=5`, {
    headers: {
      Authorization: `Bearer ${loginBody.data.token}`,
      'X-Tenant-Id': loginBody.data.organizationId
    }
  })
  const body = await response.json()
  expect(response.ok()).toBeTruthy()
  expect(body.code).toBe(200)
  expect(body.data).toHaveProperty('records')
  expect(body.data).toHaveProperty('total')
})

test('admin should import alumni by csv and see the imported row', async ({ page, request }) => {
  const seed = Date.now()
  const alumniName = `alumni-csv-${seed}`
  const filePath = path.join(os.tmpdir(), `alumni-import-${seed}.csv`)
  const csv = [
    'name,generationYear,major,workUnit,position,currentContact,email,honorCertificates,notes',
    `${alumniName},2020,Software Engineering,Test Company,Engineer,13800138000,alumni-${seed}@example.com,Outstanding Alumni,automation import`
  ].join('\n')
  fs.writeFileSync(filePath, csv, 'utf8')

  try {
    await loginByApi(page, request, ADMIN)
    await page.goto(`${BASE_URL}/admin/club/alumni`, { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: /Excel瀵煎叆/ }).click()
    await page.setInputFiles('input[type="file"]', filePath)
    await page.getByRole('button', { name: /寮€濮嬪/ }).click()
    await expect(page.locator('.el-message').last()).toContainText(/瀵煎叆|鎴愬姛/)
    await page.getByPlaceholder('璇疯緭鍏ュ').fill(alumniName)
    await page.getByRole('button', { name: /鎼滅储/ }).click()
    await expect(page.locator('.el-table__row').filter({ hasText: alumniName }).first()).toBeVisible()
  } finally {
    await cleanupAlumniFixture({
      tenantId: Number(ADMIN.organizationId || 1),
      name: alumniName,
      email: `alumni-${seed}@example.com`
    }).catch(() => {})
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath)
    }
  }
})
