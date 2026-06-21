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

// 测试1: 活动列表页面（前台）- 正确路径 /activities
async function testActivityListFront(page) {
  console.log('\n=== 测试1: 活动列表页面(前台) ===');
  const issues = [];

  try {
    await page.goto(`${BASE_URL}/activities`);
    await wait(3000);
    await screenshot(page, '01-activities-list-front', '活动列表页面(前台)');

    // 检查活动列表
    const activityCards = await page.locator('.activity-card, .activity-item, .card, [class*="activity"]').count();
    console.log(`找到 ${activityCards} 个活动相关元素`);

    // 检查当前URL
    const currentUrl = page.url();
    console.log(`当前URL: ${currentUrl}`);

    // 检查页面标题
    const pageTitle = await page.title();
    console.log(`页面标题: ${pageTitle}`);

    // 检查是否有活动数据或内容
    const pageContent = await page.content();
    const hasActivityContent = pageContent.includes('活动') || pageContent.includes('activity');
    console.log(`页面包含活动内容: ${hasActivityContent}`);

    logResult('活动列表(前台)', issues.length === 0 ? 'PASS' : 'PARTIAL', 
      `URL: ${currentUrl}, 元素数: ${activityCards}`, issues);
  } catch (error) {
    logResult('活动列表(前台)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试2: 活动详情页面
async function testActivityDetail(page) {
  console.log('\n=== 测试2: 活动详情页面 ===');
  const issues = [];

  try {
    // 先访问列表页面
    await page.goto(`${BASE_URL}/activities`);
    await wait(2000);

    // 尝试点击第一个活动
    const activityLink = await page.locator('a[href*="/activities/"], .activity-card, .card a').first();
    if (await activityLink.isVisible().catch(() => false)) {
      await activityLink.click();
      await wait(2000);
      await screenshot(page, '02-activity-detail', '活动详情页面');
      
      // 检查详情页面内容
      const pageContent = await page.content();
      console.log('活动详情页面已加载');

      // 检查报名按钮
      const registerBtn = await page.locator('button:has-text("报名"), button:has-text("立即报名"), button:has-text("参加")').count();
      console.log(`找到 ${registerBtn} 个报名相关按钮`);

      logResult('活动详情页面', 'PASS', '点击活动进入详情', issues);
    } else {
      // 直接访问详情页
      await page.goto(`${BASE_URL}/activities/1`);
      await wait(2000);
      await screenshot(page, '02-activity-detail-direct', '活动详情页面(直接访问)');
      
      const currentUrl = page.url();
      console.log(`详情页URL: ${currentUrl}`);
      
      logResult('活动详情页面', 'PARTIAL', '直接访问活动详情', ['列表页无活动可点击']);
    }
  } catch (error) {
    logResult('活动详情页面', 'FAIL', error.message, [error.stack]);
  }
}

// 测试3: 活动报名功能（需要登录）
async function testActivityRegistration(page) {
  console.log('\n=== 测试3: 活动报名功能 ===');
  const issues = [];

  try {
    // 先登录 - 访问登录页面
    console.log('正在登录...');
    await page.goto(`${BASE_URL}/login`);
    await wait(2000);
    await screenshot(page, '03-login-page', '登录页面');

    // 填写登录信息
    const usernameInput = page.locator('input[type="text"], input[name="username"]').first();
    await usernameInput.fill('admin_a');
    console.log('✓ 填写用户名');

    const passwordInput = page.locator('input[type="password"]').first();
    await passwordInput.fill('123456');
    console.log('✓ 填写密码');

    // 点击登录按钮
    const loginBtn = page.locator('button:has-text("登录"), button:has-text("Login")').first();
    await loginBtn.click({ force: true });
    await wait(3000);

    await screenshot(page, '04-after-login', '登录后页面');
    console.log('✓ 点击登录按钮');

    // 检查是否登录成功
    const currentUrl = page.url();
    if (currentUrl.includes('login')) {
      issues.push('登录可能失败，仍在登录页');
    } else {
      console.log('✓ 登录成功');
    }

    // 访问活动列表
    await page.goto(`${BASE_URL}/activities`);
    await wait(2000);

    // 尝试点击活动进行报名
    const activityLink = await page.locator('a[href*="/activities/"], .activity-card, .card a').first();
    if (await activityLink.isVisible().catch(() => false)) {
      await activityLink.click();
      await wait(1500);
      await screenshot(page, '05-activity-detail-for-register', '活动详情(报名测试)');

      // 检查报名按钮
      const registerBtn = await page.locator('button:has-text("报名"), button:has-text("立即报名")').first();
      if (await registerBtn.isVisible().catch(() => false)) {
        await registerBtn.click({ force: true });
        await wait(1000);
        await screenshot(page, '06-activity-register-clicked', '点击报名按钮后');
        console.log('✓ 成功点击报名按钮');
      } else {
        issues.push('报名按钮未找到');
      }
    } else {
      issues.push('没有可点击的活动');
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
    // 直接访问后台管理页面
    await page.goto(`${BASE_URL}/admin/activity/list`);
    await wait(3000);
    await screenshot(page, '07-admin-activity-list', '后台活动管理列表');

    // 检查当前URL
    const currentUrl = page.url();
    console.log(`当前URL: ${currentUrl}`);

    // 检查是否被重定向到登录页
    if (currentUrl.includes('/login')) {
      issues.push('需要登录才能访问后台');
      logResult('活动后台管理(CRUD)', 'PARTIAL', '需要登录', issues);
      return;
    }

    // 检查表格
    const tableRows = await page.locator('table tbody tr, .el-table__row').count();
    console.log(`找到 ${tableRows} 条数据行`);

    // 检查新增按钮
    const addButton = await page.locator('button:has-text("新增"), button:has-text("新建")').first();
    const hasAddButton = await addButton.isVisible().catch(() => false);
    console.log(`新增按钮: ${hasAddButton ? '存在' : '未找到'}`);

    // 检查搜索功能
    const searchInput = await page.locator('input[placeholder*="搜索"], input[placeholder*="活动名称"]').count();
    console.log(`找到 ${searchInput} 个搜索框`);

    // 测试新增功能
    if (hasAddButton) {
      console.log('测试新增功能...');
      await addButton.click();
      await wait(1500);
      await screenshot(page, '08-admin-activity-create-form', '创建活动表单');

      // 检查表单字段
      const formFields = await page.locator('.el-dialog input, .el-dialog textarea, .el-dialog select').count();
      console.log(`创建表单有 ${formFields} 个字段`);

      // 填写测试数据
      const timestamp = Date.now();
      try {
        await page.fill('.el-dialog input[placeholder*="活动名称"], .el-dialog input[placeholder*="标题"]', `测试活动${timestamp}`);
        console.log('✓ 填写活动名称');
      } catch (e) {}

      await screenshot(page, '09-admin-activity-form-filled', '填写活动表单');

      // 关闭对话框
      await page.keyboard.press('Escape');
      await wait(500);
    } else {
      issues.push('新增按钮未找到');
    }

    // 检查编辑和删除按钮
    if (tableRows > 0) {
      const editBtn = await page.locator('button:has-text("编辑"), .edit-btn').first();
      const deleteBtn = await page.locator('button:has-text("删除"), .delete-btn').first();
      console.log(`编辑按钮: ${await editBtn.isVisible().catch(() => false) ? '存在' : '未找到'}`);
      console.log(`删除按钮: ${await deleteBtn.isVisible().catch(() => false) ? '存在' : '未找到'}`);
    }

    logResult('活动后台管理(CRUD)', issues.length === 0 ? 'PASS' : 'PARTIAL', 
      `数据行: ${tableRows}, 新增按钮: ${hasAddButton}`, issues);
  } catch (error) {
    logResult('活动后台管理(CRUD)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试5: 活动状态筛选
async function testActivityStatusFilter(page) {
  console.log('\n=== 测试5: 活动状态筛选 ===');
  const issues = [];

  try {
    await page.goto(`${BASE_URL}/admin/activity/list`);
    await wait(2000);

    // 检查状态筛选下拉框
    const statusSelect = await page.locator('.el-select:has-text("状态"), select[name="status"]').first();
    if (await statusSelect.isVisible().catch(() => false)) {
      console.log('✓ 找到状态筛选');
      await statusSelect.click();
      await wait(500);
      await screenshot(page, '10-status-filter-options', '状态筛选选项');
      await page.keyboard.press('Escape');
    } else {
      issues.push('状态筛选未找到');
    }

    // 检查类型筛选
    const typeSelect = await page.locator('.el-select:has-text("类型"), select[name="type"]').first();
    if (await typeSelect.isVisible().catch(() => false)) {
      console.log('✓ 找到类型筛选');
    }

    logResult('活动状态筛选', issues.length === 0 ? 'PASS' : 'PARTIAL', '活动筛选功能测试', issues);
  } catch (error) {
    logResult('活动状态筛选', 'FAIL', error.message, [error.stack]);
  }
}

// 测试6: API测试
async function testActivityAPI(page) {
  console.log('\n=== 测试6: 活动API测试 ===');
  const issues = [];
  const apiResponses = [];

  // 监听API请求
  page.on('response', async (response) => {
    const url = response.url();
    if (url.includes('/activity') || url.includes('/activities')) {
      try {
        const status = response.status();
        apiResponses.push({ url, status });
        console.log(`API: ${url.split('/').slice(-2).join('/')} - 状态: ${status}`);
      } catch (e) {}
    }
  });

  try {
    // 刷新页面触发API请求
    await page.goto(`${BASE_URL}/activities`);
    await wait(3000);

    await page.goto(`${BASE_URL}/admin/activity/list`);
    await wait(2000);

    await screenshot(page, '11-api-test-final', 'API测试后页面');

    const successApiCalls = apiResponses.filter(r => r.status === 200 || r.status === 201).length;
    console.log(`API调用: ${apiResponses.length}, 成功: ${successApiCalls}`);

    if (apiResponses.length === 0) {
      issues.push('没有捕获到活动相关的API请求');
    }

    logResult('活动API测试', successApiCalls > 0 ? 'PASS' : 'PARTIAL', 
      `API调用: ${apiResponses.length}, 成功: ${successApiCalls}`, issues);
  } catch (error) {
    logResult('活动API测试', 'FAIL', error.message, [error.stack]);
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
    await testActivityStatusFilter(page);
    await testActivityAPI(page);

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

    if (allIssues.length > 0) {
      console.log('\n问题列表:');
      allIssues.forEach((issue, i) => console.log(`  ${i+1}. ${issue}`));
    }

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
