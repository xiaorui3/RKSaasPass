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
    await wait(3000);
    await screenshot(page, '01-activity-list-front', '活动列表页面(前台)');

    // 检查活动列表 - 更广泛的选择器
    const activityCards = await page.locator('.activity-card, .activity-item, .card, [class*="activity"]').count();
    console.log(`找到 ${activityCards} 个活动相关元素`);

    // 检查是否有活动数据
    const pageContent = await page.content();
    const hasActivityText = pageContent.includes('活动') || pageContent.includes('activity') || pageContent.includes('Activity');
    console.log(`页面包含活动相关文字: ${hasActivityText}`);

    // 检查是否有列表容器
    const listContainer = await page.locator('.list, .content, main, .container').count();
    console.log(`找到 ${listContainer} 个容器元素`);

    // 检查路由是否正确
    const currentUrl = page.url();
    console.log(`当前URL: ${currentUrl}`);

    if (!currentUrl.includes('/activity')) {
      issues.push('活动路由不正确');
    }

    // 尝试查找任何链接或卡片
    const links = await page.locator('a, .card, .item').count();
    console.log(`找到 ${links} 个链接/卡片元素`);

    logResult('活动列表(前台)', issues.length === 0 ? 'PASS' : 'PARTIAL', `URL: ${currentUrl}, 元素数: ${links}`, issues);
  } catch (error) {
    logResult('活动列表(前台)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试2: 活动详情页面
async function testActivityDetail(page) {
  console.log('\n=== 测试2: 活动详情页面 ===');
  const issues = [];

  try {
    // 直接访问一个活动详情页面（假设ID=1）
    await page.goto(`${BASE_URL}/activity/1`);
    await wait(2000);
    await screenshot(page, '02-activity-detail', '活动详情页面');

    // 检查页面内容
    const pageContent = await page.content();
    console.log('活动详情页面已加载');

    // 检查是否有404或错误提示
    const hasError = pageContent.includes('404') || pageContent.includes('找不到') || pageContent.includes('不存在');
    if (hasError) {
      issues.push('活动详情显示错误或不存在');
    }

    // 检查报名按钮
    const registerButton = await page.locator('button:has-text("报名"), button:has-text("立即报名"), button:has-text("参加")').count();
    console.log(`找到 ${registerButton} 个报名相关按钮`);

    logResult('活动详情页面', issues.length === 0 ? 'PASS' : 'PARTIAL', '直接访问活动详情', issues);
  } catch (error) {
    logResult('活动详情页面', 'FAIL', error.message, [error.stack]);
  }
}

// 测试3: 后台登录并测试活动管理
async function testAdminActivityManagement(page) {
  console.log('\n=== 测试3: 活动后台管理(CRUD) ===');
  const issues = [];

  try {
    // 直接访问后台管理页面
    console.log('访问后台管理...');
    await page.goto(`${BASE_URL}/admin/activity`);
    await wait(3000);
    await screenshot(page, '03-admin-activity-list', '后台活动管理列表');

    // 检查当前URL
    const currentUrl = page.url();
    console.log(`当前URL: ${currentUrl}`);

    // 检查是否被重定向到登录页
    if (currentUrl.includes('/login')) {
      console.log('需要登录，正在处理登录模态框...');

      // 等待登录模态框出现
      await wait(1000);

      // 关闭可能存在的登录模态框（点击遮罩层或关闭按钮）
      try {
        await page.locator('.el-overlay, .el-dialog__close, .close-btn').first().click({ timeout: 2000 });
        await wait(500);
      } catch (e) {
        console.log('没有模态框需要关闭');
      }

      // 填写登录信息 - 使用更精确的选择器
      const loginForm = await page.locator('.login-form, form, .el-form').first();
      if (await loginForm.isVisible()) {
        // 填写用户名
        const usernameInput = page.locator('input[type="text"]').first();
        await usernameInput.fill('admin_a');
        console.log('✓ 填写用户名');

        // 填写密码
        const passwordInput = page.locator('input[type="password"]').first();
        await passwordInput.fill('123456');
        console.log('✓ 填写密码');

        await screenshot(page, '04-login-form-filled', '填写登录表单');

        // 点击登录按钮 - 使用force选项绕过遮挡
        const loginBtn = page.locator('button:has-text("登录"), button:has-text("Login")').first();
        await loginBtn.click({ force: true });
        console.log('✓ 点击登录按钮');
        await wait(3000);

        await screenshot(page, '05-after-login', '登录后页面');

        // 再次访问管理页面
        await page.goto(`${BASE_URL}/admin/activity`);
        await wait(2000);
        await screenshot(page, '06-admin-activity-after-login', '登录后活动管理');
      }
    }

    // 检查管理页面元素
    const currentUrl2 = page.url();
    console.log(`管理页面URL: ${currentUrl2}`);

    // 检查表格或列表
    const tableRows = await page.locator('table tr, .el-table__row, .data-row').count();
    console.log(`找到 ${tableRows} 条数据行`);

    // 检查新增按钮
    const addButton = await page.locator('button:has-text("新增"), button:has-text("新建"), button:has-text("创建")').count();
    console.log(`找到 ${addButton} 个新增按钮`);

    // 检查搜索功能
    const searchInput = await page.locator('input[placeholder*="搜索"], input[placeholder*="查询"], .search-input').count();
    console.log(`找到 ${searchInput} 个搜索框`);

    // 测试新增功能
    if (addButton > 0) {
      console.log('测试新增功能...');
      await page.locator('button:has-text("新增"), button:has-text("新建")').first().click();
      await wait(1500);
      await screenshot(page, '07-admin-activity-create-form', '创建活动表单');

      // 检查表单
      const formFields = await page.locator('input, textarea, select').count();
      console.log(`创建表单有 ${formFields} 个字段`);

      // 关闭表单
      await page.keyboard.press('Escape');
      await wait(500);
    }

    logResult('活动后台管理(CRUD)', tableRows > 0 || addButton > 0 ? 'PASS' : 'PARTIAL', 
      `数据行: ${tableRows}, 新增按钮: ${addButton}`, issues);
  } catch (error) {
    logResult('活动后台管理(CRUD)', 'FAIL', error.message, [error.stack]);
  }
}

// 测试4: 活动报名功能（需要登录状态）
async function testActivityRegistration(page) {
  console.log('\n=== 测试4: 活动报名功能 ===');
  const issues = [];

  try {
    // 访问活动列表
    await page.goto(`${BASE_URL}/activity`);
    await wait(2000);

    // 尝试点击一个活动
    const activityLink = await page.locator('a[href*="/activity/"], .activity-card, .activity-item').first();
    if (await activityLink.isVisible().catch(() => false)) {
      await activityLink.click();
      await wait(1500);
      await screenshot(page, '08-activity-detail-for-register', '活动详情(报名测试)');

      // 检查报名按钮
      const registerBtn = await page.locator('button:has-text("报名"), button:has-text("立即报名")').first();
      if (await registerBtn.isVisible().catch(() => false)) {
        await registerBtn.click({ force: true });
        await wait(1000);
        await screenshot(page, '09-activity-register-clicked', '点击报名按钮后');
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

// 测试5: API测试
async function testActivityAPI(page) {
  console.log('\n=== 测试5: 活动API测试 ===');
  const issues = [];

  try {
    // 检查网络请求
    const apiResponses = [];

    page.on('response', async (response) => {
      if (response.url().includes('/api/activity') || response.url().includes('/activity')) {
        try {
          const status = response.status();
          apiResponses.push({ url: response.url(), status });
          console.log(`API请求: ${response.url()} - 状态: ${status}`);
        } catch (e) {}
      }
    });

    // 刷新页面触发API请求
    await page.goto(`${BASE_URL}/activity`);
    await wait(3000);

    await screenshot(page, '10-api-test-final', 'API测试后页面');

    const successApiCalls = apiResponses.filter(r => r.status === 200 || r.status === 201).length;
    console.log(`成功的API调用: ${successApiCalls}/${apiResponses.length}`);

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
    await testAdminActivityManagement(page);
    await testActivityRegistration(page);
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

    // 返回结果供调用者使用
    return { passCount, failCount, partialCount, issues: allIssues };

  } finally {
    await browser.close();
  }
}

runTests().catch(console.error);
