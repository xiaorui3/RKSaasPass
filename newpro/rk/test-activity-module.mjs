import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

// 测试配置
const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:/Users/Administrator/IdeaProjects/RK-Web/test-screenshots';
const TEST_RESULTS = [];

// 创建截图目录
if (!fs.existsSync(SCREENSHOT_DIR)) {
  fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
}

// 工具函数
async function screenshot(page, name, description) {
  const filepath = path.join(SCREENSHOT_DIR, `test-activity-${name}.png`);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`✓ 截图: test-activity-${name}.png - ${description}`);
  return filepath;
}

function logResult(testName, status, details, issues = []) {
  const result = { test: testName, status, details, issues, timestamp: new Date().toISOString() };
  TEST_RESULTS.push(result);
  console.log(`\n${status === 'PASS' ? '✅' : status === 'FAIL' ? '❌' : '⚠️'} ${testName}: ${status}`);
  issues.forEach(issue => console.log(`   问题: ${issue}`));
}

const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

// 测试1: 活动列表页面（前台）
async function testActivityListFront(page) {
  console.log('\n=== 测试1: 活动列表页面(前台) ===');
  const issues = [];

  try {
    await page.goto(`${BASE_URL}/activity`);
    await wait(2000);
    await screenshot(page, '01-activity-list-front', '活动列表页面(前台)');

    // 检查活动列表
    const activityCards = await page.locator('.activity-card, .activity-item, [class*="activity"]').count();
    console.log(`找到 ${activityCards} 个活动元素`);
    
    if (activityCards === 0) {
      issues.push('活动列表为空或元素未找到');
    }

    // 检查页面标题
    const pageTitle = await page.title();
    console.log(`页面标题: ${pageTitle}`);

    logResult('活动列表(前台)', issues.length === 0 ? 'PASS' : 'PARTIAL', `找到${activityCards}个活动`, issues);
  } catch (error) {
    logResult('活动列表(前台)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试2: 活动详情页面
async function testActivityDetail(page) {
  console.log('\n=== 测试2: 活动详情页面 ===');
  const issues = [];

  try {
    await page.goto(`${BASE_URL}/activity`);
    await wait(2000);

    // 点击第一个活动
    const activityItem = await page.locator('.activity-card, .activity-item, a[href*="/activity/"]').first();
    if (await activityItem.isVisible().catch(() => false)) {
      await activityItem.click();
      await wait(2000);
      await screenshot(page, '02-activity-detail', '活动详情页面');

      // 检查报名按钮
      const registerButton = await page.locator('button:has-text("报名"), button:has-text("立即报名"), .register-btn').first();
      if (await registerButton.isVisible().catch(() => false)) {
        console.log('✓ 找到报名按钮');
        await screenshot(page, '03-activity-detail-with-register', '活动详情-报名按钮');
      } else {
        issues.push('报名按钮未找到');
      }

      // 检查活动信息
      const activityInfo = await page.locator('.activity-info, .detail-info, [class*="detail"]').first();
      if (await activityInfo.isVisible().catch(() => false)) {
        console.log('✓ 找到活动信息区域');
      }
    } else {
      issues.push('没有可点击的活动');
    }

    logResult('活动详情页面', issues.length === 0 ? 'PASS' : 'PARTIAL', '活动详情页面测试', issues);
  } catch (error) {
    logResult('活动详情页面', 'FAIL', error.message, [error.stack]);
  }
}

// 测试3: 活动报名功能（需要登录）
async function testActivityRegistration(page) {
  console.log('\n=== 测试3: 活动报名功能 ===');
  const issues = [];

  try {
    // 先登录
    console.log('正在登录...');
    await page.goto(`${BASE_URL}/login`);
    await wait(1500);
    await screenshot(page, '04-login-page', '登录页面');

    // 填写登录信息
    const usernameInput = await page.locator('input[name="username"], input[placeholder*="用户名"]').first();
    const passwordInput = await page.locator('input[name="password"], input[placeholder*="密码"]').first();
    const tenantSelect = await page.locator('select[name="tenant"], select').first();

    if (await usernameInput.isVisible()) {
      await usernameInput.fill('admin_a');
      await passwordInput.fill('123456');
      
      // 选择租户
      try {
        await tenantSelect.selectOption('1');
      } catch (e) {
        console.log('租户选择跳过');
      }

      // 点击登录
      await page.click('button[type="submit"], button:has-text("登录"), button:has-text("Login")');
      await wait(3000);
      await screenshot(page, '05-after-login', '登录后');

      // 检查是否登录成功
      const currentUrl = page.url();
      console.log(`当前URL: ${currentUrl}`);
      
      if (currentUrl.includes('login')) {
        issues.push('登录可能失败');
      } else {
        console.log('✓ 登录成功');
      }
    } else {
      issues.push('登录表单未找到');
    }

    // 访问活动并尝试报名
    await page.goto(`${BASE_URL}/activity`);
    await wait(2000);

    const activityItem = await page.locator('.activity-card, .activity-item, a[href*="/activity/"]').first();
    if (await activityItem.isVisible().catch(() => false)) {
      await activityItem.click();
      await wait(1500);

      const registerButton = await page.locator('button:has-text("报名"), button:has-text("立即报名"), .register-btn').first();
      if (await registerButton.isVisible().catch(() => false)) {
        await registerButton.click();
        await wait(1500);
        await screenshot(page, '06-activity-register-dialog', '活动报名对话框');
        console.log('✓ 点击报名按钮成功');
      } else {
        issues.push('报名按钮不可见（可能已报名或活动已结束）');
      }
    }

    logResult('活动报名功能', issues.length === 0 ? 'PASS' : 'PARTIAL', '活动报名流程测试', issues);
  } catch (error) {
    logResult('活动报名功能', 'FAIL', error.message, [error.stack]);
  }
}

// 测试4: 活动后台管理(CRUD)
async function testAdminActivityCRUD(page) {
  console.log('\n=== 测试4: 活动后台管理(CRUD) ===');
  const issues = [];

  try {
    // 访问管理页面
    await page.goto(`${BASE_URL}/admin/activity`);
    await wait(2000);
    await screenshot(page, '07-admin-activity-list', '管理员活动列表');

    // 检查列表
    const tableRows = await page.locator('table tbody tr, .el-table__row, [class*="table-row"]').count();
    console.log(`找到 ${tableRows} 条活动记录`);

    // 检查新增按钮
    const addButton = await page.locator('button:has-text("新增"), button:has-text("创建"), button:has-text("新建")').first();
    if (await addButton.isVisible().catch(() => false)) {
      console.log('✓ 找到新增按钮');
      
      await addButton.click();
      await wait(1500);
      await screenshot(page, '08-admin-activity-create-form', '创建活动表单');

      // 检查表单字段
      const formFields = await page.locator('input, textarea, select').count();
      console.log(`找到 ${formFields} 个表单字段`);

      // 填写测试数据
      const timestamp = Date.now();
      try {
        await page.fill('input[name="title"], input[placeholder*="标题"], input[placeholder*="活动名"]', `自动化测试活动${timestamp}`);
        console.log('✓ 填写活动标题');
      } catch (e) {
        console.log('活动标题字段未找到');
      }

      try {
        await page.fill('input[name="location"], input[placeholder*="地点"], input[placeholder*="位置"]', '测试地点');
        console.log('✓ 填写活动地点');
      } catch (e) {
        console.log('活动地点字段未找到');
      }

      await screenshot(page, '09-admin-activity-form-filled', '填写活动表单');

      // 关闭对话框
      await page.keyboard.press('Escape');
      await wait(500);
    } else {
      issues.push('新增按钮未找到');
    }

    // 检查编辑功能
    const editButton = await page.locator('button:has-text("编辑"), .edit-btn').first();
    if (await editButton.isVisible().catch(() => false)) {
      console.log('✓ 找到编辑按钮');
    }

    // 检查删除功能
    const deleteButton = await page.locator('button:has-text("删除"), .delete-btn').first();
    if (await deleteButton.isVisible().catch(() => false)) {
      console.log('✓ 找到删除按钮');
    }

    logResult('活动后台管理(CRUD)', issues.length === 0 ? 'PASS' : 'PARTIAL', `找到${tableRows}条记录`, issues);
  } catch (error) {
    logResult('活动后台管理(CRUD)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试5: 活动状态管理
async function testActivityStatus(page) {
  console.log('\n=== 测试5: 活动状态管理 ===');
  const issues = [];

  try {
    await page.goto(`${BASE_URL}/admin/activity`);
    await wait(2000);

    // 检查状态筛选
    const statusFilter = await page.locator('select[name="status"], .status-filter, [class*="status-select"]').first();
    if (await statusFilter.isVisible().catch(() => false)) {
      console.log('✓ 找到状态筛选');
      await screenshot(page, '10-admin-activity-status-filter', '活动状态筛选');
    } else {
      issues.push('状态筛选未找到');
    }

    // 检查状态标签
    const statusTags = await page.locator('.status-tag, .el-tag, [class*="status"]').count();
    console.log(`找到 ${statusTags} 个状态标签`);

    logResult('活动状态管理', issues.length === 0 ? 'PASS' : 'PARTIAL', '活动状态管理测试', issues);
  } catch (error) {
    logResult('活动状态管理', 'FAIL', error.message, [error.stack]);
  }
}

// 主测试函数
async function runTests() {
  console.log('🚀 开始活动模块浏览器自动化测试\n');
  console.log(`前端地址: ${BASE_URL}`);
  console.log(`截图目录: ${SCREENSHOT_DIR}\n`);

  const browser = await chromium.launch({
    headless: true
  });

  const context = await browser.newContext({
    viewport: { width: 1920, height: 1080 }
  });

  const page = await context.newPage();

  try {
    await testActivityListFront(page);
    await testActivityDetail(page);
    await testActivityRegistration(page);
    await testAdminActivityCRUD(page);
    await testActivityStatus(page);

    // 生成报告
    console.log('\n' + '='.repeat(60));
    console.log('📊 活动模块测试报告');
    console.log('='.repeat(60));

    const passCount = TEST_RESULTS.filter(r => r.status === 'PASS').length;
    const failCount = TEST_RESULTS.filter(r => r.status === 'FAIL').length;
    const partialCount = TEST_RESULTS.filter(r => r.status === 'PARTIAL').length;

    console.log(`总测试数: ${TEST_RESULTS.length}`);
    console.log(`✅ 通过: ${passCount}`);
    console.log(`⚠️  部分通过: ${partialCount}`);
    console.log(`❌ 失败: ${failCount}`);

    const allIssues = TEST_RESULTS.flatMap(r => r.issues);
    console.log(`\n🔍 发现问题总数: ${allIssues.length}`);

    // 保存结果
    const reportPath = path.join(SCREENSHOT_DIR, 'test-activity-report.json');
    fs.writeFileSync(reportPath, JSON.stringify({
      summary: { 
        total: TEST_RESULTS.length, 
        pass: passCount, 
        fail: failCount, 
        partial: partialCount, 
        issues: allIssues.length 
      },
      results: TEST_RESULTS,
      timestamp: new Date().toISOString()
    }, null, 2));
    console.log(`\n📄 测试报告已保存: ${reportPath}`);

  } finally {
    await browser.close();
  }
}

runTests().catch(console.error);
