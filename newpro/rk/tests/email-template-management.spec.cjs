// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'

const MANAGER = {
  username: 'manager_a',
  password: '123456',
  organizationId: '1'
}

async function loginApi(request, user) {
  const res = await request.post(`${API_BASE}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
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

test('manager should create and apply an email template in email center', async ({ page, request }) => {
  const seed = Date.now()
  const templateName = `E2E_TEMPLATE_${seed}`
  const templateCode = `EMAIL_${seed}`
  const templateSubject = `E2E Subject ${seed}`
  const templateContent = `E2E Content ${seed}`

  const login = await loginApi(request, MANAGER)
  await applyLogin(page, login)

  await page.goto(`${BASE_URL}/admin/club/email-center`, { waitUntil: 'domcontentloaded' })
  await expect(page).not.toHaveURL(/\/login/)
  await expect(page.locator('.admin-page')).toBeVisible()

  await page.locator('#tab-templates').click()
  await expect(page.locator('.template-actions')).toBeVisible()

  await page.locator('.template-actions .el-button--primary').click()
  const dialog = page.locator('.el-dialog').last()
  await expect(dialog).toBeVisible()

  const dialogInputs = dialog.locator('input')
  await dialogInputs.nth(0).fill(templateName)
  await dialogInputs.nth(1).fill(templateCode)
  await dialogInputs.nth(2).fill(templateSubject)
  await dialog.locator('textarea').first().fill(templateContent)
  await dialog.locator('.el-dialog__footer .el-button--primary').click()

  await expect(page.locator('.el-table__row').filter({ hasText: templateName }).first()).toBeVisible()

  const templateRow = page.locator('.el-table__row').filter({ hasText: templateName }).first()
  await templateRow.locator('td').last().locator('button').first().click()

  await expect(page.locator('#tab-send')).toHaveClass(/is-active/)
})
