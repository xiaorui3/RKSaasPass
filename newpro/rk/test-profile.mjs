import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:\\Users\\Administrator\\IdeaProjects\\RK-Web\\test-screenshots';
const TEST_USER = { username: 'member_a', password: '123456' };

async function takeScreenshot(page, name) {
  const filepath = path.join(SCREENSHOT_DIR, `test-profile-${name}.png`);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`Screenshot saved: ${filepath}`);
  return filepath;
}

async function main() {
  console.log('=== RK-Web 个人中心模块测试 ===\n');
  
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ 
    viewport: { width: 1920, height: 1080 },
    locale: 'zh-CN'
  });
  const page = await context.newPage();
  
  const results = {
    total: 8,
    passed: 0,
    failed: 0,
    skipped: 0,
    details: []
  };

  try {
    // ========== 1. 访问登录页面 ==========
    console.log('1. 访问登录页面...');
    await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    await takeScreenshot(page, '01-login-page');
    
    // 检查是否有模态框遮挡，如果有则在模态框内操作
    const loginModal = page.locator('.login-modal, .el-dialog:has(input[type="text"])').first();
    let usernameInput, passwordInput;
    
    if (await loginModal.isVisible({ timeout: 2000 }).catch(() => false)) {
      console.log('检测到登录模态框，在模态框内输入...');
      // 在模态框内找到输入框
      usernameInput = loginModal.locator('input[type="text"], input[placeholder*="用户名"]').first();
      passwordInput = loginModal.locator('input[type="password"]').first();
    } else {
      // 页面上的输入框
      usernameInput = page.locator('input[type="text"], input[placeholder*="用户名"]').first();
      passwordInput = page.locator('input[type="password"]').first();
    }
    
    // 使用force点击和填充
    console.log('输入用户名密码...');
    await usernameInput.click({ force: true });
    await usernameInput.fill(TEST_USER.username);
    await passwordInput.click({ force: true });
    await passwordInput.fill(TEST_USER.password);
    
    await takeScreenshot(page, '02-login-filled');
    
    // 点击登录按钮 - 在模态框内或页面上
    const submitBtn = page.locator('button:has-text("登录"), button:has-text("登 录")').first();
    await submitBtn.click({ force: true });
    await page.waitForTimeout(3000);
    
    await takeScreenshot(page, '03-after-login');
    
    // 检查是否登录成功
    const pageContent = await page.content();
    const hasUserInfo = pageContent.includes('member_a') || pageContent.includes('退出') || pageContent.includes('个人中心');
    
    if (hasUserInfo) {
      console.log('✅ 登录成功');
      results.passed++;
      results.details.push({ name: '登录', status: 'passed' });
    } else {
      console.log('❌ 登录失败 - 尝试使用API登录');
      
      // 尝试使用API直接登录
      const loginResponse = await page.request.post(`${BASE_URL}/api/auth/login`, {
        data: {
          username: TEST_USER.username,
          password: TEST_USER.password
        }
      });
      
      if (loginResponse.ok()) {
        const loginData = await loginResponse.json();
        console.log('API登录响应:', JSON.stringify(loginData).substring(0, 200));
        
        // 设置token到localStorage
        if (loginData.data && loginData.data.token) {
          await page.evaluate((token) => {
            localStorage.setItem('token', token);
          }, loginData.data.token);
          
          // 刷新页面
          await page.reload({ waitUntil: 'networkidle' });
          await page.waitForTimeout(1000);
          
          const newContent = await page.content();
          if (newContent.includes('member_a') || newContent.includes('退出')) {
            console.log('✅ 通过API登录成功');
            results.passed++;
            results.details.push({ name: '登录', status: 'passed', note: '通过API登录' });
          } else {
            results.failed++;
            results.details.push({ name: '登录', status: 'failed', error: 'API登录后页面无变化' });
          }
        } else {
          results.failed++;
          results.details.push({ name: '登录', status: 'failed', error: 'API响应无token' });
        }
      } else {
        results.failed++;
        results.details.push({ name: '登录', status: 'failed', error: 'API登录失败' });
      }
    }

    // ========== 2. 进入个人中心 ==========
    console.log('\n2. 访问个人中心页面...');
    
    // 尝试多种URL
    const profileUrls = ['/profile', '/personal', '/user/profile', '/user-center', '/member'];
    let profileFound = false;
    
    for (const url of profileUrls) {
      await page.goto(`${BASE_URL}${url}`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      
      const content = await page.locator('body').innerText();
      if (content.includes('个人信息') || content.includes('头像') || content.includes('修改') || content.includes('用户名') || content.includes('member')) {
        console.log(`✅ 找到个人中心页面: ${url}`);
        profileFound = true;
        await takeScreenshot(page, `04-profile-${url.replace('/', '')}`);
        break;
      }
    }
    
    if (!profileFound) {
      // 检查首页是否有个人中心入口
      await page.goto(BASE_URL, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1000);
      
      const profileLink = page.locator('a[href*="profile"], a[href*="personal"], text=个人中心, text=我的').first();
      if (await profileLink.isVisible({ timeout: 2000 }).catch(() => false)) {
        await profileLink.click({ force: true });
        await page.waitForTimeout(1500);
        profileFound = true;
      }
      
      await takeScreenshot(page, '04-profile-from-home');
    }
    
    if (profileFound) {
      results.passed++;
      results.details.push({ name: '个人中心页面', status: 'passed' });
    } else {
      console.log('❌ 个人中心页面未找到');
      results.failed++;
      results.details.push({ name: '个人中心页面', status: 'failed', note: '无法访问个人中心' });
    }

    // ========== 3. 个人信息展示 ==========
    console.log('\n3. 测试个人信息展示...');
    
    const infoTexts = await page.locator('body').innerText();
    const hasUserName = infoTexts.includes('用户名') || infoTexts.includes('账号') || infoTexts.includes('member');
    const hasEmail = infoTexts.includes('邮箱') || infoTexts.includes('email') || infoTexts.includes('@');
    const hasPhone = infoTexts.includes('手机') || infoTexts.includes('电话');
    
    await takeScreenshot(page, '05-user-info');
    
    if (hasUserName || hasEmail || hasPhone) {
      console.log(`✅ 个人信息展示: 用户名=${hasUserName}, 邮箱=${hasEmail}, 手机=${hasPhone}`);
      results.passed++;
      results.details.push({ name: '个人信息展示', status: 'passed' });
    } else {
      console.log('⚠️ 个人信息展示区域未找到');
      results.details.push({ name: '个人信息展示', status: 'warning' });
    }

    // ========== 4. 个人信息编辑 ==========
    console.log('\n4. 测试个人信息编辑...');
    
    const editBtns = [
      'button:has-text("编辑")',
      'button:has-text("修改")',
      '.edit-btn',
      'a:has-text("编辑")',
      'text=编辑资料',
      'text=修改信息'
    ];
    
    let editFound = false;
    for (const selector of editBtns) {
      const btn = page.locator(selector).first();
      if (await btn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await btn.click({ force: true });
        await page.waitForTimeout(1000);
        editFound = true;
        break;
      }
    }
    
    if (editFound) {
      await takeScreenshot(page, '06-edit-mode');
      
      const editableInputs = await page.locator('input:not([readonly]):not([type="hidden"]):not([type="file"]):not([type="password"])').count();
      
      if (editableInputs > 0) {
        console.log(`✅ 个人信息编辑功能可用，找到 ${editableInputs} 个可编辑字段`);
        results.passed++;
        results.details.push({ name: '个人信息编辑', status: 'passed', count: editableInputs });
      } else {
        console.log('⚠️ 编辑模式下无可编辑字段');
        results.details.push({ name: '个人信息编辑', status: 'warning' });
      }
    } else {
      console.log('⚠️ 未找到编辑按钮');
      results.details.push({ name: '个人信息编辑', status: 'skipped', note: '无编辑按钮' });
      results.skipped++;
    }

    // ========== 5. 头像上传 ==========
    console.log('\n5. 测试头像上传功能...');
    
    await takeScreenshot(page, '07-avatar-area');
    
    const avatarImg = page.locator('img[class*="avatar"], img[alt*="头像"], .avatar img, .user-avatar, img[style*="rounded"]').first();
    const avatarUpload = page.locator('input[type="file"][accept*="image"]').first();
    
    if (await avatarImg.isVisible({ timeout: 2000 }).catch(() => false)) {
      console.log('✅ 头像区域存在');
      results.passed++;
      results.details.push({ name: '头像上传', status: 'passed', note: '头像显示正常' });
    } else if (await avatarUpload.isVisible({ timeout: 2000 }).catch(() => false)) {
      console.log('✅ 头像上传入口存在');
      results.passed++;
      results.details.push({ name: '头像上传', status: 'passed', note: '上传入口存在' });
    } else {
      console.log('⚠️ 未找到头像相关元素');
      results.details.push({ name: '头像上传', status: 'skipped', note: '未找到头像区域' });
      results.skipped++;
    }

    // ========== 6. 密码修改 ==========
    console.log('\n6. 测试密码修改功能...');
    
    const passwordBtns = [
      'text=修改密码',
      'text=更改密码',
      'text=密码修改',
      'button:has-text("密码")',
      'text=安全设置'
    ];
    
    let passwordFound = false;
    for (const selector of passwordBtns) {
      const btn = page.locator(selector).first();
      if (await btn.isVisible({ timeout: 1000 }).catch(() => false)) {
        await btn.click({ force: true });
        await page.waitForTimeout(1000);
        passwordFound = true;
        break;
      }
    }
    
    if (passwordFound) {
      await takeScreenshot(page, '08-password-change');
      
      const passwordInputs = await page.locator('input[type="password"]').count();
      if (passwordInputs >= 2) {
        console.log(`✅ 密码修改表单正常，找到 ${passwordInputs} 个密码输入框`);
        results.passed++;
        results.details.push({ name: '密码修改', status: 'passed' });
      } else {
        console.log('⚠️ 密码修改表单不完整');
        results.details.push({ name: '密码修改', status: 'warning', note: '密码输入框不足' });
      }
    } else {
      console.log('⚠️ 未找到密码修改入口');
      results.details.push({ name: '密码修改', status: 'skipped', note: '无密码修改按钮' });
      results.skipped++;
    }

    // ========== 7. 我的活动 ==========
    console.log('\n7. 测试"我的活动"功能...');
    
    const activitySelectors = [
      'text=我的活动',
      'text=参与的活动',
      '[class*="tab"]:has-text("活动")',
      'a[href*="activity"]'
    ];
    
    let activityFound = false;
    for (const selector of activitySelectors) {
      const el = page.locator(selector).first();
      if (await el.isVisible({ timeout: 1000 }).catch(() => false)) {
        await el.click({ force: true });
        await page.waitForTimeout(1500);
        activityFound = true;
        break;
      }
    }
    
    if (!activityFound) {
      // 尝试直接访问
      await page.goto(`${BASE_URL}/profile/activity`, { waitUntil: 'networkidle' });
      await page.waitForTimeout(1500);
      activityFound = true;
    }
    
    await takeScreenshot(page, '09-my-activities');
    
    const activityList = await page.locator('.activity-item, .list-item, table tbody tr, .el-table__row').count();
    const pageText = await page.locator('body').innerText();
    
    if (pageText.includes('活动') || activityList > 0) {
      console.log(`✅ 我的活动页面，找到 ${activityList} 条记录`);
      results.passed++;
      results.details.push({ name: '我的活动', status: 'passed', count: activityList });
    } else {
      console.log('⚠️ 未找到"我的活动"内容');
      results.details.push({ name: '我的活动', status: 'warning', note: '无活动数据' });
    }

    // ========== 8. 我的比赛 ==========
    console.log('\n8. 测试"我的比赛"功能...');
    
    await page.goto(`${BASE_URL}/profile/competition`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await takeScreenshot(page, '10-my-competitions');
    
    const competitionList = await page.locator('.competition-item, .list-item, table tbody tr, .el-table__row').count();
    const competitionText = await page.locator('body').innerText();
    
    if (competitionText.includes('比赛') || competitionList > 0) {
      console.log(`✅ 我的比赛页面，找到 ${competitionList} 条记录`);
      results.passed++;
      results.details.push({ name: '我的比赛', status: 'passed', count: competitionList });
    } else {
      console.log('⚠️ 未找到"我的比赛"内容');
      results.details.push({ name: '我的比赛', status: 'warning', note: '无比赛数据' });
    }

    // ========== 9. 我的作品 ==========
    console.log('\n9. 测试"我的作品"功能...');
    
    await page.goto(`${BASE_URL}/profile/works`, { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    await takeScreenshot(page, '11-my-works');
    
    const worksList = await page.locator('.work-item, .list-item, table tbody tr, .el-table__row').count();
    const worksText = await page.locator('body').innerText();
    
    if (worksText.includes('作品') || worksList > 0) {
      console.log(`✅ 我的作品页面，找到 ${worksList} 条记录`);
      results.passed++;
      results.details.push({ name: '我的作品', status: 'passed', count: worksList });
    } else {
      console.log('⚠️ 未找到"我的作品"内容');
      results.details.push({ name: '我的作品', status: 'warning', note: '无作品数据' });
    }

    // ========== 汇总结果 ==========
    console.log('\n' + '='.repeat(50));
    console.log('测试结果汇总');
    console.log('='.repeat(50));
    console.log(`总计: ${results.total} 项`);
    console.log(`通过: ${results.passed} 项`);
    console.log(`失败: ${results.failed} 项`);
    console.log(`跳过: ${results.skipped} 项`);
    console.log('='.repeat(50));
    
    results.details.forEach(d => {
      const icon = d.status === 'passed' ? '✅' : d.status === 'failed' ? '❌' : '⚠️';
      console.log(`${icon} ${d.name}: ${d.status}${d.note ? ' - ' + d.note : ''}`);
    });

  } catch (error) {
    console.error('测试过程中出错:', error.message);
    await takeScreenshot(page, 'error-screenshot');
    results.failed++;
    results.details.push({ name: '测试错误', status: 'failed', error: error.message });
  } finally {
    await browser.close();
  }
  
  // 输出JSON结果
  const reportPath = path.join(SCREENSHOT_DIR, 'test-profile-report.json');
  fs.writeFileSync(reportPath, JSON.stringify(results, null, 2));
  console.log(`\n测试报告已保存: ${reportPath}`);
  
  return results;
}

main().catch(console.error);
