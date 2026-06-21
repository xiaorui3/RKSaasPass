// @ts-check
const fs = require('node:fs');
const path = require('node:path');
const { test, expect } = require('@playwright/test');
const { SUPER_ADMIN } = require('./helpers/test-users.cjs');

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173';
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';
const SCREENSHOT_DIR = path.resolve(__dirname, '..', 'test-screenshots', 'news-module');

fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });

async function shot(page, name) {
  await page.screenshot({
    path: path.join(SCREENSHOT_DIR, name),
    fullPage: true
  });
}

async function loginApi(request, user = SUPER_ADMIN) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user });
  const body = await res.json();
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`);
  }
  return body.data;
}

async function applyLogin(page, loginData) {
  await page.addInitScript((data) => {
    localStorage.setItem('token', data.token);
    localStorage.setItem('tenantId', data.organizationId);
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles || []
    }));
  }, loginData);
}

test.describe('RK-Web 棣栭〉涓庢柊闂绘ā鍧楁祴璇?, () => {
  test('01-棣栭〉鍔犺浇鍜屽鑸?, async ({ page }) => {
    await page.goto(BASE_URL, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await shot(page, 'test-news-01-homepage.png');

    await expect(page).toHaveTitle(/璇风櫥褰晐RK|绀惧洟|宸ヤ綔瀹?i);
    await expect(page.locator('main, #app').first()).toBeVisible();
  });

  test('02-鏂伴椈鍒楄〃鍓嶅彴', async ({ page }) => {
    await page.goto(`${BASE_URL}/news`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);

    if (!page.url().includes('/news')) {
      await page.goto(BASE_URL, { waitUntil: 'networkidle' });
      const newsLink = page
        .locator('a[href*="news"], a:has-text("鏂伴椈"), a:has-text("鍔ㄦ€?)')
        .first();
      if (await newsLink.count()) {
        await newsLink.click();
        await page.waitForTimeout(1000);
      }
    }

    await shot(page, 'test-news-02-news-list.png');
    await expect(page).toHaveURL(/news|home|\/$/);
  });

  test('03-鏂伴椈璇︽儏椤?, async ({ page }) => {
    await page.goto(`${BASE_URL}/news`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);

    const firstNews = page
      .locator('.news-item a, .news-card a, article a, .list-item a, a[href*="/news/"]')
      .first();

    if (await firstNews.count()) {
      await firstNews.click();
      await page.waitForTimeout(1000);
      await shot(page, 'test-news-03-news-detail.png');
      const hasTitle = await page.locator('h1, .title, .news-title').count();
      const hasContent = await page.locator('.content, .news-content, article').count();
      expect(hasTitle > 0 || hasContent > 0).toBeTruthy();
      return;
    }

    await shot(page, 'test-news-03-no-news.png');
  });

  test('04-鍚庡彴鏂伴椈绠＄悊鍒楄〃', async ({ page, request }) => {
    const login = await loginApi(request);
    await applyLogin(page, login);

    await page.goto(`${BASE_URL}/admin/content/news`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    await shot(page, 'test-news-04-admin-news-list.png');
    await expect(page).toHaveURL(/admin\/content\/news/);
  });

  test('05-鏂伴椈鏂板椤靛彲璁块棶', async ({ page, request }) => {
    const login = await loginApi(request);
    await applyLogin(page, login);

    await page.goto(`${BASE_URL}/admin/content/news`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);
    const publishBtn = page.getByRole('button', { name: /鍙戝竷鏂伴椈|鏂板鏂伴椈/ }).first();
    await expect(publishBtn).toBeVisible();
    await publishBtn.click();
    await page.waitForTimeout(500);
    await shot(page, 'test-news-05-create-dialog.png');

    const titleInput = page
      .locator('input[name="title"], input[placeholder*="鏍囬"], input[placeholder*="title"]')
      .first();
    await expect(titleInput).toBeVisible();

    await titleInput.fill(`鑷姩鍖栨祴璇曟柊闂绘爣棰?${Date.now()}`);
    await shot(page, 'test-news-05-form-filled.png');
  });

  test.afterAll(async () => {
    // eslint-disable-next-line no-console
    console.log(`news-module screenshots: ${SCREENSHOT_DIR}`);
  });
});
