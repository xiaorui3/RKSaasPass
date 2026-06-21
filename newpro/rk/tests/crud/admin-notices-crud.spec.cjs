// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, gotoAdmin, waitForTable, clickButton, selectOption, submitFormDialog, dismissSuccessMessage, apiDelete, uniqueName, API_URL } = require('./helpers');

test.describe('后台公告 CRUD', () => {
  let token;
  const createdIds = [];

  test.beforeEach(async ({ page, request }) => {
    const loginData = await loginByApi(page, request, 'admin');
    token = loginData.token;
    await gotoAdmin(page, '/admin/content/notices');
    await waitForTable(page);
  });

  test.afterEach(async ({ request }) => {
    for (const id of createdIds) {
      await apiDelete(request, `/notifications/api/notices/${id}`, token).catch(() => {});
    }
    createdIds.length = 0;
  });

  test('创建公告（草稿）', async ({ page }) => {
    await clickButton(page, '发布公告');
    await page.waitForTimeout(800);

    const title = uniqueName('CRUD测试公告');
    const dialog = page.locator('.el-dialog:visible');

    await dialog.locator('input.el-input__inner').first().fill(title);

    const typeSelect = dialog.locator('.el-select').first();
    await selectOption(page, typeSelect, '系统公告');

    const contentArea = dialog.locator('textarea.el-textarea__inner').first();
    await contentArea.fill('这是CRUD自动化测试创建的公告内容');

    const draftBtn = dialog.locator('.el-button:has-text("保存草稿")').first();
    if (await draftBtn.count() > 0) {
      await draftBtn.click();
    } else {
      await submitFormDialog(page);
    }
    await page.waitForTimeout(2000);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);
    const found = await page.locator('.el-table__body-wrapper').textContent();
    expect(found).toContain(title);
  });

  test('编辑公告', async ({ page, request }) => {
    const title = uniqueName('待编辑公告');
    const resp = await request.post(`${API_URL}/notifications/api/notices/create`, {
      data: {
        title, content: '原始公告内容', noticeType: 1,
        isTop: 0, isPublished: false,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    });
    const body = await resp.json();
    const noticeId = body.data?.id || body.data;
    if (noticeId) createdIds.push(noticeId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    const row = page.locator('.el-table__row').filter({ hasText: title }).first();
    if (await row.count() > 0) {
      await row.locator('.el-button:has-text("编辑")').first().click();
      await page.waitForTimeout(1000);

      const dialog = page.locator('.el-dialog:visible');
      const titleInput = dialog.locator('input.el-input__inner').first();
      await titleInput.clear();
      await titleInput.fill(title + '-已编辑');

      await submitFormDialog(page);
      await page.waitForTimeout(2000);
    }
  });

  test('删除公告', async ({ page, request }) => {
    const title = uniqueName('待删除公告');
    const resp = await request.post(`${API_URL}/notifications/api/notices/create`, {
      data: {
        title, content: '删除测试公告', noticeType: 2,
        isTop: 0, isPublished: false,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    });
    const body = await resp.json();
    const noticeId = body.data?.id || body.data;
    if (noticeId) createdIds.push(noticeId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    const row = page.locator('.el-table__row').filter({ hasText: title }).first();
    if (await row.count() > 0) {
      await row.locator('.el-button:has-text("删除")').first().click();
      await page.waitForTimeout(500);
      const confirmBtn = page.locator('.el-message-box .el-button--primary').first();
      await confirmBtn.click();
      await page.waitForTimeout(2000);
    }
  });

  test('发布公告', async ({ page, request }) => {
    const title = uniqueName('发布测试公告');
    const resp = await request.post(`${API_URL}/notifications/api/notices/create`, {
      data: {
        title, content: '发布测试', noticeType: 1,
        isTop: 0, isPublished: false,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    });
    const body = await resp.json();
    const noticeId = body.data?.id || body.data;
    if (noticeId) createdIds.push(noticeId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    const row = page.locator('.el-table__row').filter({ hasText: title }).first();
    const publishBtn = row.locator('.el-button:has-text("发布")').first();
    if (await publishBtn.count() > 0) {
      await publishBtn.click();
      await page.waitForTimeout(1500);
    }
  });
});
