// @ts-check
const { test, expect } = require('@playwright/test')
const { SUPER_ADMIN } = require('./helpers/test-users.cjs')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://example.com:30001'
const API_BASE = process.env.PLAYWRIGHT_API_BASE || BASE_URL

async function loginApi(request) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: SUPER_ADMIN })
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

async function gotoAdmin(page, path) {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.admin-layout')).toBeVisible({ timeout: 15000 })
  await expect(page.locator('.el-table, .el-card, .gallery-page').first()).toBeVisible({ timeout: 15000 })
  await expect(page.locator('text=404')).toHaveCount(0)
}

test.describe('current k8s admin smoke', () => {
  test.beforeEach(async ({ page, request }) => {
    await applyLogin(page, await loginApi(request))
  })

  test('album, registrations, credit picker, role menu tree and menu page should render', async ({ page }) => {
    const failedResponses = []
    page.on('response', (response) => {
      if (response.status() >= 500) {
        failedResponses.push(`${response.status()} ${response.url()}`)
      }
    })

    await gotoAdmin(page, '/admin/activity/photo-gallery')
    await expect(page.locator('.gallery-page')).toBeVisible()
    await expect(page.locator('.activity-select')).toBeVisible()

    await gotoAdmin(page, '/admin/activity/list')
    const registrationButton = page.getByRole('button', { name: /鎶ュ悕绠＄悊/ }).first()
    await expect(registrationButton).toBeVisible({ timeout: 15000 })
    await registrationButton.click()
    await expect(page.locator('.el-dialog:visible').filter({ hasText: /鎶ュ悕绠＄悊/ })).toBeVisible({ timeout: 15000 })

    await gotoAdmin(page, '/admin/activity/credit')
    await page.getByRole('button', { name: /鏂板璁板綍/ }).first().click()
    await expect(page.locator('.el-dialog:visible').filter({ hasText: /鏂板瀛﹀垎璁板綍/ })).toBeVisible()
    await page.getByRole('button', { name: /^閫夋嫨$/ }).first().click()
    const userPicker = page.locator('.el-dialog:visible').filter({ hasText: /閫夋嫨瀛﹀垎鐢ㄦ埛/ })
    await expect(userPicker).toBeVisible({ timeout: 15000 })
    await expect(userPicker.locator('.el-table')).toBeVisible()

    await gotoAdmin(page, '/admin/system/roles')
    await page.getByRole('button', { name: /鍒嗛厤鏉冮檺/ }).first().click()
    const permissionDialog = page.locator('.el-dialog:visible').filter({ hasText: /鍒嗛厤鏉冮檺/ })
    await expect(permissionDialog).toBeVisible({ timeout: 15000 })
    await expect(permissionDialog.locator('.el-tree-node').first()).toBeVisible({ timeout: 15000 })

    await gotoAdmin(page, '/admin/system/menus')
    await expect(page.locator('.el-table')).toBeVisible({ timeout: 15000 })
    expect(failedResponses).toEqual([])
  })
})
