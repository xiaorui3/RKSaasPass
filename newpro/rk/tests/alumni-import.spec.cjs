// @ts-check
const { test, expect } = require('@playwright/test')
const XLSX = require('xlsx')
const fs = require('fs')
const os = require('os')
const path = require('path')
const { cleanupAlumniFixture } = require('./helpers/live-fixture-cleanup.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

async function loginByApi(page, user) {
  const response = await page.request.post(`${API_BASE}/auth/login`, { data: user })
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

test('manager should import alumni by excel and see imported row', async ({ page }) => {
  const seed = Date.now()
  const alumniName = `alumni-import-${seed}`
  const filePath = path.join(os.tmpdir(), `alumni-import-${seed}.xlsx`)
  const workbook = XLSX.utils.book_new()
  const sheet = XLSX.utils.json_to_sheet([
    {
      name: alumniName,
      generationYear: 2020,
      major: 'Software Engineering',
      workUnit: 'Test Company',
      position: 'Engineer',
      currentContact: '13800138000',
      email: `alumni-${seed}@example.com`,
      honorCertificates: 'Outstanding Alumni',
      notes: 'automation import'
    }
  ])
  XLSX.utils.book_append_sheet(workbook, sheet, 'Sheet1')
  XLSX.writeFile(workbook, filePath)

  try {
    await loginByApi(page, MANAGER)
    await page.goto(`${BASE_URL}/admin/club/alumni`, { waitUntil: 'networkidle' })
    await page.getByRole('button', { name: /Excel瀵煎叆/ }).click()

    const importDialog = page.locator('.el-dialog').filter({ hasText: /Excel瀵煎叆鏍″弸/ }).last()
    await page.locator('.el-upload input[type="file"]').setInputFiles(filePath)
    await page.getByRole('button', { name: /寮€濮嬪/ }).click()
    await expect(importDialog).toBeHidden({ timeout: 10000 })

    await page.getByPlaceholder('璇疯緭鍏ュ').fill(alumniName)
    await page.getByRole('button', { name: /鎼滅储/ }).click()
    await expect(page.locator('.el-table__row').filter({ hasText: alumniName }).first()).toBeVisible()
  } finally {
    await cleanupAlumniFixture({
      tenantId: Number(MANAGER.organizationId || 1),
      name: alumniName,
      email: `alumni-${seed}@example.com`
    }).catch(() => {})
    if (fs.existsSync(filePath)) {
      try {
        fs.unlinkSync(filePath)
      } catch (error) {
        // Windows can keep the uploaded temp file locked briefly.
      }
    }
  }
})
