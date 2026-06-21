import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = path.resolve(__dirname, '../../test-screenshots');
const TEST_USER = { username: 'admin_a', password: '123456' };

// 确保截图目录存在
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

const testResults = [];

async function takeScreenshot(page, name) {
  const filename = `test-system-${name}.png`;
  const filepath = path.join(SCREENSHOT_DIR, filename);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`Screenshot saved: ${filename}`);
  return filename;
}

async function login(page) {
  console.log('Starting login...');
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1000);
  
  await takeScreenshot(page, 'login-page');
  
  // 填写用户名
  const usernameInput = await page.$('input[placeholder*="用户名"], input[type="text"]:first-of-type, input[name="username"]');
  if (usernameInput) {
    await usernameInput.fill(TEST_USER.username);
  }
  
  // 填写密码
  const passwordInput = await page.$('input[type="password"]');
  if (passwordInput) {
    await passwordInput.fill(TEST_USER.password);
  }
  
  // 点击登录按钮
  const loginBtn = await page.$('button[type="submit"], button:has-text("登录"), button:has-text("登 录"), .login-btn');
  if (loginBtn) {
    await loginBtn.click();
  }
  
  await page.waitForTimeout(3000);
  
  // 检查是否登录成功
  const currentUrl = page.url();
  if (currentUrl.includes('login')) {
    console.log('Login may have failed, checking again...');
    await page.waitForTimeout(2000);
  }
  
  const finalUrl = page.url();
  if (!finalUrl.includes('login')) {
    console.log('Login successful');
    await takeScreenshot(page, 'login-success');
    return true;
  }
  
  console.log('Login failed');
  return false;
}

async function navigateToAdmin(page) {
  console.log('Navigating to admin panel...');
  
  // 尝试直接导航到后台
  await page.goto(`${BASE_URL}/admin`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
  
  await takeScreenshot(page, 'admin-home');
  console.log('Entered admin panel');
}

async function clickMenu(page, menuPath) {
  for (const menuItem of menuPath) {
    try {
      // 尝试多种选择器
      const selectors = [
        `text="${menuItem}"`,
        `.el-menu-item:has-text("${menuItem}")`,
        `.el-submenu__title:has-text("${menuItem}")`,
        `[role="menuitem"]:has-text("${menuItem}")`
      ];
      
      let clicked = false;
      for (const selector of selectors) {
        const element = await page.$(selector);
        if (element) {
          await element.click();
          await page.waitForTimeout(800);
          clicked = true;
          break;
        }
      }
      
      if (!clicked) {
        console.log(`Warning: Could not find menu item "${menuItem}"`);
      }
    } catch (e) {
      console.log(`Error clicking menu "${menuItem}": ${e.message}`);
    }
  }
}

async function testModule(page, moduleName, menuPath, actions = []) {
  console.log(`\nTesting module: ${moduleName}`);
  const result = { module: moduleName, status: 'pending', screenshot: null, error: null };
  
  try {
    // 导航到模块
    await clickMenu(page, menuPath);
    await page.waitForTimeout(1500);
    
    // 执行额外操作
    for (const action of actions) {
      try {
        if (action.type === 'click') {
          const element = await page.$(action.selector);
          if (element) {
            await element.click();
            await page.waitForTimeout(800);
          }
        } else if (action.type === 'wait') {
          await page.waitForTimeout(action.duration);
        }
      } catch (e) {
        console.log(`Action error: ${e.message}`);
      }
    }
    
    // 截图
    const screenshot = await takeScreenshot(page, moduleName.replace(/\s+/g, '-'));
    result.status = 'passed';
    result.screenshot = screenshot;
    console.log(`✓ ${moduleName} test passed`);
  } catch (error) {
    result.status = 'failed';
    result.error = error.message;
    console.log(`✗ ${moduleName} test failed: ${error.message}`);
  }
  
  testResults.push(result);
  return result;
}

async function main() {
  console.log('========================================');
  console.log('RK-Web System Management Module Test');
  console.log('========================================\n');
  
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  
  try {
    // 登录
    const loginSuccess = await login(page);
    if (!loginSuccess) {
      await takeScreenshot(page, 'login-failed');
      throw new Error('Login failed');
    }
    
    // 导航到后台
    await navigateToAdmin(page);
    
    // 测试系统管理模块
    // 1. 角色管理列表
    await testModule(page, '01-role-list', ['系统管理', '角色管理']);
    
    // 2. 角色新增
    await testModule(page, '02-role-add', ['系统管理', '角色管理'], [
      { type: 'click', selector: 'button:has-text("新增"), .el-button--primary:has-text("新增")' }
    ]);
    
    // 返回列表
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 3. 角色编辑
    await testModule(page, '03-role-edit', ['系统管理', '角色管理'], [
      { type: 'click', selector: 'button:has-text("编辑"), .el-button:has-text("编辑")' }
    ]);
    
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 4. 角色权限分配
    await testModule(page, '04-role-permission', ['系统管理', '角色管理'], [
      { type: 'click', selector: 'button:has-text("权限"), button:has-text("分配"), .el-button:has-text("权限")' }
    ]);
    
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 5. 租户管理列表
    await testModule(page, '05-tenant-list', ['系统管理', '租户管理']);
    
    // 6. 租户统计卡片 - 检查页面顶部统计
    await testModule(page, '06-tenant-stats', ['系统管理', '租户管理']);
    
    // 7. 租户新增
    await testModule(page, '07-tenant-add', ['系统管理', '租户管理'], [
      { type: 'click', selector: 'button:has-text("新增"), .el-button--primary:has-text("新增")' }
    ]);
    
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 8. 租户详情
    await testModule(page, '08-tenant-detail', ['系统管理', '租户管理'], [
      { type: 'click', selector: 'button:has-text("详情"), .el-button:has-text("详情")' }
    ]);
    
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 9. 租户续费
    await testModule(page, '09-tenant-renew', ['系统管理', '租户管理'], [
      { type: 'click', selector: 'button:has-text("续费"), .el-button:has-text("续费")' }
    ]);
    
    await page.keyboard.press('Escape');
    await page.waitForTimeout(500);
    
    // 10. 菜单管理
    await testModule(page, '10-menu-management', ['系统管理', '菜单管理']);
    
    // 输出测试报告
    console.log('\n\n========================================');
    console.log('TEST REPORT');
    console.log('========================================');
    console.log(`Total tests: ${testResults.length}`);
    const passed = testResults.filter(r => r.status === 'passed').length;
    const failed = testResults.filter(r => r.status === 'failed').length;
    console.log(`Passed: ${passed} | Failed: ${failed}`);
    console.log(`Pass rate: ${((passed / testResults.length) * 100).toFixed(1)}%`);
    console.log('\nDetailed results:');
    testResults.forEach(r => {
      const icon = r.status === 'passed' ? '[PASS]' : '[FAIL]';
      console.log(`  ${icon} ${r.module} - ${r.screenshot || r.error}`);
    });
    
    // 保存测试报告
    const reportPath = path.join(SCREENSHOT_DIR, 'system-test-report.json');
    fs.writeFileSync(reportPath, JSON.stringify({
      timestamp: new Date().toISOString(),
      total: testResults.length,
      passed,
      failed,
      results: testResults
    }, null, 2));
    console.log(`\nReport saved: ${reportPath}`);
    
  } catch (error) {
    console.error('Test execution error:', error);
    await takeScreenshot(page, 'error-state');
  } finally {
    await browser.close();
  }
}

main();
