// @ts-check
const { test, expect } = require('@playwright/test');
const {
  TENANT1_MANAGER,
  TENANT1_MEMBER,
  TENANT1_LIVE_TEACHER
} = require('./helpers/test-users.cjs');
const { loginApi } = require('./helpers/live-login.cjs');

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173';
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';

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

async function searchApprovalActivity(page, title) {
  const searchInput = page.locator('.search-form input').first();
  await searchInput.fill(title);
  await page.locator('.search-form .el-button--primary').click({ force: true });
  await page.waitForTimeout(1000);
}

async function confirmDialogIfVisible(page) {
  const primaryButton = page.locator('.el-message-box__btns .el-button--primary').last();
  if (await primaryButton.isVisible().catch(() => false)) {
    await primaryButton.click();
    await page.waitForTimeout(800);
  }
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

test('member submitted activity should pass manager and teacher approval', async ({ browser }) => {
  test.setTimeout(90000);
  const seed = Date.now();
  const activityTitle = `dual-review-activity-${seed}`;
  const activityLocation = `QA-ROOM-${seed}`;
  const now = new Date();

  const memberContext = await browser.newContext();
  const memberPage = await memberContext.newPage();
  const memberLogin = await loginApi(memberPage.request, TENANT1_MEMBER, API_BASE);
  await applyLogin(memberPage, memberLogin);

  const managerContext = await browser.newContext();
  const managerPage = await managerContext.newPage();
  const managerLogin = await loginApi(managerPage.request, TENANT1_MANAGER, API_BASE);
  await applyLogin(managerPage, managerLogin);

  const teacherContext = await browser.newContext();
  const teacherPage = await teacherContext.newPage();
  const teacherLogin = await loginApi(teacherPage.request, TENANT1_LIVE_TEACHER, API_BASE);
  await applyLogin(teacherPage, teacherLogin);

  const submitRes = await memberPage.request.post(`${API_BASE}/api/activity/submit`, {
    headers: {
      Authorization: `Bearer ${memberLogin.token}`,
      'X-Tenant-Id': memberLogin.organizationId,
      'Content-Type': 'application/json'
    },
    data: {
      activityName: activityTitle,
      activityType: 1,
      organizer: memberLogin.username || TENANT1_MEMBER.username,
      location: activityLocation,
      maxParticipants: 50,
      startTime: formatDateTime(new Date(now.getTime() + 24 * 60 * 60 * 1000)),
      endTime: formatDateTime(new Date(now.getTime() + 26 * 60 * 60 * 1000)),
      registrationStartTime: formatDateTime(new Date(now.getTime() - 60 * 60 * 1000)),
      registrationEndTime: formatDateTime(new Date(now.getTime() + 60 * 60 * 1000)),
      managerReviewerId: managerLogin.userId,
      teacherReviewerId: teacherLogin.userId,
      content: 'Member submits activity, then manager and teacher approve before registration opens.'
    }
  });
  const submitBody = await submitRes.json();
  const submittedActivityId = submitBody?.data ? String(submitBody.data) : null;
  expect(submittedActivityId).toBeTruthy();

  await managerPage.goto(`${BASE_URL}/admin/activity/approval`, { waitUntil: 'domcontentloaded' });
  await expect(managerPage.locator('.search-form')).toBeVisible();
  await searchApprovalActivity(managerPage, activityTitle);
  await expect(managerPage.getByText(activityTitle)).toBeVisible();
  const managerRow = managerPage.locator('.el-table__row').filter({ hasText: activityTitle }).first();
  await managerRow.locator('.el-button').nth(1).click();
  await confirmDialogIfVisible(managerPage);
  await managerPage.waitForTimeout(1500);
  await searchApprovalActivity(managerPage, activityTitle);
  await expect(managerPage.getByText(activityTitle)).toHaveCount(0);

  await teacherPage.goto(`${BASE_URL}/admin/activity/approval`, { waitUntil: 'domcontentloaded' });
  await expect(teacherPage.locator('.search-form')).toBeVisible();
  await searchApprovalActivity(teacherPage, activityTitle);
  await expect(teacherPage.getByText(activityTitle)).toBeVisible();
  const teacherRow = teacherPage.locator('.el-table__row').filter({ hasText: activityTitle }).first();
  await teacherRow.locator('.el-button').nth(1).click();
  await confirmDialogIfVisible(teacherPage);
  await teacherPage.waitForTimeout(1000);

  await memberPage.goto(`${BASE_URL}/activities/${submittedActivityId}`, { waitUntil: 'domcontentloaded' });
  await expect(memberPage.getByText(activityTitle)).toBeVisible();

  await memberContext.close();
  await managerContext.close();
  await teacherContext.close();
});
