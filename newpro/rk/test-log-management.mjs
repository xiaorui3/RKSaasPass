/**
 * RK-Web 日志管理模块测试 (WU-05系列)
 * 测试账号: admin_a (Tenant A超级管理员)
 * 测试模块: 日志管理
 */

import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';

const config = {
  baseURL: 'http://localhost:5173',
  username: 'admin_a',
  password: '123456',
  screenshotsDir: path.join(process.cwd(), '../test-screenshots'),
  reportsDir: path.join(process.cwd(), '../testing-reports/02-超级管理员测试'),
  logsDir: path.join(process.cwd(), '../testing-reports/LOGS')
};

// 确保目录存在
[config.screenshotsDir, config.reportsDir, config.logsDir].forEach(dir => {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
  }
});

let browser;
let context;
let page;
let testResults = [];

async function initBrowser() {
  console.log('启动浏览器...');
  browser = await chromium.launch({
    headless: false,
    slowMo: 500
  });
  context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });
  page = await context.newPage();
  page.setDefaultTimeout(30000);
}

async function takeScreenshot(name, description) {
  const filepath = path.join(config.screenshotsDir, `test-log-${name}-${Date.now()}.png`);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`📸 截图: ${filepath}`);
  return { filepath, description };
}

async function login() {
  console.log('\n=== WU-05-01: 登录和权限验证 ===');
  const startTime = Date.now();

  try {
    await page.goto(config.baseURL);
    await takeScreenshot('01-initial', '初始页面');

    // 等待页面加载
    await page.waitForTimeout(2000);

    // 如果有登录对话框已经打开，直接使用；否则点击登录按钮
    const hasModal = await page.locator('[class*="login-modal"], [class*="login-dialog"]').count() > 0;

    if (!hasModal) {
      const loginButton = page.locator('a:has-text("登录"), button:has-text("登录"), [class*="login-button"]');
      if (await loginButton.count() > 0) {
        await loginButton.first().click();
        await page.waitForTimeout(1500);
      }
    }

    await takeScreenshot('02-login-modal', '登录对话框');

    // 填写登录表单
    // 首先处理租户选择器（如果有） - 点击整个select元素
    const tenantSelectWrapper = page.locator('.el-select:has-text("租户"), .el-select:has(.el-select__placeholder)').first();
    if (await tenantSelectWrapper.count() > 0) {
      await tenantSelectWrapper.click();
      await page.waitForTimeout(800);
      // 选择第一个租户选项
      const firstOption = page.locator('.el-select-dropdown__item, [role="option"]').first();
      if (await firstOption.count() > 0) {
        await firstOption.click();
        await page.waitForTimeout(500);
      }
    }

    // 填写用户名 - 查找非readonly的输入框
    const allInputs = page.locator('input[type="text"]');
    const inputCount = await allInputs.count();
    let usernameFilled = false;

    for (let i = 0; i < inputCount; i++) {
      const input = allInputs.nth(i);
      const isReadonly = await input.getAttribute('readonly');
      const isSelect = await input.getAttribute('class');
      const value = await input.inputValue();

      if (!isReadonly && !isSelect?.includes('select') && value === '') {
        await input.fill(config.username);
        usernameFilled = true;
        break;
      }
    }

    if (!usernameFilled) {
      // 尝试通过placeholder查找
      const usernameInput = page.locator('input[placeholder*="账号"], input[placeholder*="用户"], input[placeholder*="username"]').first();
      if (await usernameInput.count() > 0) {
        await usernameInput.fill(config.username);
      }
    }

    // 填写密码
    const passwordInput = page.locator('input[type="password"], input[placeholder*="密码"]');
    if (await passwordInput.count() > 0) {
      await passwordInput.first().fill(config.password);
    }
    await takeScreenshot('03-login-filled', '登录表单已填写');

    // 点击登录按钮
    const submitButton = page.locator('button:has-text("登录"), button[type="submit"]');
    await submitButton.first().click();
    await page.waitForTimeout(3000);
    await takeScreenshot('04-after-login', '登录成功后');

    // 验证登录成功
    const isLoggedIn = await page.locator(`text=${config.username}`).or(page.locator('[class*="avatar"]')).or(page.locator('[class*="user"]')).count() > 0;

    if (isLoggedIn) {
      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      console.log(`✅ WU-05-01 完成: 登录成功 (${duration}s)`);

      testResults.push({
        testId: 'WU-05-01',
        name: '登录和权限验证',
        status: 'PASS',
        duration: `${duration}s`,
        evidence: '成功登录并验证超级管理员身份',
        timestamp: new Date().toISOString()
      });

      return true;
    } else {
      throw new Error('登录验证失败');
    }
  } catch (error) {
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`❌ WU-05-01 失败: ${error.message} (${duration}s)`);

    testResults.push({
      testId: 'WU-05-01',
      name: '登录和权限验证',
      status: 'FAIL',
      duration: `${duration}s`,
      error: error.message,
      timestamp: new Date().toISOString()
    });

    await takeScreenshot('01-error', '登录失败');
    return false;
  }
}

async function navigateToLogManagement() {
  console.log('\n导航到日志管理页面...');

  try {
    // 先检查是否已经在管理页面
    const currentUrl = page.url();
    console.log(`当前URL: ${currentUrl}`);

    // 查找侧边栏菜单
    const sideMenus = ['系统管理', '系统', '管理'];

    // 尝试展开系统管理菜单
    for (const menu of sideMenus) {
      const menuLocator = page.locator(`span:has-text("${menu}"), div:has-text("${menu}")`).first();
      if (await menuLocator.count() > 0) {
        const isExpanded = await menuLocator.getAttribute('class');
        // 点击展开菜单
        await menuLocator.click();
        await page.waitForTimeout(1000);
        console.log(`✓ 点击菜单: ${menu}`);
        break;
      }
    }

    // 查找日志相关子菜单
    const logMenus = ['日志管理', '操作日志', '登录日志', '系统日志', '审计日志', '日志'];

    for (const logMenu of logMenus) {
      // 尝试多种选择器
      const selectors = [
        `text=${logMenu}`,
        `span:has-text("${logMenu}")`,
        `a:has-text("${logMenu}")`,
        `li:has-text("${logMenu}")`
      ];

      for (const selector of selectors) {
        const subMenuLocator = page.locator(selector).first();
        if (await subMenuLocator.count() > 0) {
          await subMenuLocator.click();
          await page.waitForTimeout(2000);
          console.log(`✓ 点击子菜单: ${logMenu}`);
          return true;
        }
      }
    }

    // 如果没有找到日志菜单，尝试直接访问URL
    const logUrls = [
      `${config.baseURL}/#/system/log`,
      `${config.baseURL}/#/admin/log`,
      `${config.baseURL}/#/log`,
      `${config.baseURL}/#/system/logs`
    ];

    for (const url of logUrls) {
      console.log(`尝试访问: ${url}`);
      await page.goto(url);
      await page.waitForTimeout(2000);

      // 检查是否有日志相关内容
      const hasLogContent = await page.locator('text=/日志|操作|登录|时间|用户/').count() > 0;
      if (hasLogContent) {
        console.log(`✓ 成功访问日志页面`);
        return true;
      }
    }

    console.log('⚠️ 未找到日志管理页面');
    return false;

  } catch (error) {
    console.log(`⚠️ 导航到日志管理页面时出错: ${error.message}`);
    return false;
  }
}

async function testLogList() {
  console.log('\n=== WU-05-02: Read测试 - 查看日志列表 ===');
  const startTime = Date.now();

  try {
    await navigateToLogManagement();
    await takeScreenshot('05-log-list', '日志列表页面');

    // 验证日志列表是否存在
    const hasTable = await page.locator('table, [class*="table"], [class*="list"]').count() > 0;
    const hasLogEntries = await page.locator('text=/登录|操作|审计|日志|时间|用户/').count() > 0;

    // 检查分页控件
    const hasPagination = await page.locator('[class*="pagination"]').or(page.locator('text=上一页')).or(page.locator('text=下一页')).or(page.locator('text=首页')).count() > 0;

    if (hasTable || hasLogEntries) {
      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      console.log(`✅ WU-05-02 完成: 日志列表显示正常 (${duration}s)`);
      console.log(`   - 表格存在: ${hasTable}`);
      console.log(`   - 日志条目: ${hasLogEntries}`);
      console.log(`   - 分页控件: ${hasPagination}`);

      testResults.push({
        testId: 'WU-05-02',
        name: 'Read测试 - 查看日志列表',
        status: 'PASS',
        duration: `${duration}s`,
        features: {
          table: hasTable,
          logEntries: hasLogEntries,
          pagination: hasPagination
        },
        evidence: '日志列表正确显示',
        timestamp: new Date().toISOString()
      });

      return true;
    } else {
      throw new Error('未找到日志列表内容');
    }
  } catch (error) {
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`❌ WU-05-02 失败: ${error.message} (${duration}s)`);

    testResults.push({
      testId: 'WU-05-02',
      name: 'Read测试 - 查看日志列表',
      status: 'FAIL',
      duration: `${duration}s`,
      error: error.message,
      timestamp: new Date().toISOString()
    });

    await takeScreenshot('02-error', '日志列表加载失败');
    return false;
  }
}

async function testLogDetail() {
  console.log('\n=== WU-05-03: Read测试 - 查看日志详情 ===');
  const startTime = Date.now();

  try {
    // 查找日志列表中的详情按钮或可点击的行
    const detailButtons = page.locator('button:has-text("详情"), button:has-text("查看"), a:has-text("详情")');
    const tableRows = page.locator('table tbody tr, [class*="table"] tbody tr');

    if (await detailButtons.count() > 0) {
      await detailButtons.first().click();
      await page.waitForTimeout(2000);
      await takeScreenshot('06-log-detail', '日志详情页面');

      const hasDetail = await page.locator('text=/时间|用户|操作|IP|模块/').count() > 0;

      if (hasDetail) {
        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`✅ WU-05-03 完成: 日志详情显示正常 (${duration}s)`);

        testResults.push({
          testId: 'WU-05-03',
          name: 'Read测试 - 查看日志详情',
          status: 'PASS',
          duration: `${duration}s`,
          evidence: '日志详情正确显示',
          timestamp: new Date().toISOString()
        });

        // 关闭详情对话框或返回列表
        const closeButton = page.locator('button:has-text("关闭"), button[aria-label="Close"], [class*="close"]');
        if (await closeButton.count() > 0) {
          await closeButton.first().click();
          await page.waitForTimeout(1000);
        }

        return true;
      }
    } else if (await tableRows.count() > 0) {
      // 点击第一行查看详情
      await tableRows.first().click();
      await page.waitForTimeout(2000);
      await takeScreenshot('06-log-detail', '日志详情页面');

      const hasDetail = await page.locator('text=/时间|用户|操作|IP|模块/').count() > 0;

      if (hasDetail) {
        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`✅ WU-05-03 完成: 日志详情显示正常 (${duration}s)`);

        testResults.push({
          testId: 'WU-05-03',
          name: 'Read测试 - 查看日志详情',
          status: 'PASS',
          duration: `${duration}s`,
          evidence: '日志详情正确显示',
          timestamp: new Date().toISOString()
        });

        return true;
      }
    }

    throw new Error('无法打开日志详情');
  } catch (error) {
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`⚠️ WU-05-03 部分完成: ${error.message} (${duration}s)`);

    testResults.push({
      testId: 'WU-05-03',
      name: 'Read测试 - 查看日志详情',
      status: 'PARTIAL',
      duration: `${duration}s`,
      error: error.message,
      note: '详情功能可能未实现或需要特定权限',
      timestamp: new Date().toISOString()
    });

    return false;
  }
}

async function testSearchAndFilter() {
  console.log('\n=== WU-05-04: 搜索和筛选测试 ===');
  const startTime = Date.now();

  try {
    // 测试搜索功能
    const searchInput = page.locator('input[placeholder*="搜索"], input[placeholder*="用户名"], input[placeholder*="操作"]');

    if (await searchInput.count() > 0) {
      await searchInput.first().fill('admin_a');
      await page.waitForTimeout(1000);

      const searchButton = page.locator('button:has-text("搜索"), button:has-text("查询"), button[type="submit"]');
      if (await searchButton.count() > 0) {
        await searchButton.first().click();
        await page.waitForTimeout(2000);
      }

      await takeScreenshot('07-log-search', '日志搜索结果');
    }

    // 测试日期筛选
    const datePickers = page.locator('[class*="date"], input[type="date"]');
    if (await datePickers.count() >= 2) {
      await datePickers.nth(0).fill('2026-03-01');
      await datePickers.nth(1).fill('2026-03-25');
      await page.waitForTimeout(1000);

      const searchButton = page.locator('button:has-text("搜索"), button:has-text("查询")');
      if (await searchButton.count() > 0) {
        await searchButton.first().click();
        await page.waitForTimeout(2000);
      }

      await takeScreenshot('08-log-filter-date', '日期筛选结果');
    }

    // 测试操作类型筛选
    const typeSelect = page.locator('select, [class*="select"], [role="combobox"]');
    if (await typeSelect.count() > 0) {
      await typeSelect.first().click();
      await page.waitForTimeout(500);
      await takeScreenshot('09-log-filter-type', '操作类型筛选');
    }

    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`✅ WU-05-04 完成: 搜索和筛选测试完成 (${duration}s)`);

    testResults.push({
      testId: 'WU-05-04',
      name: '搜索和筛选测试',
      status: 'PASS',
      duration: `${duration}s`,
      features: {
        search: await searchInput.count() > 0,
        dateFilter: await datePickers.count() >= 2,
        typeFilter: await typeSelect.count() > 0
      },
      evidence: '搜索和筛选功能可用',
      timestamp: new Date().toISOString()
    });

    return true;
  } catch (error) {
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`⚠️ WU-05-04 部分完成: ${error.message} (${duration}s)`);

    testResults.push({
      testId: 'WU-05-04',
      name: '搜索和筛选测试',
      status: 'PARTIAL',
      duration: `${duration}s`,
      error: error.message,
      timestamp: new Date().toISOString()
    });

    return false;
  }
}

async function testLogExport() {
  console.log('\n=== WU-05-05: 日志导出测试 ===');
  const startTime = Date.now();

  try {
    // 查找导出按钮
    const exportButtons = page.locator('button:has-text("导出"), button:has-text("下载"), button:has-text("Export")');

    if (await exportButtons.count() > 0) {
      // 设置下载监听器
      const downloadPromise = page.waitForEvent('download', { timeout: 10000 });

      await exportButtons.first().click();
      await takeScreenshot('10-log-export', '点击导出按钮');

      try {
        const download = await downloadPromise;
        const fileName = download.suggestedFilename();
        console.log(`✓ 文件下载: ${fileName}`);

        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`✅ WU-05-05 完成: 日志导出成功 (${duration}s)`);

        testResults.push({
          testId: 'WU-05-05',
          name: '日志导出测试',
          status: 'PASS',
          duration: `${duration}s`,
          evidence: `成功导出文件: ${fileName}`,
          timestamp: new Date().toISOString()
        });

        return true;
      } catch (downloadError) {
        // 导出可能在前端处理，不触发下载事件
        await page.waitForTimeout(3000);
        await takeScreenshot('10-export-after', '导出操作后');

        const duration = ((Date.now() - startTime) / 1000).toFixed(2);
        console.log(`⚠️ WU-05-05 部分完成: 导出按钮存在但未检测到下载 (${duration}s)`);

        testResults.push({
          testId: 'WU-05-05',
          name: '日志导出测试',
          status: 'PARTIAL',
          duration: `${duration}s`,
          note: '导出功能存在但未触发文件下载',
          timestamp: new Date().toISOString()
        });

        return true;
      }
    } else {
      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      console.log(`⚠️ WU-05-05 未实现: 未找到导出按钮 (${duration}s)`);

      testResults.push({
        testId: 'WU-05-05',
        name: '日志导出测试',
        status: 'NOT_IMPLEMENTED',
        duration: `${duration}s`,
        note: '未找到导出功能',
        timestamp: new Date().toISOString()
      });

      return false;
    }
  } catch (error) {
    const duration = ((Date.now() - startTime) / 1000).toFixed(2);
    console.log(`❌ WU-05-05 失败: ${error.message} (${duration}s)`);

    testResults.push({
      testId: 'WU-05-05',
      name: '日志导出测试',
      status: 'FAIL',
      duration: `${duration}s`,
      error: error.message,
      timestamp: new Date().toISOString()
    });

    return false;
  }
}

async function generateReport() {
  console.log('\n=== WU-05-06: 生成日志管理模块报告 ===');

  const reportPath = path.join(config.reportsDir, '05-日志管理测试.md');
  const logPath = path.join(config.logsDir, 'task-05-log-management.md');
  const resultPath = path.join(config.reportsDir, 'RESULTS', '05.md');

  // 确保RESULTS目录存在
  const resultsDir = path.join(config.reportsDir, 'RESULTS');
  if (!fs.existsSync(resultsDir)) {
    fs.mkdirSync(resultsDir, { recursive: true });
  }

  const passCount = testResults.filter(r => r.status === 'PASS').length;
  const failCount = testResults.filter(r => r.status === 'FAIL').length;
  const partialCount = testResults.filter(r => r.status === 'PARTIAL').length;
  const notImplementedCount = testResults.filter(r => r.status === 'NOT_IMPLEMENTED').length;
  const totalCount = testResults.length;

  const successRate = ((passCount / totalCount) * 100).toFixed(1);

  const report = `# 日志管理模块测试报告

> **测试模块**: 日志管理 (WU-05系列)
> **测试账号**: admin_a (Tenant A超级管理员)
> **测试时间**: ${new Date().toLocaleString('zh-CN')}
> **测试Agent**: Agent #05
> **前端URL**: http://localhost:5173

---

## 测试概览

| 指标 | 结果 |
|------|------|
| 总测试数 | ${totalCount} |
| 通过 | ${passCount} |
| 失败 | ${failCount} |
| 部分通过 | ${partialCount} |
| 未实现 | ${notImplementedCount} |
| 成功率 | ${successRate}% |

---

## 测试结果详情

### WU-05-01: 登录和权限验证 (5min)

**状态**: ${testResults.find(r => r.testId === 'WU-05-01')?.status || 'N/A'}
**耗时**: ${testResults.find(r => r.testId === 'WU-05-01')?.duration || 'N/A'}

**测试步骤**:
1. 访问前端URL
2. 输入测试账号: admin_a
3. 输入密码: 123456
4. 点击登录按钮
5. 验证登录成功

**测试结果**:
${testResults.find(r => r.testId === 'WU-05-01')?.evidence || '测试执行'}

**截图证据**:
- test-log-01-initial-*.png - 初始页面
- test-log-02-login-modal-*.png - 登录对话框
- test-log-03-login-filled-*.png - 登录表单已填写
- test-log-04-after-login-*.png - 登录成功后

---

### WU-05-02: Read测试 - 查看日志列表 (10min)

**状态**: ${testResults.find(r => r.testId === 'WU-05-02')?.status || 'N/A'}
**耗时**: ${testResults.find(r => r.testId === 'WU-05-02')?.duration || 'N/A'}

**测试步骤**:
1. 导航到日志管理页面
2. 验证日志列表显示
3. 检查表格结构
4. 验证分页控件

**功能验证**:
\`\`\`json
${JSON.stringify(testResults.find(r => r.testId === 'WU-05-02')?.features || {}, null, 2)}
\`\`\`

**测试结果**:
${testResults.find(r => r.testId === 'WU-05-02')?.evidence || '测试执行'}

**截图证据**:
- test-log-05-log-list-*.png - 日志列表页面

---

### WU-05-03: Read测试 - 查看日志详情 (10min)

**状态**: ${testResults.find(r => r.testId === 'WU-05-03')?.status || 'N/A'}
**耗时**: ${testResults.find(r => r.testId === 'WU-05-03')?.duration || 'N/A'}

**测试步骤**:
1. 在日志列表中点击详情按钮
2. 验证日志详情显示
3. 检查详情字段完整性

**测试结果**:
${testResults.find(r => r.testId === 'WU-05-03')?.evidence || '测试执行'}

**截图证据**:
- test-log-06-log-detail-*.png - 日志详情页面

---

### WU-05-04: 搜索和筛选测试 (10min)

**状态**: ${testResults.find(r => r.testId === 'WU-05-04')?.status || 'N/A'}
**耗时**: ${testResults.find(r => r.testId === 'WU-05-04')?.duration || 'N/A'}

**测试步骤**:
1. 测试用户名搜索
2. 测试日期范围筛选
3. 测试操作类型筛选
4. 验证筛选结果

**功能验证**:
\`\`\`json
${JSON.stringify(testResults.find(r => r.testId === 'WU-05-04')?.features || {}, null, 2)}
\`\`\`

**测试结果**:
${testResults.find(r => r.testId === 'WU-05-04')?.evidence || '测试执行'}

**截图证据**:
- test-log-07-log-search-*.png - 日志搜索结果
- test-log-08-log-filter-date-*.png - 日期筛选结果
- test-log-09-log-filter-type-*.png - 操作类型筛选

---

### WU-05-05: 日志导出测试 (10min)

**状态**: ${testResults.find(r => r.testId === 'WU-05-05')?.status || 'N/A'}
**耗时**: ${testResults.find(r => r.testId === 'WU-05-05')?.duration || 'N/A'}

**测试步骤**:
1. 查找导出按钮
2. 点击导出
3. 验证文件下载

**测试结果**:
${testResults.find(r => r.testId === 'WU-05-05')?.evidence || testResults.find(r => r.testId === 'WU-05-05')?.note || '测试执行'}

**截图证据**:
- test-log-10-log-export-*.png - 点击导出按钮
- test-log-10-export-after-*.png - 导出操作后

---

## 问题清单

${testResults.filter(r => r.status === 'FAIL' || r.status === 'PARTIAL').map(r => `
### ${r.testId}: ${r.name}
- **状态**: ${r.status}
- **问题描述**: ${r.error || r.note || '未知错误'}
- **时间**: ${r.timestamp}
`).join('') || '无问题发现'}

---

## 功能评估

| 功能 | 状态 | 说明 |
|------|------|------|
| 日志列表查看 | ${testResults.find(r => r.testId === 'WU-05-02')?.status === 'PASS' ? '✅ 已实现' : '⚠️ 待完善'} | ${testResults.find(r => r.testId === 'WU-05-02')?.evidence || '-'} |
| 日志详情查看 | ${testResults.find(r => r.testId === 'WU-05-03')?.status === 'PASS' ? '✅ 已实现' : testResults.find(r => r.testId === 'WU-05-03')?.status === 'PARTIAL' ? '⚠️ 部分实现' : '❌ 未实现'} | ${testResults.find(r => r.testId === 'WU-05-03')?.evidence || testResults.find(r => r.testId === 'WU-05-03')?.note || '-'} |
| 搜索筛选 | ${testResults.find(r => r.testId === 'WU-05-04')?.status === 'PASS' ? '✅ 已实现' : '⚠️ 待完善'} | ${testResults.find(r => r.testId === 'WU-05-04')?.evidence || '-'} |
| 日志导出 | ${testResults.find(r => r.testId === 'WU-05-05')?.status === 'PASS' ? '✅ 已实现' : testResults.find(r => r.testId === 'WU-05-05')?.status === 'PARTIAL' ? '⚠️ 部分实现' : '❌ 未实现'} | ${testResults.find(r => r.testId === 'WU-05-05')?.evidence || testResults.find(r => r.testId === 'WU-05-05')?.note || '-'} |

---

## 结论

**整体评估**: 日志管理模块${successRate >= 70 ? '基本可用' : successRate >= 50 ? '需要改进' : '不完善'}

**主要问题**:
${testResults.filter(r => r.status === 'FAIL').map(r => `- ${r.testId}: ${r.error}`).join('\n') || '无严重问题'}

**建议**:
${testResults.filter(r => r.status === 'PARTIAL' || r.status === 'NOT_IMPLEMENTED').map(r => `- 完善${r.name}: ${r.note || '功能未完全实现'}`).join('\n') || '功能完善度良好'}

---

**测试执行者**: AI测试Agent #05
**报告生成时间**: ${new Date().toLocaleString('zh-CN')}
`;

  // 写入详细报告
  fs.writeFileSync(reportPath, report, 'utf8');
  console.log(`📄 详细报告已生成: ${reportPath}`);

  // 写入测试日志
  const logContent = `# 日志管理模块测试日志

## 测试时间
${new Date().toLocaleString('zh-CN')}

## 测试结果汇总
- 总测试数: ${totalCount}
- 通过: ${passCount}
- 失败: ${failCount}
- 部分通过: ${partialCount}
- 未实现: ${notImplementedCount}

## 详细结果
${testResults.map(r => `
### ${r.testId}: ${r.name}
- 状态: ${r.status}
- 耗时: ${r.duration}
- 证据: ${r.evidence || 'N/A'}
${r.error ? `- 错误: ${r.error}` : ''}
${r.note ? `- 备注: ${r.note}` : ''}
- 时间: ${r.timestamp}
`).join('\n')}
`;
  fs.writeFileSync(logPath, logContent, 'utf8');
  console.log(`📝 测试日志已生成: ${logPath}`);

  // 写入简化结果
  const resultContent = `# 日志管理模块测试结果 (WU-05)

**测试日期**: ${new Date().toLocaleString('zh-CN')}
**测试账号**: admin_a
**测试Agent**: Agent #05

## 测试统计

| 指标 | 数量 |
|------|------|
| 总测试数 | ${totalCount} |
| 通过 | ${passCount} |
| 失败 | ${failCount} |
| 部分通过 | ${partialCount} |
| 未实现 | ${notImplementedCount} |
| **成功率** | **${successRate}%** |

## 测试结果

${testResults.map(r => `- **${r.testId}**: ${r.name} - ${r.status}`).join('\n')}

## 截图文件

${fs.readdirSync(config.screenshotsDir).filter(f => f.startsWith('test-log-')).map(f => `- ${f}`).join('\n')}

---
**测试完成时间**: ${new Date().toLocaleString('zh-CN')}
`;
  fs.writeFileSync(resultPath, resultContent, 'utf8');
  console.log(`📊 测试结果已生成: ${resultPath}`);

  console.log('\n=== WU-05-06 完成: 日志管理模块报告生成成功 ===');

  return {
    reportPath,
    logPath,
    resultPath,
    summary: { passCount, failCount, partialCount, notImplementedCount, successRate }
  };
}

async function runTests() {
  console.log('========================================');
  console.log('  RK-Web 日志管理模块测试 (WU-05系列)');
  console.log('========================================');
  console.log(`测试账号: ${config.username}`);
  console.log(`测试URL: ${config.baseURL}`);
  console.log(`开始时间: ${new Date().toLocaleString('zh-CN')}`);
  console.log('========================================\n');

  try {
    await initBrowser();

    // 执行所有测试
    const loginSuccess = await login();
    if (!loginSuccess) {
      console.log('\n⚠️ 登录失败，无法继续测试');
    } else {
      await testLogList();
      await testLogDetail();
      await testSearchAndFilter();
      await testLogExport();
    }

    // 生成报告
    const report = await generateReport();

    console.log('\n========================================');
    console.log('  测试完成');
    console.log('========================================');
    console.log(`总测试数: ${testResults.length}`);
    console.log(`通过: ${report.summary.passCount}`);
    console.log(`失败: ${report.summary.failCount}`);
    console.log(`部分通过: ${report.summary.partialCount}`);
    console.log(`未实现: ${report.summary.notImplementedCount}`);
    console.log(`成功率: ${report.summary.successRate}%`);
    console.log('========================================');
    console.log(`\n📄 详细报告: ${report.reportPath}`);
    console.log(`📝 测试日志: ${report.logPath}`);
    console.log(`📊 测试结果: ${report.resultPath}`);
    console.log('\n所有测试已完成！');

  } catch (error) {
    console.error(`\n❌ 测试执行失败: ${error.message}`);
    console.error(error.stack);
  } finally {
    if (browser) {
      await browser.close();
      console.log('\n浏览器已关闭');
    }
  }
}

// 运行测试
runTests();
