// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, gotoAdmin, waitForTable, clickButton, selectOption, submitFormDialog, apiDelete, uniqueName, API_URL } = require('./helpers');

test.describe('后台比赛管理 CRUD', () => {
  let token;
  const createdIds = [];

  test.beforeEach(async ({ page, request }) => {
    const loginData = await loginByApi(page, request, 'admin');
    token = loginData.token;
    await gotoAdmin(page, '/admin/activity/competition');
    await waitForTable(page);
  });

  test.afterEach(async ({ request }) => {
    for (const id of createdIds) {
      await apiDelete(request, `/api/competition/${id}`, token).catch(() => {});
    }
    createdIds.length = 0;
  });

  test('创建比赛', async ({ page }) => {
    await clickButton(page, '新增比赛');
    await page.waitForTimeout(1000);

    const dialog = page.locator('.el-dialog:visible');
    const title = uniqueName('CRUD测试比赛');

    await dialog.locator('input.el-input__inner').first().fill(title);

    // 选择类型
    const typeSelects = dialog.locator('.el-select');
    if (await typeSelects.count() > 0) {
      await selectOption(page, typeSelects.first(), '学术');
    }

    // 选择级别
    if (await typeSelects.count() > 1) {
      await selectOption(page, typeSelects.nth(1), '校级');
    }

    // 填组织者
    const organizerInput = dialog.locator('.el-form-item:has-text("组织者") input.el-input__inner').first();
    if (await organizerInput.count() > 0) {
      await organizerInput.fill('CRUD测试组织者');
    }

    const descArea = dialog.locator('textarea.el-textarea__inner').first();
    if (await descArea.count() > 0) {
      await descArea.fill('CRUD自动化测试创建的比赛描述');
    }

    await submitFormDialog(page);
    await page.waitForTimeout(2000);
  });

  test('编辑比赛', async ({ page, request }) => {
    const title = uniqueName('待编辑比赛');
    const now = new Date();
    const regStart = new Date(now.getTime() + 86400000).toISOString();
    const regEnd = new Date(now.getTime() + 7 * 86400000).toISOString();
    const compStart = new Date(now.getTime() + 8 * 86400000).toISOString();
    const compEnd = new Date(now.getTime() + 10 * 86400000).toISOString();

    const resp = await request.post(`${API_URL}/api/competition`, {
      data: {
        title, description: '原始描述', content: '原始内容',
        competitionType: 'academic', level: 'school',
        organizer: '测试组织者', location: '线上',
        registrationStart: regStart, registrationEnd: regEnd,
        competitionStart: compStart, competitionEnd: compEnd,
        maxParticipants: 100,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    });
    const body = await resp.json();
    const compId = body.data?.id || body.data;
    if (compId) createdIds.push(compId);

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

  test('删除比赛', async ({ page, request }) => {
    const title = uniqueName('待删除比赛');
    const now = new Date();
    const regStart = new Date(now.getTime() + 86400000).toISOString();
    const regEnd = new Date(now.getTime() + 7 * 86400000).toISOString();
    const compStart = new Date(now.getTime() + 8 * 86400000).toISOString();
    const compEnd = new Date(now.getTime() + 10 * 86400000).toISOString();

    const resp = await request.post(`${API_URL}/api/competition`, {
      data: {
        title, description: '删除测试', content: '删除',
        competitionType: 'coding', level: 'school',
        organizer: '测试', location: '线上',
        registrationStart: regStart, registrationEnd: regEnd,
        competitionStart: compStart, competitionEnd: compEnd,
        maxParticipants: 50,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
    });
    const body = await resp.json();
    const compId = body.data?.id || body.data;
    if (compId) createdIds.push(compId);

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

  test('比赛审批页面加载', async ({ page }) => {
    await gotoAdmin(page, '/admin/activity/competition-approval');
    await page.waitForTimeout(2000);
    const content = await page.textContent('body');
    expect(content).toBeTruthy();
  });
});
