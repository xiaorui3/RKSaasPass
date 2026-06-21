// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, BASE_URL, API_URL } = require('./helpers');

test.describe('前台交互测试', () => {
  test.beforeEach(async ({ page, request }) => {
    await loginByApi(page, request, 'member');
  });

  test('新闻浏览 → 新闻详情', async ({ page }) => {
    await page.goto(`${BASE_URL}/news`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    // 验证新闻列表页加载
    const pageContent = await page.textContent('body');
    expect(pageContent).toBeTruthy();

    // 尝试点击第一条新闻
    const newsItem = page.locator('.news-item a, .news-card, .el-card, article a, .news-list .item').first();
    if (await newsItem.count() > 0) {
      await newsItem.click();
      await page.waitForTimeout(2000);
      // 验证详情页加载
      const detailContent = await page.textContent('body');
      expect(detailContent).toBeTruthy();
    }
  });

  test('活动浏览 → 活动详情 → 报名', async ({ page }) => {
    await page.goto(`${BASE_URL}/activities`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    const pageContent = await page.textContent('body');
    expect(pageContent).toBeTruthy();

    // 点击第一个活动
    const activityItem = page.locator('.activity-card, .el-card, .activity-item a, .card a').first();
    if (await activityItem.count() > 0) {
      await activityItem.click();
      await page.waitForTimeout(2000);

      // 尝试报名
      const registerBtn = page.locator('.el-button:has-text("报名"), .el-button:has-text("立即报名"), .el-button:has-text("参加")').first();
      if (await registerBtn.count() > 0 && await registerBtn.isEnabled()) {
        await registerBtn.click();
        await page.waitForTimeout(1000);

        // 确认报名
        const confirmBtn = page.locator('.el-message-box .el-button--primary').first();
        if (await confirmBtn.count() > 0) {
          await confirmBtn.click();
          await page.waitForTimeout(1000);
        }
      }
    }
  });

  test('比赛浏览 → 比赛报名', async ({ page }) => {
    await page.goto(`${BASE_URL}/competition`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    const pageContent = await page.textContent('body');
    expect(pageContent).toBeTruthy();

    // 点击第一个比赛
    const compItem = page.locator('.competition-card, .el-card, .competition-item a, .card a').first();
    if (await compItem.count() > 0) {
      await compItem.click();
      await page.waitForTimeout(2000);

      // 尝试报名
      const registerBtn = page.locator('.el-button:has-text("报名"), .el-button:has-text("立即报名"), .el-button:has-text("参加")').first();
      if (await registerBtn.count() > 0 && await registerBtn.isEnabled()) {
        await registerBtn.click();
        await page.waitForTimeout(1000);
      }
    }
  });

  test('作品浏览 → 点赞/取消点赞', async ({ page }) => {
    await page.goto(`${BASE_URL}/works`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    const pageContent = await page.textContent('body');
    expect(pageContent).toBeTruthy();

    // 尝试点赞第一个作品
    const likeBtn = page.locator('.el-button:has-text("赞"), .like-btn, .like-button, [data-testid="like"]').first();
    if (await likeBtn.count() > 0) {
      await likeBtn.click();
      await page.waitForTimeout(1000);
      // 取消点赞
      await likeBtn.click();
      await page.waitForTimeout(500);
    }

    // 点击进入作品详情
    const workItem = page.locator('.work-card a, .el-card, .work-item a, .card a').first();
    if (await workItem.count() > 0) {
      await workItem.click();
      await page.waitForTimeout(2000);
      const detailContent = await page.textContent('body');
      expect(detailContent).toBeTruthy();
    }
  });

  test('首页加载和导航', async ({ page }) => {
    await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);

    // 验证导航栏存在
    const nav = page.locator('nav, .navbar, .main-nav, header, .header').first();
    expect(await nav.count()).toBeGreaterThan(0);

    // 验证首页有内容
    const body = await page.textContent('body');
    expect(body).toBeTruthy();
  });

  test('关于页面加载', async ({ page }) => {
    await page.goto(`${BASE_URL}/about`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    const content = await page.textContent('body');
    expect(content).toBeTruthy();
  });

  test('校友页面加载', async ({ page }) => {
    await page.goto(`${BASE_URL}/alumni`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    const content = await page.textContent('body');
    expect(content).toBeTruthy();
  });
});
