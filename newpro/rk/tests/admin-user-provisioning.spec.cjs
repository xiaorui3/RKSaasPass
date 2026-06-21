// @ts-check
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const path = require('path')
const XLSX = require('xlsx')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')
const { cleanupManagedUserFixture } = require('./helpers/live-fixture-cleanup.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: user
  })
  const body = await res.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  return body.data
}

async function waitForLogin(request, user) {
  const deadline = Date.now() + 15000
  let lastBody = null
  while (Date.now() < deadline) {
    const res = await request.post(`${API_BASE}/auth/login`, {
      data: user
    })
    lastBody = await res.json()
    if (lastBody.code === 200 && lastBody.data?.token) {
      return lastBody
    }
    await new Promise((resolve) => setTimeout(resolve, 1000))
  }
  throw new Error(`Login failed after retry: ${JSON.stringify({ user, lastBody })}`)
}

function buildHeaders(loginData) {
  return {
    Authorization: `Bearer ${loginData.token}`,
    'X-Tenant-Id': String(loginData.organizationId || '1')
  }
}

async function applyLogin(page, loginData) {
  await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle' })
  await page.evaluate((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', String(data.organizationId))
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }))
  }, loginData)
}

async function fetchRoleList(request, loginData, tenantId = loginData.organizationId || '1') {
  const res = await request.get(`${API_BASE}/roles/list`, {
    headers: buildHeaders(loginData),
    params: { tenantId }
  })
  expect(res.status()).toBe(200)
  const body = await res.json()
  expect(body.code).toBe(200)
  return body.data || []
}

test('super admin should create a login-capable user and surface it in user management', async ({ page, request }) => {
  const seed = Date.now()
  const username = `admin_created_${seed}`
  const password = 'Passw0rd!'
  const mobile = `139${String(seed).slice(-8)}`
  const email = `admin-created-${seed}@example.com`
  const studentId = `S${seed}`

  try {
    const login = await loginApi(request, SUPER_ADMIN)
    const roles = await fetchRoleList(request, login, '1')
    const targetRole = roles.find((item) => Number(item.id) === 2) || roles[0]
    expect(targetRole).toBeTruthy()

    const createRes = await request.post(`${API_BASE}/users`, {
      headers: buildHeaders(login),
      data: {
        tenantId: 1,
        username,
        password,
        name: `鑷姩鍒涘缓${seed}`,
        cellPhone: mobile,
        email,
        studentId,
        type: 2,
        roleId: Number(targetRole.id),
        status: 1
      }
    })
    expect(createRes.status()).toBe(200)

    const loginBody = await waitForLogin(request, {
      username,
      password,
      organizationId: String(login.organizationId || 1)
    })
    expect(loginBody.code).toBe(200)
    expect(loginBody.data?.token).toBeTruthy()

    await applyLogin(page, login)
    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'networkidle' })
    await page.getByRole('textbox').first().fill(username)
    await page.getByRole('button', { name: /鎼滅储|鏌ヨ/ }).click()
    await expect(page.getByText(username, { exact: true }).first()).toBeVisible()
  } finally {
    await cleanupManagedUserFixture({
      tenantId: 1,
      username,
      email,
      studentId
    }).catch(() => {})
  }
})

test('super admin should import a login-capable user from excel', async ({ page, request }) => {
  const seed = Date.now()
  const username = `import_created_${seed}`
  const password = 'Passw0rd!'
  const mobile = `137${String(seed).slice(-8)}`
  const email = `import-created-${seed}@example.com`
  const studentId = `I${seed}`
  const tempFile = path.resolve(__dirname, `../../../logs/import-user-${seed}.xlsx`)

  fs.mkdirSync(path.dirname(tempFile), { recursive: true })

  const workbook = XLSX.utils.book_new()
  const worksheet = XLSX.utils.json_to_sheet([
    {
      tenantId: 1,
      username,
      password,
      name: `瀵煎叆鍒涘缓${seed}`,
      cellPhone: mobile,
      email,
      studentId,
      type: 2,
      roleId: 2,
      status: 1
    }
  ])
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Users')
  XLSX.writeFile(workbook, tempFile)

  try {
    const login = await loginApi(request, SUPER_ADMIN)
    await applyLogin(page, login)

    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: /Excel瀵煎叆/ }).click()
    await page.locator('input[type="file"]').setInputFiles(tempFile)
    await page.getByRole('button', { name: /寮€濮嬪鍏? }).click()
    await expect(page.locator('.el-message--success').last()).toContainText(/鎴愬姛瀵煎叆/, { timeout: 5000 })

    const loginBody = await waitForLogin(request, {
      username,
      password,
      organizationId: String(login.organizationId || 1)
    })
    expect(loginBody.code).toBe(200)
    expect(loginBody.data?.token).toBeTruthy()
  } finally {
    await cleanupManagedUserFixture({
      tenantId: 1,
      username,
      email,
      studentId
    }).catch(() => {})
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile)
    }
  }
})

test('super admin should block importing a student row without studentId', async ({ page, request }) => {
  const seed = Date.now()
  const username = `import_invalid_${seed}`
  const tempFile = path.resolve(__dirname, `../../../logs/import-invalid-user-${seed}.xlsx`)

  fs.mkdirSync(path.dirname(tempFile), { recursive: true })

  const workbook = XLSX.utils.book_new()
  const worksheet = XLSX.utils.json_to_sheet([
    {
      tenantId: 1,
      username,
      password: 'Passw0rd!',
      name: `閺冪姴顒熼崣宄邦嚤閸?{seed}`,
      cellPhone: `136${String(seed).slice(-8)}`,
      email: `import-invalid-${seed}@example.com`,
      type: 2,
      roleId: 2,
      status: 1
    }
  ])
  XLSX.utils.book_append_sheet(workbook, worksheet, 'Users')
  XLSX.writeFile(workbook, tempFile)

  try {
    const login = await loginApi(request, SUPER_ADMIN)
    await applyLogin(page, login)

    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'networkidle' })
    await page.locator('.header-actions .el-button').nth(1).click()
    await page.locator('input[type="file"]').setInputFiles(tempFile)
    await expect(page.locator('.el-message--error').last()).toContainText(/鐎涳箑褰縷瀛﹀彿/, { timeout: 5000 })
  } finally {
    if (fs.existsSync(tempFile)) {
      fs.unlinkSync(tempFile)
    }
  }
})
