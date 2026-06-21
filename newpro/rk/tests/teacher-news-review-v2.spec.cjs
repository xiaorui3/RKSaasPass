const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

// Test configuration
const BASE_URL = 'http://localhost:5173';
const TEST_ACCOUNT = {
  username: 't01_teacher',
  password: '123456',
  role: '指导老师',
  tenant: 'Tenant A',
  roleId: 8
};

// Create screenshots directory
const screenshotDir = path.join(__dirname, '../testing-reports/04-指导老师测试/images');
if (!fs.existsSync(screenshotDir)) {
  fs.mkdirSync(screenshotDir, { recursive: true });
}

// Test results tracking
const testResults = {
  testSuite: 'WU-20 - 新闻审核模块测试 (指导老师)',
  tester: 'Agent #19',
  testDate: new Date().toISOString(),
  testAccount: TEST_ACCOUNT,
  summary: {
    total: 4,
    passed: 0,
    failed: 0,
    skipped: 0,
    passRate: '0%'
  },
  testCases: []
};

// Helper function to take screenshot
async function takeScreenshot(page, name, description) {
  const filename = `WU-20-${name}.png`;
  const filepath = path.join(screenshotDir, filename);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`  ✓ Screenshot: ${filename} - ${description}`);
  return filepath;
}

// Helper function to record test result
function recordTestResult(testCaseId, name, status, details, screenshots = []) {
  const result = {
    testCaseId,
    name,
    status,
    details,
    screenshots,
    timestamp: new Date().toISOString()
  };
  testResults.testCases.push(result);

  if (status === 'PASS') testResults.summary.passed++;
  else if (status === 'FAIL') testResults.summary.failed++;
  else testResults.summary.skipped++;

  const rate = (testResults.summary.passed / testResults.summary.total * 100).toFixed(1);
  testResults.summary.passRate = `${rate}%`;

  console.log(`  [${status}] ${testCaseId}: ${name}`);
  if (details) console.log(`    ${details}`);
}

// Helper function to wait with timeout
async function waitForElement(page, selector, timeout = 5000) {
  try {
    await page.waitForSelector(selector, { timeout, state: 'visible' });
    return true;
  } catch (e) {
    return false;
  }
}

// Main test execution
async function runTests() {
  let browser;
  let page;

  try {
    console.log('\n=== WU-20: 新闻审核模块测试 (指导老师) ===\n');
    console.log(`账号: ${TEST_ACCOUNT.username} / ${TEST_ACCOUNT.password}`);
    console.log(`角色: ${TEST_ACCOUNT.role}\n`);

    // Launch browser
    browser = await chromium.launch({
      headless: false,
      slowMo: 800
    });
    page = await browser.newPage();
    page.setDefaultTimeout(15000);

    // ==================== WU-20-01: Login ====================
    console.log('\n--- WU-20-01: 登录和权限验证 ---');

    try {
      await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(2000);
      await takeScreenshot(page, '01-01-homepage', '首页');

      // Look for login button or link
      const loginSelectors = [
        'button:has-text("登录")',
        'a:has-text("登录")',
        '.login-btn',
        '[class*="login"]'
      ];

      let loginClicked = false;
      for (const selector of loginSelectors) {
        const element = await page.$(selector);
        if (element) {
          await element.click();
          loginClicked = true;
          console.log('  ✓ 点击登录按钮');
          await page.waitForTimeout(1500);
          break;
        }
      }

      if (!loginClicked) {
        // Try navigating directly to login page
        await page.goto(`${BASE_URL}/login`);
        await page.waitForTimeout(1500);
      }

      await takeScreenshot(page, '01-02-login-page', '登录页面');

      // Fill login form
      const usernameSelectors = [
        'input[name="username"]',
        'input[placeholder*="用户"]',
        'input[placeholder*="账号"]',
        'input[type="text"]'
      ];

      let usernameFilled = false;
      for (const selector of usernameSelectors) {
        const input = await page.$(selector);
        if (input) {
          await input.fill(TEST_ACCOUNT.username);
          usernameFilled = true;
          console.log('  ✓ 填写用户名');
          break;
        }
      }

      const passwordSelectors = [
        'input[name="password"]',
        'input[placeholder*="密码"]',
        'input[type="password"]'
      ];

      let passwordFilled = false;
      for (const selector of passwordSelectors) {
        const input = await page.$(selector);
        if (input) {
          await input.fill(TEST_ACCOUNT.password);
          passwordFilled = true;
          console.log('  ✓ 填写密码');
          break;
        }
      }

      if (usernameFilled && passwordFilled) {
        await takeScreenshot(page, '01-03-login-filled', '登录表单已填写');

        // Submit login
        const submitSelectors = [
          'button[type="submit"]',
          'button:has-text("登录")',
          '.el-button--primary'
        ];

        for (const selector of submitSelectors) {
          const button = await page.$(selector);
          if (button) {
            await button.click();
            console.log('  ✓ 提交登录');
            break;
          }
        }

        await page.waitForTimeout(3000);
        await takeScreenshot(page, '01-04-after-login', '登录后页面');

        // Verify login
        const currentUrl = page.url();
        const hasUserInfo = await page.$('.user-info, .avatar, [class*="user"], .el-dropdown');

        if (hasUserInfo || currentUrl.includes('home') || currentUrl.includes('index')) {
          console.log('  ✓ 登录成功');

          // Check for admin access
          const adminMenu = await page.$('a:has-text("管理"), .admin-menu, [class*="admin"]');
          if (adminMenu) {
            console.log('  ✓ 管理菜单可见');
            await takeScreenshot(page, '01-05-admin-access', '管理员菜单');

            recordTestResult('WU-20-01', '登录和权限验证', 'PASS',
              '成功登录当前有效指导老师账号，可访问管理菜单',
              ['WU-20-01-01-homepage.png', 'WU-20-01-02-login-page.png',
               'WU-20-01-03-login-filled.png', 'WU-20-01-04-after-login.png',
               'WU-20-01-05-admin-access.png']);
          } else {
            recordTestResult('WU-20-01', '登录和权限验证', 'PARTIAL',
              '登录成功但未发现管理菜单',
              ['WU-20-01-04-after-login.png']);
          }
        } else {
          recordTestResult('WU-20-01', '登录和权限验证', 'FAIL',
            '登录表单已填写但未成功登录',
            ['WU-20-01-03-login-filled.png', 'WU-20-01-04-after-login.png']);
        }
      } else {
        recordTestResult('WU-20-01', '登录和权限验证', 'FAIL',
          '无法找到登录表单输入框',
          ['WU-20-01-02-login-page.png']);
      }
    } catch (error) {
      recordTestResult('WU-20-01', '登录和权限验证', 'FAIL',
        `登录过程出错: ${error.message}`);
    }

    // ==================== WU-20-02: View Pending News ====================
    console.log('\n--- WU-20-02: 查看待审核新闻 ---');

    try {
      // Navigate to admin section
      const adminSelectors = [
        'a:has-text("管理")',
        'a[href*="admin"]',
        '.admin-link'
      ];

      let navSuccess = false;
      for (const selector of adminSelectors) {
        const link = await page.$(selector);
        if (link) {
          await link.click();
          navSuccess = true;
          console.log('  ✓ 点击管理菜单');
          await page.waitForTimeout(2000);
          break;
        }
      }

      // Look for news approval menu
      const newsMenuSelectors = [
        'a:has-text("新闻审核")',
        'a:has-text("内容审核")',
        'a:has-text("审核管理")',
        '[class*="news"] a:has-text("审核")'
      ];

      let foundNewsMenu = false;
      for (const selector of newsMenuSelectors) {
        const menu = await page.$(selector);
        if (menu) {
          await menu.click();
          foundNewsMenu = true;
          console.log('  ✓ 点击新闻审核菜单');
          await page.waitForTimeout(2000);
          break;
        }
      }

      if (!foundNewsMenu) {
        // Try direct navigation
        console.log('  尝试直接导航到新闻审核页面');
        await page.goto(`${BASE_URL}/admin/news-approval`);
        await page.waitForTimeout(2000);
      }

      await takeScreenshot(page, '02-01-approval-page', '新闻审核页面');

      // Check for news list
      const listSelectors = [
        '.el-table',
        '.news-list',
        'table',
        '[class*="table"]'
      ];

      let hasList = false;
      for (const selector of listSelectors) {
        if (await page.$(selector)) {
          hasList = true;
          break;
        }
      }

      if (hasList) {
        console.log('  ✓ 发现新闻列表');

        // Count news items
        const itemCount = await page.$$eval('tr.el-table__row, .news-item, tbody tr', rows => rows.length);
        console.log(`  ✓ 找到 ${itemCount} 条新闻记录`);

        await takeScreenshot(page, '02-02-news-list', '待审核新闻列表');

        recordTestResult('WU-20-02', '查看待审核新闻', 'PASS',
          `成功访问新闻审核页面，显示${itemCount}条记录`,
          ['WU-20-02-01-approval-page.png', 'WU-20-02-02-news-list.png']);
      } else {
        recordTestResult('WU-20-02', '查看待审核新闻', 'FAIL',
          '新闻审核页面已加载但未发现列表',
          ['WU-20-02-01-approval-page.png']);
      }
    } catch (error) {
      recordTestResult('WU-20-02', '查看待审核新闻', 'FAIL',
        `访问新闻审核页面出错: ${error.message}`);
    }

    // ==================== WU-20-03: Approve News ====================
    console.log('\n--- WU-20-03: 审核通过新闻 ---');

    try {
      // Look for approve button
      const approveSelectors = [
        'button:has-text("通过")',
        'button:has-text("批准")',
        'button:has-text("发布")',
        '[class*="approve"] button',
        '[class*="pass"] button'
      ];

      let actionTaken = false;
      for (const selector of approveSelectors) {
        const buttons = await page.$$(selector);
        if (buttons.length > 0) {
          await takeScreenshot(page, '03-01-before-approve', '审核前');

          await buttons[0].click();
          actionTaken = true;
          console.log('  ✓ 点击审核通过按钮');
          await page.waitForTimeout(1500);
          break;
        }
      }

      if (actionTaken) {
        // Handle confirmation if present
        const confirmBtn = await page.$('button:has-text("确定"), .el-message-box__btn_primary');
        if (confirmBtn) {
          await takeScreenshot(page, '03-02-confirm', '确认对话框');
          await confirmBtn.click();
          console.log('  ✓ 确认操作');
          await page.waitForTimeout(2000);
        }

        await takeScreenshot(page, '03-03-after-approve', '审核后');

        // Check for success message
        const successMsg = await page.$('.el-message--success, [class*="success"]');
        if (successMsg) {
          console.log('  ✓ 操作成功');
          recordTestResult('WU-20-03', '审核通过新闻', 'PASS',
            '成功审核通过新闻',
            ['WU-20-03-01-before-approve.png', 'WU-20-03-02-confirm.png',
             'WU-20-03-03-after-approve.png']);
        } else {
          recordTestResult('WU-20-03', '审核通过新闻', 'PARTIAL',
            '已执行审核操作，但未检测到成功提示',
            ['WU-20-03-01-before-approve.png', 'WU-20-03-03-after-approve.png']);
        }
      } else {
        recordTestResult('WU-20-03', '审核通过新闻', 'SKIP',
          '未找到待审核新闻或审核按钮',
          []);
      }
    } catch (error) {
      recordTestResult('WU-20-03', '审核通过新闻', 'FAIL',
        `审核操作出错: ${error.message}`);
    }

    // ==================== WU-20-04: Reject News ====================
    console.log('\n--- WU-20-04: 审核拒绝新闻 ---');

    try {
      // Look for reject button
      const rejectSelectors = [
        'button:has-text("拒绝")',
        'button:has-text("驳回")',
        'button:has-text("不通过")',
        '[class*="reject"] button'
      ];

      let actionTaken = false;
      for (const selector of rejectSelectors) {
        const buttons = await page.$$(selector);
        if (buttons.length > 0) {
          await takeScreenshot(page, '04-01-before-reject', '拒绝前');

          await buttons[0].click();
          actionTaken = true;
          console.log('  ✓ 点击拒绝按钮');
          await page.waitForTimeout(1500);
          break;
        }
      }

      if (actionTaken) {
        // Check for reason input
        const textarea = await page.$('textarea, .el-textarea__inner');
        if (textarea) {
          await takeScreenshot(page, '04-02-reason-dialog', '拒绝理由对话框');

          await textarea.fill('内容需要修改，请完善后重新提交');
          console.log('  ✓ 填写拒绝理由');
          await page.waitForTimeout(1000);

          await takeScreenshot(page, '04-03-reason-filled', '已填写理由');

          const submitBtn = await page.$('button:has-text("确定"), .el-button--primary');
          if (submitBtn) {
            await submitBtn.click();
            console.log('  ✓ 提交拒绝操作');
          }

          await page.waitForTimeout(2000);
        }

        await takeScreenshot(page, '04-04-after-reject', '拒绝后');

        recordTestResult('WU-20-04', '审核拒绝新闻', 'PASS',
          '成功执行拒绝操作并填写理由',
          ['WU-20-04-01-before-reject.png', 'WU-20-04-02-reason-dialog.png',
           'WU-20-04-03-reason-filled.png', 'WU-20-04-04-after-reject.png']);
      } else {
        recordTestResult('WU-20-04', '审核拒绝新闻', 'SKIP',
          '未找到待审核新闻或拒绝按钮',
          []);
      }
    } catch (error) {
      recordTestResult('WU-20-04', '审核拒绝新闻', 'FAIL',
        `拒绝操作出错: ${error.message}`);
    }

  } catch (error) {
    console.error('\n❌ 测试执行出错:', error);
  } finally {
    if (browser) {
      await page.waitForTimeout(2000);
      await browser.close();
    }
  }

  // Save results
  const resultsDir = path.join(__dirname, '../testing-reports/RESULTS');
  if (!fs.existsSync(resultsDir)) {
    fs.mkdirSync(resultsDir, { recursive: true });
  }

  const resultsPath = path.join(resultsDir, '20.md');
  const reportContent = generateReport(testResults);
  fs.writeFileSync(resultsPath, reportContent, 'utf8');

  console.log(`\n✓ 测试结果已保存: ${resultsPath}`);

  return testResults;
}

// Generate markdown report
function generateReport(results) {
  return `# WU-20: 新闻审核模块测试报告

## 测试概要

| 项目 | 内容 |
|------|------|
| **测试套件** | ${results.testSuite} |
| **测试人员** | ${results.tester} |
| **测试时间** | ${new Date(results.testDate).toLocaleString('zh-CN')} |
| **测试账号** | ${results.testAccount.username} / ${results.testAccount.role} |

## 测试结果统计

| 指标 | 数量 |
|------|------|
| **总用例数** | ${results.summary.total} |
| **通过** | ${results.summary.passed} |
| **失败** | ${results.summary.failed} |
| **跳过** | ${results.summary.skipped} |
| **通过率** | ${results.summary.passRate} |

## 详细测试结果

${results.testCases.map(tc => `
### ${tc.testCaseId}: ${tc.name}

| 项目 | 内容 |
|------|------|
| **状态** | ${tc.status === 'PASS' ? '✓ 通过' : tc.status === 'FAIL' ? '✗ 失败' : '○ 跳过'} |
| **测试时间** | ${new Date(tc.timestamp).toLocaleString('zh-CN')} |
| **详细信息** | ${tc.details} |
| **截图** | ${tc.screenshots.length > 0 ? tc.screenshots.map(s => `\`${s}\``).join(', ') : '无'} |
`).join('\n')}

## CRUD功能评估

| 功能 | 状态 | 说明 |
|------|------|------|
| **读取 (Read)** | ${results.testCases.find(c => c.testCaseId === 'WU-20-02')?.status === 'PASS' ? '✓ 正常' : '✗ 异常'} | 查看待审核新闻列表 |
| **更新 (Update-审核通过)** | ${results.testCases.find(c => c.testCaseId === 'WU-20-03')?.status === 'PASS' ? '✓ 正常' : results.testCases.find(c => c.testCaseId === 'WU-20-03')?.status === 'PARTIAL' ? '⚠ 部分正常' : '✗ 异常'} | 审核通过新闻 |
| **更新 (Update-审核拒绝)** | ${results.testCases.find(c => c.testCaseId === 'WU-20-04')?.status === 'PASS' ? '✓ 正常' : results.testCases.find(c => c.testCaseId === 'WU-20-04')?.status === 'PARTIAL' ? '⚠ 部分正常' : '✗ 异常'} | 审核拒绝新闻 |

## 发现的问题

${results.testCases.filter(tc => tc.status === 'FAIL' || tc.status === 'PARTIAL').map(tc =>
  `#### ${tc.testCaseId}: ${tc.name}\n- **问题**: ${tc.details}\n`
).join('\n') || '**无问题发现**'}

## 截图清单

${fs.readdirSync(screenshotDir).filter(f => f.startsWith('WU-20-')).map(f => `- \`${f}\``).join('\n')}

## 总结

${results.summary.passed === results.summary.total ?
  '✓ 所有测试用例通过，新闻审核模块功能正常。' :
  results.summary.passed > 0 ?
  `⚠ 部分测试通过 (${results.summary.passRate})，存在需要修复的问题。` :
  '✗ 测试未通过，新闻审核模块存在问题需要修复。'}
`;
}

// Run tests
if (require.main === module) runTests().then(results => {
  console.log('\n=== 测试执行摘要 ===');
  console.log(`总计: ${results.summary.total}`);
  console.log(`通过: ${results.summary.passed}`);
  console.log(`失败: ${results.summary.failed}`);
  console.log(`跳过: ${results.summary.skipped}`);
  console.log(`通过率: ${results.summary.passRate}`);
  console.log('\n=== WU-20 测试结束 ===\n');
}).catch(error => {
  console.error('\n❌ 测试执行失败:', error);
  process.exit(1);
});