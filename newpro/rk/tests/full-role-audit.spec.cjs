// @ts-check
const { test, expect } = require('@playwright/test');
const {
  SUPER_ADMIN,
  TENANT1_MANAGER,
  TENANT1_MEMBER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs');

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173';
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';

const USERS = {
  admin: SUPER_ADMIN,
  manager: TENANT1_MANAGER,
  teacher: TENANT1_LIVE_TEACHER,
  member: TENANT1_MEMBER,
};

async function loginApi(request, user) {
  const response = await request.post(`${API_BASE}/auth/login`, { data: user });
  const body = await response.json();
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

async function prepareSession(page, request, user) {
  const loginData = await loginApi(request, user);
  await applyLogin(page, loginData);
  page.on('dialog', async (dialog) => {
    await dialog.accept();
  });
  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(600);
}

test.describe('Full Role Audit', () => {
  test('guest join page should expose tenant selection and not force login', async ({ page }) => {
    await page.goto(`${BASE_URL}/join`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('.join-form').first()).toBeVisible();
    await expect(page.locator('.join-form .el-select').first()).toBeVisible();
    await expect(page.locator('.join-form input[type="password"]')).toHaveCount(2);
    expect.soft(page.url(), 'guest join should not force redirect to login').not.toContain('/login');
  });

  test('member should be blocked from admin routes', async ({ page }) => {
    await prepareSession(page, page.request, USERS.member);
    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(1000);

    const url = page.url();
    const blocked = url === `${BASE_URL}/` || url === `${BASE_URL}` || url.includes('/login') || url.includes('/403');
    expect.soft(blocked).toBeTruthy();
  });

  test('teacher should not access management routes beyond approvals and stats', async ({ page }) => {
    await prepareSession(page, page.request, USERS.teacher);

    await page.goto(`${BASE_URL}/admin/activity/list`, { waitUntil: 'domcontentloaded' });
    const teacherHasAdd = await page.getByRole('button').filter({ hasText: /鏂板娲诲姩|鏂板/ }).first().isVisible().catch(() => false);
    const teacherHasEdit = await page.getByText(/缂栬緫/).first().isVisible().catch(() => false);
    const teacherHasDelete = await page.getByText(/鍒犻櫎/).first().isVisible().catch(() => false);
    expect.soft(teacherHasAdd, 'teacher should not see add activity actions').toBeFalsy();
    expect.soft(teacherHasEdit, 'teacher should not see edit activity actions').toBeFalsy();
    expect.soft(teacherHasDelete, 'teacher should not see delete activity actions').toBeFalsy();

    await page.goto(`${BASE_URL}/admin/content/news-approval`, { waitUntil: 'domcontentloaded' });
    expect.soft(page.url(), 'teacher should access news approval').toContain('/admin/content/news-approval');

    await page.goto(`${BASE_URL}/admin/activity/approval`, { waitUntil: 'domcontentloaded' });
    expect.soft(page.url(), 'teacher should access activity approval').toContain('/admin/activity/approval');
  });

  test('manager should have club content management entry points', async ({ page }) => {
    await prepareSession(page, page.request, USERS.manager);
    expect.soft(await page.getByText(USERS.manager.username).first().isVisible().catch(() => false), 'manager should keep an authenticated admin session').toBeTruthy();

    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(800);
    expect.soft(page.url(), 'manager should not access user management').not.toContain('/admin/system/users');
  });

  test('admin should access system management routes', async ({ page }) => {
    await prepareSession(page, page.request, USERS.admin);

    await page.goto(`${BASE_URL}/admin/system/users`, { waitUntil: 'domcontentloaded' });
    expect.soft(page.url()).toContain('/admin/system/users');

    await page.goto(`${BASE_URL}/admin/system/roles`, { waitUntil: 'domcontentloaded' });
    expect.soft(page.url()).toContain('/admin/system/roles');

    await page.goto(`${BASE_URL}/admin/system/tenants`, { waitUntil: 'domcontentloaded' });
    expect.soft(page.url()).toContain('/admin/system/tenants');
  });
});
