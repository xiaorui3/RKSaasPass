// @ts-check
const { expect } = require('@playwright/test');
const { TENANT1_LIVE_TEACHER } = require('../helpers/test-users.cjs');

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173';
const API_URL = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';

const TEST_USERS = {
  admin: { username: 'admin_a', password: 'change-me', organizationId: 1 },
  manager: { username: 'manager_a', password: '123456', organizationId: 1 },
  member: { username: 'member_a', password: '123456', organizationId: 1 },
  teacher: { ...TENANT1_LIVE_TEACHER, organizationId: 1 },
};

async function loginByApi(page, request, userKey = 'admin') {
  const user = TEST_USERS[userKey];
  const resp = await request.post(`${API_URL}/auth/login`, {
    data: { username: user.username, password: user.password, organizationId: user.organizationId },
  });
  const body = await resp.json();
  if (body.code !== 200 && body.state !== 200) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`);
  }

  await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
  await page.evaluate((loginData) => {
    localStorage.setItem('token', loginData.token);
    localStorage.setItem('tenantId', String(loginData.organizationId));
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      organizationId: loginData.organizationId,
      roles: loginData.roles || [],
      userType: loginData.userType,
    }));
  }, body.data);
  return body.data;
}

async function gotoAdmin(page, path = '/admin') {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(1500);
}

async function waitForTable(page) {
  await page.waitForSelector('.el-table__body-wrapper', { timeout: 10000 });
  await page.waitForTimeout(500);
}

async function clickButton(page, text) {
  const btn = page.locator(`.el-button:has-text("${text}")`).first();
  await btn.waitFor({ state: 'visible', timeout: 5000 });
  await btn.click();
  await page.waitForTimeout(300);
}

async function fillForm(page, fields) {
  for (const [label, value] of Object.entries(fields)) {
    const formItem = page.locator(`.el-form-item:has(.el-form-item__label:has-text("${label}"))`);
    if (value === null || value === undefined) continue;

    const input = formItem.locator('input.el-input__inner').first();
    const textarea = formItem.locator('textarea.el-textarea__inner').first();
    const select = formItem.locator('.el-select').first();

    if (await input.count() > 0) {
      await input.fill(String(value));
    } else if (await textarea.count() > 0) {
      await textarea.fill(String(value));
    } else if (await select.count() > 0) {
      await select.click();
      await page.waitForTimeout(300);
      const option = page.locator(`.el-select-dropdown__item:has-text("${value}")`).first();
      await option.click();
      await page.waitForTimeout(200);
    }
  }
}

async function confirmDialog(page) {
  const confirm = page.locator('.el-message-box .el-button--primary').first();
  if (await confirm.count() > 0) {
    await confirm.click();
    await page.waitForTimeout(500);
  }
}

async function selectOption(page, selectLocator, optionText) {
  await selectLocator.click();
  await page.waitForTimeout(500);
  // Element Plus teleports dropdown to body
  const dropdown = page.locator('.el-select-dropdown:visible, .el-popper[aria-hidden="false"]').last();
  const option = dropdown.locator(`.el-select-dropdown__item:has-text("${optionText}")`).first();
  if (await option.count() > 0) {
    await option.click({ force: true });
  } else {
    // fallback: click any visible dropdown item
    const anyOption = page.locator('.el-select-dropdown__item:visible').first();
    if (await anyOption.count() > 0) await anyOption.click({ force: true });
  }
  await page.waitForTimeout(300);
}

async function submitFormDialog(page) {
  const dialog = page.locator('.el-dialog:visible').first();
  const submitBtn = dialog.locator('.el-button--primary:has-text("纭畾"), .el-button--primary:has-text("鎻愪氦"), .el-button--primary:has-text("淇濆瓨"), .el-button--primary:has-text("鍙戝竷"), .el-button--primary:has-text("淇濆瓨鑽夌")').first();
  await submitBtn.click();
  await page.waitForTimeout(1000);
}

async function dismissSuccessMessage(page) {
  await page.waitForTimeout(500);
  const close = page.locator('.el-message .el-message__closeBtn').first();
  if (await close.count() > 0) await close.click().catch(() => {});
}

async function apiCreate(request, endpoint, data, token) {
  const resp = await request.post(`${API_URL}${endpoint}`, {
    data,
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1', 'Content-Type': 'application/json' },
  });
  return resp.json();
}

async function apiDelete(request, endpoint, token) {
  const resp = await request.delete(`${API_URL}${endpoint}`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  });
  return resp.json();
}

async function apiGet(request, endpoint, token) {
  const resp = await request.get(`${API_URL}${endpoint}`, {
    headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': '1' },
  });
  return resp.json();
}

function ts() {
  return Date.now();
}

function uniqueName(prefix) {
  return `${prefix}-${ts()}`;
}

module.exports = {
  BASE_URL, API_URL, TEST_USERS,
  loginByApi, gotoAdmin, waitForTable, clickButton, selectOption,
  fillForm, confirmDialog, submitFormDialog, dismissSuccessMessage,
  apiCreate, apiDelete, apiGet, ts, uniqueName,
};
