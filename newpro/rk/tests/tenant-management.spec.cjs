// @ts-check
const { test, expect } = require('@playwright/test')
const helpers = require('./crud/helpers.cjs')

const { BASE_URL, API_URL, loginByApi, gotoAdmin, waitForTable, clickButton, submitFormDialog, dismissSuccessMessage } = helpers

const TEST_TENANT = {
  tenantName: `测试租户-${Date.now()}`,
  tenantCode: `test_${Date.now()}`,
  contactPerson: '自动化测试',
  contactPhone: '13800001111',
  contactEmail: 'test@example.com',
  address: '测试地址-自动化',
  remark: 'Playwright自动化创建'
}

async function cleanupTenant(request, token, name) {
  try {
    const list = await request.get(`${API_URL}/tenants/list`, {
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    })
    const body = await list.json()
    const records = body.data?.records || body.data || []
    for (const r of records) {
      if (r.tenantName === name) {
        await request.delete(`${API_URL}/tenants/${r.id}`, {
          headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
        }).catch(() => {})
      }
    }
  } catch {}
}

// ── 1. Page Load ─────────────────────────────────────────────────────

test('tenant management page loads with stats and table', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(2000)

  // Stats cards
  const statsCards = page.locator('.stats-card')
  expect(await statsCards.count()).toBe(4)

  // Table
  await waitForTable(page)
  const rows = page.locator('.el-table__row')
  const rowCount = await rows.count()
  expect(rowCount).toBeGreaterThanOrEqual(1)

  // Search form
  await expect(page.locator('input[placeholder="请输入租户名称"]').first()).toBeVisible()
  await expect(page.locator('.el-button:has-text("新增租户")').first()).toBeVisible()
})

// ── 2. Create Tenant ────────────────────────────────────────────────

test('create a new tenant via dialog', async ({ page, request }) => {
  const loginData = await loginByApi(page, request, 'admin')
  const token = loginData.token

  await cleanupTenant(request, token, TEST_TENANT.tenantName)
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)

  // Click add
  await clickButton(page, '新增租户')
  const dialog = page.locator('.el-dialog:visible').first()
  await expect(dialog).toBeVisible()

  // Fill form
  await dialog.locator('input[placeholder="请输入租户名称"]').fill(TEST_TENANT.tenantName)
  await dialog.locator('input[placeholder="请输入租户编码"]').fill(TEST_TENANT.tenantCode)
  await dialog.locator('input[placeholder="请输入联系人"]').fill(TEST_TENANT.contactPerson)
  await dialog.locator('input[placeholder="请输入联系电话"]').fill(TEST_TENANT.contactPhone)
  await dialog.locator('input[placeholder="请输入联系邮箱"]').fill(TEST_TENANT.contactEmail)
  await dialog.locator('input[placeholder="请输入地址"]').fill(TEST_TENANT.address)
  await dialog.locator('textarea[placeholder="请输入备注"]').fill(TEST_TENANT.remark)

  // Submit
  await dialog.locator('.el-button--primary:has-text("确定")').click()
  await page.waitForTimeout(2000)
  await dismissSuccessMessage(page)

  // Verify via API
  const list = await request.get(`${API_URL}/tenants/list`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  })
  const body = await list.json()
  const records = body.data?.records || body.data || []
  const found = records.some(r => r.tenantName === TEST_TENANT.tenantName)
  expect(found).toBeTruthy()

  // Cleanup
  await cleanupTenant(request, token, TEST_TENANT.tenantName)
})

// ── 3. Search Tenant ────────────────────────────────────────────────

test('search tenants by name', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)

  const searchInput = page.locator('input[placeholder="请输入租户名称"]').first()
  await searchInput.fill('租户')
  await clickButton(page, '搜索')
  await page.waitForTimeout(1500)

  // Table should reload
  await waitForTable(page)
})

// ── 4. View Tenant Detail ───────────────────────────────────────────

test('view tenant detail dialog', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)
  await waitForTable(page)

  // Click first detail button
  const detailBtn = page.locator('.el-button:has-text("详情")').first()
  if (await detailBtn.isVisible()) {
    await detailBtn.click()
    await page.waitForTimeout(500)

    const detailDialog = page.locator('.el-dialog:visible').first()
    await expect(detailDialog).toBeVisible()
    await expect(page.getByText('租户详情')).toBeVisible()

    // Close
    await detailDialog.locator('.el-button:has-text("关闭")').click()
  }
})

// ── 5. Edit Tenant ──────────────────────────────────────────────────

test('edit tenant name', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)
  await waitForTable(page)

  const editBtn = page.locator('.el-button:has-text("编辑")').first()
  if (await editBtn.isVisible()) {
    await editBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog:visible').first()
    await expect(dialog).toBeVisible()

    const nameInput = dialog.locator('input[placeholder="请输入租户名称"]').first()
    const currentName = await nameInput.inputValue()
    const newName = currentName + '-edited'

    await nameInput.clear()
    await nameInput.fill(newName)

    await dialog.locator('.el-button--primary:has-text("确定")').click()
    await page.waitForTimeout(2000)
    await dismissSuccessMessage(page)

    // Revert
    await page.waitForTimeout(500)
    const revertBtn = page.locator('.el-button:has-text("编辑")').first()
    if (await revertBtn.isVisible()) {
      await revertBtn.click()
      await page.waitForTimeout(500)
      const d = page.locator('.el-dialog:visible').first()
      const ni = d.locator('input[placeholder="请输入租户名称"]').first()
      await ni.clear()
      await ni.fill(currentName)
      await d.locator('.el-button--primary:has-text("确定")').click()
      await page.waitForTimeout(1500)
    }
  }
})

// ── 6. Status Toggle ────────────────────────────────────────────────

test('toggle tenant status switch', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)
  await waitForTable(page)

  const switchEl = page.locator('.el-table .el-switch').first()
  if (await switchEl.isVisible()) {
    const isOn = await switchEl.getAttribute('aria-checked')
    await switchEl.click()
    await page.waitForTimeout(2000)
    await dismissSuccessMessage(page)

    // Revert
    await page.waitForTimeout(500)
    await switchEl.click()
    await page.waitForTimeout(2000)
    await dismissSuccessMessage(page)
  }
})

// ── 7. Export ────────────────────────────────────────────────────────

test('export tenants to CSV', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)

  const exportBtn = page.locator('.el-button:has-text("导出")').first()
  if (await exportBtn.isVisible()) {
    await exportBtn.click()
    await page.waitForTimeout(1000)
    // Check for success message
    const success = page.locator('.el-message--success')
    const hasSuccess = await success.isVisible().catch(() => false)
    expect(hasSuccess).toBeTruthy()
  }
})

// ── 8. Renew Dialog ─────────────────────────────────────────────────

test('renew dialog opens with tenant info', async ({ page, request }) => {
  await loginByApi(page, request, 'admin')
  await gotoAdmin(page, '/admin/system/tenants')
  await page.waitForTimeout(1500)
  await waitForTable(page)

  const renewBtn = page.locator('.el-button:has-text("续费")').first()
  if (await renewBtn.isVisible()) {
    await renewBtn.click()
    await page.waitForTimeout(500)

    const dialog = page.locator('.el-dialog:visible').first()
    await expect(dialog).toBeVisible()
    await expect(page.getByText('租户续费')).toBeVisible()
    await expect(page.getByText('续费时长')).toBeVisible()

    await dialog.locator('.el-button:has-text("取消")').click()
  }
})
