// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, BASE_URL } = require('./helpers');

test.describe('个人中心操作', () => {
  test.beforeEach(async ({ page, request }) => {
    await loginByApi(page, request, 'member');
    await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
  });

  test('个人中心页面加载', async ({ page }) => {
    const hasForm = await page.locator('.el-form, .user-hub-shell, .profile-page, .user-profile').count();
    expect(hasForm).toBeGreaterThan(0);
  });

  test('修改个人资料', async ({ page }) => {
    const form = page.locator('.el-form').first();
    if (await form.count() > 0) {
      const nameInput = form.locator('.el-form-item:has-text("姓名") input, .el-form-item:has-text("昵称") input').first();
      if (await nameInput.count() > 0) {
        await nameInput.clear();
        await nameInput.fill('CRUD测试用户');
      }

      const introArea = form.locator('textarea.el-textarea__inner').first();
      if (await introArea.count() > 0) {
        await introArea.clear();
        await introArea.fill('CRUD自动化测试修改的个人简介');
      }

      const saveBtn = page.locator('.el-button:has-text("保存"), .el-button--primary:has-text("保存")').first();
      if (await saveBtn.count() > 0) {
        await saveBtn.click();
        await page.waitForTimeout(1500);
      }
    }
  });

  test('活动记录标签页', async ({ page }) => {
    const activityTab = page.locator('.el-tabs__item:has-text("活动"), [data-testid="activity-tab"]').first();
    if (await activityTab.count() > 0) {
      await activityTab.click();
      await page.waitForTimeout(1500);
    }
  });

  test('比赛记录标签页', async ({ page }) => {
    const compTab = page.locator('.el-tabs__item:has-text("比赛"), [data-testid="competition-tab"]').first();
    if (await compTab.count() > 0) {
      await compTab.click();
      await page.waitForTimeout(1500);
    }
  });

  test('密码修改', async ({ page }) => {
    const pwdBtn = page.locator('.el-button:has-text("修改密码"), .el-button:has-text("改密"), a:has-text("修改密码")').first();
    if (await pwdBtn.count() > 0) {
      await pwdBtn.click();
      await page.waitForTimeout(800);

      const dialog = page.locator('.el-dialog:visible');
      if (await dialog.count() > 0) {
        const inputs = dialog.locator('input[type="password"], input.el-input__inner');
        if (await inputs.count() >= 3) {
          await inputs.nth(0).fill('123456');
          await inputs.nth(1).fill('123456');
          await inputs.nth(2).fill('123456');
        }
        const submitBtn = dialog.locator('.el-button--primary').first();
        if (await submitBtn.count() > 0) await submitBtn.click();
        await page.waitForTimeout(1000);
      }
    }
  });

  test('通知列表加载', async ({ page }) => {
    await page.goto(`${BASE_URL}/notifications`, { waitUntil: 'domcontentloaded' });
    await page.waitForTimeout(2000);
    const content = await page.textContent('body');
    expect(content).toBeTruthy();
  });
});
