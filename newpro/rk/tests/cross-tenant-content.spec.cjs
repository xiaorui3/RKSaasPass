// @ts-check
const { test, expect } = require('@playwright/test');
const {
  TENANT1_MANAGER,
  TENANT1_LIVE_TEACHER,
  TENANT2_ADMIN
} = require('./helpers/test-users.cjs');

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173';
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

async function loginApi(request, user) {
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

async function login(page, user) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('.el-select').first().click();
  const tenantOptions = page.locator('.el-select-dropdown:visible .el-select-dropdown__item');
  if (user.tenantName) {
    await tenantOptions.filter({ hasText: user.tenantName }).first().click({ force: true });
  } else {
    await tenantOptions.first().click({ force: true });
  }
  await page.locator('input:not([readonly]):not([type="password"])').first().fill(user.username);
  await page.locator('input[type="password"]').fill(user.password);
  await page.getByRole('button').filter({ hasText: /褰?i }).click();
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(1000);
}

test('shared activity/news/competition should be visible cross-tenant and shared activity should be registerable', async ({ browser, request }) => {
  const seed = Date.now();
  const activityTitle = `鍏变韩娲诲姩-${seed}`;
  const newsTitle = `鍏变韩鏂伴椈-${seed}`;
  const competitionTitle = `鍏变韩姣旇禌-${seed}`;
  const managerContext = await browser.newContext();
  const managerPage = await managerContext.newPage();
  const managerLogin = await loginApi(request, TENANT1_MANAGER);
  const teacherLogin = await loginApi(request, TENANT1_LIVE_TEACHER);
  await applyLogin(managerPage, managerLogin);
  await managerPage.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });

  const managerAuth = await managerPage.evaluate(() => ({
    token: localStorage.getItem('token'),
    tenantId: localStorage.getItem('tenantId'),
  }));

  const headers = {
    Authorization: `Bearer ${managerAuth.token}`,
    'X-Tenant-Id': managerAuth.tenantId,
    'Content-Type': 'application/json',
  };

  const now = new Date();
  const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
  const oneHourLater = new Date(now.getTime() + 60 * 60 * 1000);
  const tomorrow = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  const tomorrowPlusTwo = new Date(now.getTime() + 26 * 60 * 60 * 1000);

  const activityRes = await request.post(`${API_BASE}/api/activity/add`, {
    headers,
    data: {
      activityName: activityTitle,
      activityType: 1,
      location: '鍏变韩娲诲姩娴嬭瘯鍦扮偣',
      maxParticipants: 50,
      startTime: formatDateTime(tomorrow),
      endTime: formatDateTime(tomorrowPlusTwo),
      registrationStartTime: formatDateTime(oneHourAgo),
      registrationEndTime: formatDateTime(oneHourLater),
      content: '璺ㄧ鎴峰叡浜椿鍔ㄨ嚜鍔ㄥ寲娴嬭瘯鍐呭',
      isCrossTenant: 1,
    },
  });
  expect(activityRes.ok()).toBeTruthy();
  const activityBody = await activityRes.json();
  const activityId = String(activityBody.data);

  const competitionRes = await request.post(`${API_BASE}/api/competition`, {
    headers,
    data: {
      title: competitionTitle,
      description: '璺ㄧ鎴峰叡浜瘮璧涜嚜鍔ㄥ寲娴嬭瘯',
      competitionType: 'coding',
      level: 'school',
      organizer: '绉熸埛1绀惧洟',
      registrationStart: formatDateTime(oneHourAgo),
      registrationEnd: formatDateTime(oneHourLater),
      competitionStart: formatDateTime(tomorrow),
      competitionEnd: formatDateTime(tomorrowPlusTwo),
      maxParticipants: 30,
      isPublished: true,
      isCrossTenant: true,
    },
  });
  expect(competitionRes.ok()).toBeTruthy();
  const competitionBody = await competitionRes.json();
  const competitionId = String(competitionBody.data);

  const approveCompetitionRes = await request.post(`${API_BASE}/api/competition/${competitionId}/review/teacher`, {
    headers: {
      Authorization: `Bearer ${teacherLogin.token}`,
      'X-Tenant-Id': teacherLogin.organizationId,
      'Content-Type': 'application/json',
    },
    data: {
      approved: true,
      reviewComment: 'cross tenant content approval'
    }
  });
  expect(approveCompetitionRes.ok()).toBeTruthy();
  const approveCompetitionBody = await approveCompetitionRes.json();
  expect(approveCompetitionBody.code).toBe(200);

  const newsRes = await request.post(`${API_BASE}/api/news/add`, {
    headers,
    data: {
      title: newsTitle,
      category: 'club-news',
      author: 'manager_a',
      content: '璺ㄧ鎴峰叡浜柊闂昏嚜鍔ㄥ寲娴嬭瘯',
      isPublished: 1,
      isFeatured: 0,
      isCrossTenant: 1,
    },
  });
  expect(newsRes.ok()).toBeTruthy();

  const memberContext = await browser.newContext();
  const memberPage = await memberContext.newPage();
  const memberLogin = await loginApi(request, TENANT2_ADMIN);
  await applyLogin(memberPage, memberLogin);
  await memberPage.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
  const memberAuth = await memberPage.evaluate(() => ({
    token: localStorage.getItem('token'),
    tenantId: localStorage.getItem('tenantId')
  }));

  const detailResponse = memberPage.waitForResponse((response) =>
    response.url().includes(`/api/activity/${activityId}`) && response.request().method() === 'GET' && response.status() === 200
  );
  await memberPage.goto(`${BASE_URL}/activities/${activityId}`, { waitUntil: 'domcontentloaded' });
  await detailResponse;
  const registerResponse = await memberPage.request.post(`${API_BASE}/api/activity/${activityId}/register`, {
    headers: {
      Authorization: `Bearer ${memberAuth.token}`,
      'X-Tenant-Id': memberAuth.tenantId
    }
  });
  const registerBody = await registerResponse.json();
  expect(registerBody.code).toBe(200);

  await memberPage.goto(`${BASE_URL}/competition`, { waitUntil: 'domcontentloaded' });
  await expect(memberPage.getByText(competitionTitle).first()).toBeVisible();

  await memberPage.goto(`${BASE_URL}/news`, { waitUntil: 'domcontentloaded' });
  await expect(memberPage.getByText(newsTitle).first()).toBeVisible();

  const cleanupHeaders = {
    Authorization: `Bearer ${managerLogin.token}`,
    'X-Tenant-Id': managerLogin.organizationId,
    'Content-Type': 'application/json',
  };
  const registrationsRes = await request.get(`${API_BASE}/api/activity/${activityId}/registrations`, {
    headers: cleanupHeaders,
  });
  expect(registrationsRes.ok()).toBeTruthy();
  const registrationsBody = await registrationsRes.json();
  expect(registrationsBody.code).toBe(200);
  const registrations = Array.isArray(registrationsBody.data) ? registrationsBody.data : [];
  expect(registrations.some((item) => String(item.userId || '') === String(memberLogin.userId))).toBeTruthy();

  await memberContext.close();
  await managerContext.close();
  await request.delete(`${API_BASE}/api/activity/delete/${activityId}`, { headers: cleanupHeaders }).catch(() => {});
  await request.delete(`${API_BASE}/api/competition/${competitionId}`, { headers: cleanupHeaders }).catch(() => {});
  const newsList = await request.get(`${API_BASE}/api/news?search=${encodeURIComponent(newsTitle)}`, { headers: cleanupHeaders }).catch(() => null);
  if (newsList) {
    const newsBody = await newsList.json().catch(() => null);
    const newsId = newsBody?.data?.records?.[0]?.id || newsBody?.data?.[0]?.id;
    if (newsId) {
      await request.delete(`${API_BASE}/api/news/delete/${newsId}`, { headers: cleanupHeaders }).catch(() => {});
    }
  }
});
