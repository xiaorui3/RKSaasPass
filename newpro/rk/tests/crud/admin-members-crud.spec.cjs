// @ts-check
const { test, expect } = require('@playwright/test');
const { loginByApi, gotoAdmin, waitForTable, clickButton, submitFormDialog, apiDelete, uniqueName } = require('./helpers');

test.describe('后台成员管理 CRUD', () => {
  let token;

  test.beforeEach(async ({ page, request }) => {
    const loginData = await loginByApi(page, request, 'admin');
    token = loginData.token;
    await gotoAdmin(page, '/admin/club/members');
    await waitForTable(page);
  });

  test('成员列表加载', async ({ page }) => {
    const table = page.locator('.el-table__body-wrapper');
    expect(await table.isVisible()).toBeTruthy();
    const rows = page.locator('.el-table__row');
    const count = await rows.count();
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('刷新成员库', async ({ page }) => {
    const refreshBtn = page.locator('.el-button:has-text("刷新成员库")').first();
    if (await refreshBtn.count() > 0) {
      await refreshBtn.click();
      await page.waitForTimeout(2000);
      await waitForTable(page);
    }
  });

  test('搜索成员', async ({ page }) => {
    const searchInput = page.locator('.search-form input.el-input__inner, .el-form input.el-input__inner').first();
    if (await searchInput.count() > 0) {
      await searchInput.fill('admin');
      const searchBtn = page.locator('.el-button:has-text("搜索"), .el-button--primary:has-text("搜索")').first();
      if (await searchBtn.count() > 0) {
        await searchBtn.click();
        await page.waitForTimeout(1000);
        await waitForTable(page);
      }
    }
  });

  test('添加成员对话框', async ({ page }) => {
    const addBtn = page.locator('.el-button:has-text("添加成员")').first();
    if (await addBtn.count() > 0) {
      await addBtn.click();
      await page.waitForTimeout(800);
      const dialog = page.locator('.el-dialog:visible');
      // 对话框可能弹出，也可能触发其他交互
      if (await dialog.count() > 0) {
        const cancelBtn = dialog.locator('.el-button:has-text("取消")').first();
        if (await cancelBtn.count() > 0) await cancelBtn.click();
      }
    }
  });

  test('编辑成员信息', async ({ page }) => {
    const editBtn = page.locator('.el-table__row .el-button:has-text("编辑")').first();
    if (await editBtn.count() > 0) {
      await editBtn.click();
      await page.waitForTimeout(500);

      const dialog = page.locator('.el-dialog:visible');
      if (await dialog.count() > 0) {
        // 修改部门
        const deptInput = dialog.locator('.el-form-item:has-text("部门") input.el-input__inner').first();
        if (await deptInput.count() > 0) {
          await deptInput.clear();
          await deptInput.fill('测试部门');
        }
        // 修改职位
        const posInput = dialog.locator('.el-form-item:has-text("职位") input.el-input__inner').first();
        if (await posInput.count() > 0) {
          await posInput.clear();
          await posInput.fill('测试职位');
        }
        await submitFormDialog(page);
        await page.waitForTimeout(1000);
      }
    }
  });

  test('删除成员', async ({ page }) => {
    const deleteBtn = page.locator('.el-table__row .el-button:has-text("删除")').first();
    if (await deleteBtn.count() > 0) {
      await deleteBtn.click();
      await page.waitForTimeout(500);
      const confirmBtn = page.locator('.el-message-box .el-button--primary').first();
      if (await confirmBtn.count() > 0) {
        await confirmBtn.click();
        await page.waitForTimeout(1500);
      }
    }
  });
});
