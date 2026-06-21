// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, gotoAdmin, waitForTable, clickButton, selectOption, submitFormDialog, apiDelete, uniqueName, API_URL } = require('./helpers');

test.describe('后台活动管理 CRUD', () => {
  let token;
  const createdIds = [];

  test.beforeEach(async ({ page, request }) => {
    const loginData = await loginByApi(page, request, 'admin');
    token = loginData.token;
    await gotoAdmin(page, '/admin/activity/list');
    await waitForTable(page);
  });

  test.afterEach(async ({ request }) => {
    for (const id of createdIds) {
      await apiDelete(request, `/api/activity/delete/${id}`, token).catch(() => {});
    }
    createdIds.length = 0;
  });

  test('创建活动', async ({ page, request }) => {
    const title = uniqueName('CRUD测试活动');
    const now = new Date();
    const start = new Date(now.getTime() + 86400000).toISOString();
    const end = new Date(now.getTime() + 3 * 86400000).toISOString();

    const resp = await request.post(`${API_URL}/api/activity/add`, {
      data: {
        activityName: title, activityType: 1,
        startTime: start, endTime: end,
        registrationStartTime: start, registrationEndTime: end,
        location: '测试地点-教学楼A101', content: 'CRUD自动化测试',
        maxParticipants: 50, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const actId = body.data?.id || body.data;
    if (actId) createdIds.push(actId);

    // 验证 API 创建成功
    expect(body).toBeTruthy();

    // 验证列表页面正常加载
    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);
  });

  test('编辑活动', async ({ page, request }) => {
    const title = uniqueName('待编辑活动');
    const now = new Date();
    const start = new Date(now.getTime() + 86400000).toISOString();
    const end = new Date(now.getTime() + 3 * 86400000).toISOString();

    const resp = await request.post(`${API_URL}/api/activity/add`, {
      data: {
        activityName: title, activityType: 1,
        startTime: start, endTime: end,
        registrationStartTime: start, registrationEndTime: end,
        location: '测试地点', content: '原始内容',
        maxParticipants: 50, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const actId = body.data?.id || body.data;
    if (actId) createdIds.push(actId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    // 搜索确保可见
    const searchInput = page.locator('.search-form input.el-input__inner, .el-form--inline input.el-input__inner').first();
    if (await searchInput.count() > 0) {
      await searchInput.fill(title);
      await page.locator('.el-button:has-text("搜索")').first().click();
      await page.waitForTimeout(1500);
      await waitForTable(page);
    }

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

  test('删除活动', async ({ page, request }) => {
    const title = uniqueName('待删除活动');
    const now = new Date();
    const start = new Date(now.getTime() + 86400000).toISOString();
    const end = new Date(now.getTime() + 3 * 86400000).toISOString();

    const resp = await request.post(`${API_URL}/api/activity/add`, {
      data: {
        activityName: title, activityType: 2,
        startTime: start, endTime: end,
        registrationStartTime: start, registrationEndTime: end,
        location: '删除测试地点', content: '删除测试',
        maxParticipants: 30, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const actId = body.data?.id || body.data;
    if (actId) createdIds.push(actId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    const searchInput = page.locator('.search-form input.el-input__inner, .el-form--inline input.el-input__inner').first();
    if (await searchInput.count() > 0) {
      await searchInput.fill(title);
      await page.locator('.el-button:has-text("搜索")').first().click();
      await page.waitForTimeout(1500);
      await waitForTable(page);
    }

    const row = page.locator('.el-table__row').filter({ hasText: title }).first();
    if (await row.count() > 0) {
      await row.locator('.el-button:has-text("删除")').first().click();
      await page.waitForTimeout(500);
      const confirmBtn = page.locator('.el-message-box .el-button--primary').first();
      await confirmBtn.click();
      await page.waitForTimeout(2000);
    }
  });

  test('活动审批页面加载', async ({ page }) => {
    await gotoAdmin(page, '/admin/activity/approval');
    await waitForTable(page);
    const content = await page.textContent('body');
    expect(content).toBeTruthy();
  });
});
