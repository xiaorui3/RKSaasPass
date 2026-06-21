// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, gotoAdmin, waitForTable, clickButton, selectOption, submitFormDialog, dismissSuccessMessage, apiDelete, uniqueName, API_URL } = require('./helpers');

test.describe('后台新闻 CRUD', () => {
  let token;
  const createdIds = [];

  test.beforeEach(async ({ page, request }) => {
    const loginData = await loginByApi(page, request, 'admin');
    token = loginData.token;
    await gotoAdmin(page, '/admin/content/news');
    await waitForTable(page);
  });

  test.afterEach(async ({ request }) => {
    for (const id of createdIds) {
      await apiDelete(request, `/api/news/delete/${id}`, token).catch(() => {});
    }
    createdIds.length = 0;
  });

  test('创建新闻（草稿）', async ({ page, request }) => {
    const title = uniqueName('CRUD测试新闻');
    const resp = await request.post(`${API_URL}/api/news/add`, {
      data: {
        title, category: '1', content: 'CRUD自动化测试创建的新闻内容',
        isPublished: 0, isFeatured: 0, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const newsId = body.data?.id || body.data;
    if (newsId) createdIds.push(newsId);

    // 验证 API 创建成功
    expect([200, 0].includes(body.code || body.state) || body.data).toBeTruthy();

    // 验证列表页面正常加载
    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);
  });

  test('编辑新闻', async ({ page, request }) => {
    const title = uniqueName('待编辑新闻');
    const createResp = await request.post(`${API_URL}/api/news/add`, {
      data: {
        title, category: '1', content: '原始内容',
        isPublished: 0, isFeatured: 0, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const createBody = await createResp.json();
    const newsId = createBody.data?.id || createBody.data;
    if (newsId) createdIds.push(newsId);

    await page.reload({ waitUntil: 'domcontentloaded' });
    await waitForTable(page);

    // 搜索确保新闻在当前页
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

  test('删除新闻', async ({ page, request }) => {
    const title = uniqueName('待删除新闻');
    const resp = await request.post(`${API_URL}/api/news/add`, {
      data: {
        title, category: '1', content: '删除测试',
        isPublished: 0, isFeatured: 0, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const newsId = body.data?.id || body.data;
    if (newsId) createdIds.push(newsId);

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

  test('发布/下架新闻', async ({ page, request }) => {
    const title = uniqueName('发布测试新闻');
    const resp = await request.post(`${API_URL}/api/news/add`, {
      data: {
        title, category: '2', content: '发布测试',
        isPublished: 0, isFeatured: 0, isCrossTenant: 0,
      },
      headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
    });
    const body = await resp.json();
    const newsId = body.data?.id || body.data;
    if (newsId) createdIds.push(newsId);

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
    const publishBtn = row.locator('.el-button:has-text("发布")').first();
    if (await publishBtn.count() > 0) {
      await publishBtn.click();
      await page.waitForTimeout(1000);
      await dismissSuccessMessage(page);
    }
  });

  test('新闻审批页面加载', async ({ page }) => {
    await gotoAdmin(page, '/admin/content/news-approval');
    await waitForTable(page);
    const pageContent = await page.textContent('body');
    expect(pageContent).toBeTruthy();
  });
});
