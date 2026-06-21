// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://127.0.0.1:5173'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const AUTH_BASE = process.env.PLAYWRIGHT_AUTH_BASE || API_BASE
const ADMIN = { username: 'admin_a', password: 'change-me', organizationId: '1' }

async function loginByApi(page, user) {
  const response = await page.request.post(`${AUTH_BASE}/auth/login`, { data: user })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }
  await page.addInitScript((loginData) => {
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('tenantId', loginData.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      organizationId: loginData.organizationId,
      roles: loginData.roles || []
    }))
  }, body.data)
  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' })
}

test('admin should complete real menu crud and tenant paging smoke', async ({ page }) => {
  const seed = Date.now()
  const menuName = `E2E鑿滃崟${seed}`
  const updatedMenuName = `${menuName}-宸叉敼`
  const menuPath = `/e2e-menu-${seed}`

  await loginByApi(page, ADMIN)

  await page.goto(`${BASE_URL}/admin/system/menus`, { waitUntil: 'domcontentloaded' })
  await page.getByRole('button', { name: /鏂板鑿滃崟/ }).click()

  const dialogInputs = page.locator('.el-dialog .el-input__inner')
  await dialogInputs.nth(0).fill(menuName)
  await dialogInputs.nth(1).fill(menuPath)
  await page.locator('.el-dialog .el-input-number input').fill('99')
  await page.locator('.el-dialog__footer .el-button--primary').click()
  await expect(page.locator('.el-table__row').filter({ hasText: menuName }).first()).toBeVisible()

  const row = page.locator('.el-table__row').filter({ hasText: menuName }).first()
  await row.getByText('缂栬緫').click()
  await page.locator('.el-dialog .el-input__inner').nth(0).fill(updatedMenuName)
  await page.locator('.el-dialog__footer .el-button--primary').click()
  await expect(page.locator('.el-table__row').filter({ hasText: updatedMenuName }).first()).toBeVisible()

  await page.locator('.el-table__row').filter({ hasText: updatedMenuName }).first().getByText('鍒犻櫎').click()
  const confirmButton = page.locator('.el-message-box__btns .el-button--primary').last()
  if (await confirmButton.isVisible().catch(() => false)) {
    await confirmButton.click()
  }
  await expect(page.locator('.el-table__row').filter({ hasText: updatedMenuName })).toHaveCount(0)

  await page.goto(`${BASE_URL}/admin/system/tenants`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.admin-page, .el-table, .el-empty').first()).toBeVisible()

  const tenantResponse = await page.request.get(`${API_BASE}/tenants/list`)
  const tenantBody = await tenantResponse.json()
  expect(tenantBody.code).toBe(200)
  expect(Array.isArray(tenantBody.data)).toBeTruthy()
  expect(tenantBody.data.length).toBeGreaterThan(0)
})
