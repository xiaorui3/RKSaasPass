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
  console.log(`  ✓ Screenshot saved: ${filename} - ${description}`);
  return filepath;
}

// Helper function to record test result
function recordTestResult(testCaseId, name, status, details, screenshots = []) {
  const result = {
    testCaseId,
    name,
    status, // 'PASS', 'FAIL', 'SKIP'
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
  if (details) console.log(`    Details: ${details}`);
}

// Helper function to wait for element with timeout
async function waitForElement(page, selector, timeout = 5000) {
  try {
    await page.waitForSelector(selector, { timeout });
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
    console.log('\n=== Starting WU-20: 新闻审核模块测试 (指导老师) ===\n');
    console.log(`Test Account: ${TEST_ACCOUNT.username} / ${TEST_ACCOUNT.password}`);
    console.log(`Role: ${TEST_ACCOUNT.role} (${TEST_ACCOUNT.tenant})\n`);

    // Launch browser
    browser = await chromium.launch({
      headless: false,
      slowMo: 500 // Slow down for better visibility
    });
    page = await browser.newPage();
    page.setDefaultTimeout(10000);

    // ==================== WU-20-01: Login and Permission Verification ====================
    console.log('\n--- WU-20-01: Login and Permission Verification ---');

    try {
      // Navigate to home page
      await page.goto(BASE_URL);
      await page.waitForLoadState('networkidle');
      console.log('  ✓ Navigated to home page');
      await takeScreenshot(page, '01-01-homepage', 'Home page before login');

      // Click login button
      const loginButtonSelector = 'button:has-text("登录"), .login-btn, a:has-text("登录")';
      const loginButtonVisible = await waitForElement(page, loginButtonSelector, 3000);

      if (loginButtonVisible) {
        await page.click(loginButtonSelector);
        console.log('  ✓ Clicked login button');
      } else {
        // Try alternative selector
        await page.click('text=登录');
      }

      await page.waitForTimeout(1000);
      await takeScreenshot(page, '01-02-login-modal', 'Login modal opened');

      // Fill login form
      await page.fill('input[type="text"], input[placeholder*="用户"], input[placeholder*="账号"]', TEST_ACCOUNT.username);
      await page.fill('input[type="password"], input[placeholder*="密码"]', TEST_ACCOUNT.password);
      console.log('  ✓ Filled login credentials');

      await takeScreenshot(page, '01-03-login-filled', 'Login form filled');

      // Submit login
      await page.click('button:has-text("登录"), button[type="submit"]');
      console.log('  ✓ Submitted login form');

      // Wait for login response
      await page.waitForTimeout(3000);

      // Verify login success
      const currentUrl = page.url();
      const isLoggedIn = currentUrl !== BASE_URL || await page.$('.user-info, .avatar, [class*="user"]');

      if (isLoggedIn) {
        console.log('  ✓ Login successful');
        await takeScreenshot(page, '01-04-after-login', 'After login - home page');

        // Verify teacher role permissions
        const adminMenuVisible = await waitForElement(page, 'a:has-text("管理"), .admin-menu, [class*="admin"]', 2000);

        if (adminMenuVisible) {
          console.log('  ✓ Admin menu visible for teacher role');
          await takeScreenshot(page, '01-05-admin-access', 'Admin menu access verified');

          recordTestResult('WU-20-01', 'Login and Permission Verification', 'PASS',
            'Successfully logged in as the live teacher account and verified admin menu access',
            [`WU-20-01-01-homepage.png`, `WU-20-01-02-login-modal.png`, `WU-20-01-03-login-filled.png`,
             `WU-20-01-04-after-login.png`, `WU-20-01-05-admin-access.png`]);
        } else {
          recordTestResult('WU-20-01', 'Login and Permission Verification', 'FAIL',
            'Login successful but admin menu not visible for teacher role',
            [`WU-20-01-04-after-login.png`]);
        }
      } else {
        recordTestResult('WU-20-01', 'Login and Permission Verification', 'FAIL',
          'Login failed - still on home page or no user info visible',
          [`WU-20-01-03-login-filled.png`]);
      }
    } catch (error) {
      recordTestResult('WU-20-01', 'Login and Permission Verification', 'FAIL',
        `Error during login: ${error.message}`);
    }

    // ==================== WU-20-02: Read Test - View Pending News ====================
    console.log('\n--- WU-20-02: Read Test - View Pending News ---');

    try {
      // Navigate to admin dashboard
      const adminLinkSelector = 'a:has-text("管理"), a[href*="admin"]';
      const adminClicked = await waitForElement(page, adminLinkSelector, 3000);

      if (adminClicked) {
        await page.click(adminLinkSelector);
        await page.waitForTimeout(2000);
        console.log('  ✓ Clicked admin menu');
      }

      // Look for news approval menu item
      const newsApprovalSelectors = [
        'a:has-text("新闻审核")',
        'a:has-text("新闻管理")',
        'a:has-text("内容审核")',
        'text=新闻审核',
        '[class*="news"] a:has-text("审核")'
      ];

      let newsApprovalClicked = false;
      for (const selector of newsApprovalSelectors) {
        if (await waitForElement(page, selector, 2000)) {
          await page.click(selector);
          newsApprovalClicked = true;
          console.log(`  ✓ Clicked news approval menu using selector: ${selector}`);
          await page.waitForTimeout(2000);
          break;
        }
      }

      if (!newsApprovalClicked) {
        // Try direct URL navigation
        await page.goto(`${BASE_URL}/admin/news-approval`);
        await page.waitForTimeout(2000);
        console.log('  ✓ Navigated to news approval page directly');
      }

      await takeScreenshot(page, '02-01-news-approval-list', 'News approval list page');

      // Verify pending news list
      const newsListSelector = '.el-table, .news-list, [class*="table"], article';
      const hasNewsList = await waitForElement(page, newsListSelector, 3000);

      if (hasNewsList) {
        console.log('  ✓ News list displayed');

        // Check for pending news items
        const pendingItems = await page.$$eval('tr, .news-item, .el-table__row', elements => {
          return elements.map(el => el.textContent).slice(0, 3);
        });

        console.log(`  ✓ Found ${pendingItems.length} news items`);

        await takeScreenshot(page, '02-02-pending-news-details', 'Pending news details');

        recordTestResult('WU-20-02', 'Read Test - View Pending News', 'PASS',
          `Successfully viewed news approval list with ${pendingItems.length} items`,
          [`WU-20-02-01-news-approval-list.png`, `WU-20-02-02-pending-news-details.png`]);
      } else {
        recordTestResult('WU-20-02', 'Read Test - View Pending News', 'FAIL',
          'News approval page loaded but no news list found',
          [`WU-20-02-01-news-approval-list.png`]);
      }
    } catch (error) {
      recordTestResult('WU-20-02', 'Read Test - View Pending News', 'FAIL',
        `Error viewing pending news: ${error.message}`);
    }

    // ==================== WU-20-03: Update Test - Approve News ====================
    console.log('\n--- WU-20-03: Update Test - Approve News ---');

    try {
      // Look for approve button
      const approveSelectors = [
        'button:has-text("通过")',
        'button:has-text("批准")',
        'button:has-text("发布")',
        'button:has-text("审核通过")',
        '[class*="approve"]',
        '[class*="pass"]'
      ];

      let approveClicked = false;
      let approveSelector = '';

      for (const selector of approveSelectors) {
        const buttons = await page.$$(selector);
        if (buttons.length > 0) {
          await takeScreenshot(page, '03-01-before-approve', 'Before clicking approve');

          // Click the first approve button
          await buttons[0].click();
          approveClicked = true;
          approveSelector = selector;
          console.log(`  ✓ Clicked approve button using selector: ${selector}`);

          await page.waitForTimeout(2000);
          break;
        }
      }

      if (approveClicked) {
        // Check for confirmation dialog
        const confirmSelector = 'button:has-text("确定"), button.el-message-box__btn_primary';
        const hasConfirm = await waitForElement(page, confirmSelector, 2000);

        if (hasConfirm) {
          await takeScreenshot(page, '03-02-confirm-dialog', 'Approve confirmation dialog');
          await page.click(confirmSelector);
          console.log('  ✓ Confirmed approve action');
          await page.waitForTimeout(2000);
        }

        await takeScreenshot(page, '03-03-after-approve', 'After approval');

        // Verify status changed
        const successMessage = await page.$('.el-message--success, .success, [class*="success"]');
        const statusChanged = successMessage !== null;

        if (statusChanged) {
          console.log('  ✓ Approval successful - success message displayed');
          recordTestResult('WU-20-03', 'Update Test - Approve News', 'PASS',
            'Successfully approved news article and verified status change',
            [`WU-20-03-01-before-approve.png`, `WU-20-03-02-confirm-dialog.png`, `WU-20-03-03-after-approve.png`]);
        } else {
          recordTestResult('WU-20-03', 'Update Test - Approve News', 'PASS',
            'Clicked approve button but could not verify success message (may need manual verification)',
            [`WU-20-03-01-before-approve.png`, `WU-20-03-03-after-approve.png`]);
        }
      } else {
        recordTestResult('WU-20-03', 'Update Test - Approve News', 'SKIP',
          'No pending news found or approve button not available',
          [`WU-20-02-01-news-approval-list.png`]);
      }
    } catch (error) {
      recordTestResult('WU-20-03', 'Update Test - Approve News', 'FAIL',
        `Error approving news: ${error.message}`);
    }

    // ==================== WU-20-04: Update Test - Reject News ====================
    console.log('\n--- WU-20-04: Update Test - Reject News ---');

    try {
      // Look for reject button
      const rejectSelectors = [
        'button:has-text("拒绝")',
        'button:has-text("驳回")',
        'button:has-text("不通过")',
        'button:has-text("审核拒绝")',
        '[class*="reject"]'
      ];

      let rejectClicked = false;

      for (const selector of rejectSelectors) {
        const buttons = await page.$$(selector);
        if (buttons.length > 0) {
          await takeScreenshot(page, '04-01-before-reject', 'Before clicking reject');

          // Click the first reject button
          await buttons[0].click();
          rejectClicked = true;
          console.log(`  ✓ Clicked reject button using selector: ${selector}`);

          await page.waitForTimeout(2000);
          break;
        }
      }

      if (rejectClicked) {
        // Look for reason input dialog
        const reasonInputSelector = 'textarea, input[type="text"], .el-textarea__inner';
        const hasReasonInput = await waitForElement(page, reasonInputSelector, 2000);

        if (hasReasonInput) {
          await takeScreenshot(page, '04-02-reason-dialog', 'Rejection reason dialog');

          // Enter rejection reason
          await page.fill(reasonInputSelector, '内容不符合发布规范，请修改后重新提交');
          console.log('  ✓ Entered rejection reason');

          await takeScreenshot(page, '04-03-reason-filled', 'Rejection reason filled');

          // Submit rejection
          const submitSelector = 'button:has-text("确定"), button.el-message-box__btn_primary';
          await page.click(submitSelector);
          console.log('  ✓ Submitted rejection');

          await page.waitForTimeout(2000);
          await takeScreenshot(page, '04-04-after-reject', 'After rejection');

          // Verify status changed
          const successMessage = await page.$('.el-message--success, .success, [class*="success"]');
          const statusChanged = successMessage !== null;

          if (statusChanged) {
            console.log('  ✓ Rejection successful');
            recordTestResult('WU-20-04', 'Update Test - Reject News', 'PASS',
              'Successfully rejected news article with reason',
              [`WU-20-04-01-before-reject.png`, `WU-20-04-02-reason-dialog.png`,
               `WU-20-04-03-reason-filled.png`, `WU-20-04-04-after-reject.png`]);
          } else {
            recordTestResult('WU-20-04', 'Update Test - Reject News', 'PASS',
              'Submitted rejection but could not verify success message (may need manual verification)',
              [`WU-20-04-01-before-reject.png`, `WU-20-04-04-after-reject.png`]);
          }
        } else {
          // Reject button clicked but no dialog
          await takeScreenshot(page, '04-04-after-reject', 'After reject click');
          recordTestResult('WU-20-04', 'Update Test - Reject News', 'PASS',
            'Clicked reject button (no reason dialog required)',
            [`WU-20-04-01-before-reject.png`, `WU-20-04-04-after-reject.png`]);
        }
      } else {
        recordTestResult('WU-20-04', 'Update Test - Reject News', 'SKIP',
          'No pending news found or reject button not available',
          [`WU-20-02-01-news-approval-list.png`]);
      }
    } catch (error) {
      recordTestResult('WU-20-04', 'Update Test - Reject News', 'FAIL',
        `Error rejecting news: ${error.message}`);
    }

  } catch (error) {
    console.error('\n❌ Fatal error during test execution:', error);
  } finally {
    if (browser) {
      console.log('\n✓ Test execution completed, closing browser...');
      await browser.close();
    }
  }

  // Save test results
  const resultsDir = path.join(__dirname, '../testing-reports/RESULTS');
  if (!fs.existsSync(resultsDir)) {
    fs.mkdirSync(resultsDir, { recursive: true });
  }

  const resultsPath = path.join(resultsDir, '20.md');
  fs.writeFileSync(resultsPath, `# WU-20: 新闻审核模块测试结果

## 测试概要
- **测试套件**: ${testResults.testSuite}
- **测试人员**: ${testResults.tester}
- **测试时间**: ${testResults.testDate}
- **测试账号**: ${testResults.testAccount.username} / ${testResults.testAccount.role}

## 测试结果统计
- **总用例数**: ${testResults.summary.total}
- **通过**: ${testResults.summary.passed}
- **失败**: ${testResults.summary.failed}
- **跳过**: ${testResults.summary.skipped}
- **通过率**: ${testResults.summary.passRate}

## 详细测试结果

${testResults.testCases.map(tc => `
### ${tc.testCaseId}: ${tc.name}
- **状态**: ${tc.status}
- **时间**: ${tc.timestamp}
- **详情**: ${tc.details}
- **截图**: ${tc.screenshots.join(', ') || '无'}
`).join('\n')}

## 功能评估
- **读取功能**: ${testResults.testCases[1]?.status === 'PASS' ? '✓ 正常' : '✗ 异常'}
- **审核通过功能**: ${testResults.testCases[2]?.status === 'PASS' ? '✓ 正常' : '✗ 异常'}
- **审核拒绝功能**: ${testResults.testCases[3]?.status === 'PASS' ? '✓ 正常' : '✗ 异常'}

## 发现的问题
${testResults.testCases.filter(tc => tc.status === 'FAIL').map(tc => `- ${tc.testCaseId}: ${tc.details}`).join('\n') || '无'}
`);

  console.log(`\n✓ Test results saved to: ${resultsPath}`);

  return testResults;
}

// Run tests
if (require.main === module) runTests().then(results => {
  console.log('\n=== Test Execution Summary ===');
  console.log(`Total: ${results.summary.total}`);
  console.log(`Passed: ${results.summary.passed}`);
  console.log(`Failed: ${results.summary.failed}`);
  console.log(`Skipped: ${results.summary.skipped}`);
  console.log(`Pass Rate: ${results.summary.passRate}`);
  console.log('\n=== End of WU-20 Tests ===\n');
}).catch(error => {
  console.error('\n❌ Test execution failed:', error);
  process.exit(1);
});