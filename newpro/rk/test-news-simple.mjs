import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:\\Users\\Administrator\\IdeaProjects\\RK-Web\\test-screenshots';
const RESULTS = [];

async function screenshot(page, name) {
  const filepath = path.join(SCREENSHOT_DIR, `test-news-${name}.png`);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`Screenshot saved: ${name}`);
  return filepath;
}

const wait = (ms) => new Promise(r => setTimeout(r, ms));

async function runTests() {
  console.log('Starting RK-Web News Module Tests...');
  console.log(`URL: ${BASE_URL}`);
  console.log(`Screenshots: ${SCREENSHOT_DIR}\n`);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  const results = { passed: [], failed: [], issues: [] };

  try {
    // Test 1: Homepage
    console.log('\n=== Test 1: Homepage ===');
    try {
      await page.goto(BASE_URL, { timeout: 30000 });
      await wait(2000);
      await screenshot(page, '01-homepage');
      
      const title = await page.title();
      console.log(`Page title: ${title}`);
      
      // Check navigation
      const nav = await page.locator('nav, .nav, [class*="nav"]').first();
      const hasNav = await nav.isVisible().catch(() => false);
      
      if (hasNav) {
        results.passed.push('Homepage loaded with navigation');
        console.log('✅ Homepage: Navigation found');
      } else {
        results.issues.push('Homepage: Navigation not found');
        console.log('⚠️ Homepage: Navigation not found');
      }
    } catch (e) {
      results.failed.push(`Homepage: ${e.message}`);
      console.log(`❌ Homepage failed: ${e.message}`);
    }

    // Test 2: Navigation Menu
    console.log('\n=== Test 2: Navigation Menu ===');
    try {
      const links = await page.locator('nav a, .nav a, [class*="nav"] a').count();
      console.log(`Found ${links} navigation links`);
      
      // Try clicking news link
      const newsLink = await page.locator('a[href*="news"], a:has-text("新闻"), a:has-text("News")').first();
      const hasNewsLink = await newsLink.isVisible().catch(() => false);
      
      if (hasNewsLink) {
        await newsLink.click();
        await wait(2000);
        await screenshot(page, '02-news-page');
        results.passed.push('Navigation: News link works');
        console.log('✅ Navigation: Clicked news link');
      } else {
        results.issues.push('Navigation: News link not found');
      }
    } catch (e) {
      results.issues.push(`Navigation: ${e.message}`);
    }

    // Test 3: News List (Frontend)
    console.log('\n=== Test 3: News List (Frontend) ===');
    try {
      await page.goto(`${BASE_URL}/news`, { timeout: 15000 });
      await wait(2000);
      await screenshot(page, '03-news-list');
      
      // Check news items
      const newsItems = await page.locator('.news-item, .news-card, [class*="news-item"], article').count();
      console.log(`Found ${newsItems} news items`);
      
      if (newsItems > 0) {
        results.passed.push(`News List: ${newsItems} items displayed`);
        console.log(`✅ News List: ${newsItems} items found`);
      } else {
        results.issues.push('News List: No news items found');
      }
      
      // Check pagination
      const pagination = await page.locator('.pagination, [class*="pagination"], .el-pagination').first();
      const hasPagination = await pagination.isVisible().catch(() => false);
      
      if (hasPagination) {
        results.passed.push('News List: Pagination visible');
      }
    } catch (e) {
      results.failed.push(`News List: ${e.message}`);
      console.log(`❌ News List failed: ${e.message}`);
    }

    // Test 4: News Detail
    console.log('\n=== Test 4: News Detail ===');
    try {
      await page.goto(`${BASE_URL}/news`, { timeout: 15000 });
      await wait(1500);
      
      // Click first news item
      const firstNews = await page.locator('.news-item, .news-card, [class*="news-item"], article').first();
      const hasNews = await firstNews.isVisible().catch(() => false);
      
      if (hasNews) {
        await firstNews.click();
        await wait(2000);
        await screenshot(page, '04-news-detail');
        
        // Check detail content
        const content = await page.locator('.news-content, .article, [class*="content"], [class*="detail"]').first();
        const hasContent = await content.isVisible().catch(() => false);
        
        if (hasContent) {
          results.passed.push('News Detail: Content displayed');
          console.log('✅ News Detail: Content found');
        } else {
          results.issues.push('News Detail: Content not found');
        }
      } else {
        results.issues.push('News Detail: No news to click');
      }
    } catch (e) {
      results.issues.push(`News Detail: ${e.message}`);
    }

    // Test 5: Admin Login
    console.log('\n=== Test 5: Admin Login ===');
    try {
      await page.goto(`${BASE_URL}/login`, { timeout: 15000 });
      await wait(1500);
      await screenshot(page, '05-login-page');
      
      // Fill login form
      const usernameInput = await page.locator('input[name="username"], input[placeholder*="用户名"], input[placeholder*="账号"]').first();
      const passwordInput = await page.locator('input[name="password"], input[placeholder*="密码"], input[type="password"]').first();
      
      await usernameInput.fill('admin_a');
      await passwordInput.fill('123456');
      
      // Click login
      const loginBtn = await page.locator('button[type="submit"], button:has-text("登录"), button:has-text("Login")').first();
      await loginBtn.click();
      await wait(3000);
      
      await screenshot(page, '06-after-login');
      
      const currentUrl = page.url();
      if (!currentUrl.includes('/login')) {
        results.passed.push('Admin Login: Success');
        console.log('✅ Admin Login: Success');
      } else {
        results.failed.push('Admin Login: Still on login page');
        console.log('❌ Admin Login: Failed');
      }
    } catch (e) {
      results.failed.push(`Admin Login: ${e.message}`);
      console.log(`❌ Admin Login failed: ${e.message}`);
    }

    // Test 6: Admin News List
    console.log('\n=== Test 6: Admin News List ===');
    try {
      await page.goto(`${BASE_URL}/admin/news`, { timeout: 15000 });
      await wait(2000);
      await screenshot(page, '07-admin-news-list');
      
      // Check admin news table
      const table = await page.locator('table, .el-table, [class*="table"]').first();
      const hasTable = await table.isVisible().catch(() => false);
      
      if (hasTable) {
        const rows = await page.locator('table tr, .el-table__row').count();
        results.passed.push(`Admin News: Table with ${rows} rows`);
        console.log(`✅ Admin News: Table found with ${rows} rows`);
      } else {
        results.issues.push('Admin News: Table not found');
      }
      
      // Check add button
      const addBtn = await page.locator('button:has-text("新增"), button:has-text("新建"), button:has-text("创建")').first();
      const hasAddBtn = await addBtn.isVisible().catch(() => false);
      
      if (hasAddBtn) {
        results.passed.push('Admin News: Add button visible');
      }
    } catch (e) {
      results.failed.push(`Admin News: ${e.message}`);
    }

    // Test 7: News Create Form
    console.log('\n=== Test 7: News Create Form ===');
    try {
      // Click add button if exists
      const addBtn = await page.locator('button:has-text("新增"), button:has-text("新建"), button:has-text("创建")').first();
      const hasAddBtn = await addBtn.isVisible().catch(() => false);
      
      if (hasAddBtn) {
        await addBtn.click();
        await wait(1500);
        await screenshot(page, '08-news-create-form');
        
        // Check form fields
        const titleInput = await page.locator('input[name="title"], input[placeholder*="标题"]').first();
        const hasTitleInput = await titleInput.isVisible().catch(() => false);
        
        if (hasTitleInput) {
          // Fill form
          await titleInput.fill(`自动化测试新闻 ${Date.now()}`);
          
          const contentEditor = await page.locator('textarea[name="content"], .editor, [class*="editor"]').first();
          const hasEditor = await contentEditor.isVisible().catch(() => false);
          
          if (hasEditor) {
            await contentEditor.fill('这是自动化测试的新闻内容。');
          }
          
          await screenshot(page, '09-news-form-filled');
          results.passed.push('News Create: Form filled');
          console.log('✅ News Create: Form filled');
          
          // Cancel instead of submit to avoid creating real data
          const cancelBtn = await page.locator('button:has-text("取消"), button:has-text("关闭")').first();
          await cancelBtn.click().catch(() => {});
          await wait(500);
        } else {
          results.issues.push('News Create: Title input not found');
        }
      } else {
        results.issues.push('News Create: Add button not found');
      }
    } catch (e) {
      results.issues.push(`News Create: ${e.message}`);
    }

    // Test 8: Edit/Delete buttons
    console.log('\n=== Test 8: Edit/Delete Operations ===');
    try {
      await page.goto(`${BASE_URL}/admin/news`, { timeout: 15000 });
      await wait(2000);
      
      const editBtn = await page.locator('button:has-text("编辑"), button:has-text("修改")').first();
      const deleteBtn = await page.locator('button:has-text("删除")').first();
      
      const hasEdit = await editBtn.isVisible().catch(() => false);
      const hasDelete = await deleteBtn.isVisible().catch(() => false);
      
      await screenshot(page, '10-admin-operations');
      
      if (hasEdit && hasDelete) {
        results.passed.push('Admin Operations: Edit/Delete buttons visible');
        console.log('✅ Admin Operations: Edit/Delete buttons found');
      } else {
        results.issues.push('Admin Operations: Missing Edit or Delete button');
      }
    } catch (e) {
      results.issues.push(`Admin Operations: ${e.message}`);
    }

  } finally {
    await browser.close();
  }

  // Print summary
  console.log('\n' + '='.repeat(50));
  console.log('TEST SUMMARY');
  console.log('='.repeat(50));
  console.log(`✅ Passed: ${results.passed.length}`);
  results.passed.forEach(r => console.log(`   - ${r}`));
  
  console.log(`\n❌ Failed: ${results.failed.length}`);
  results.failed.forEach(r => console.log(`   - ${r}`));
  
  console.log(`\n⚠️ Issues: ${results.issues.length}`);
  results.issues.forEach(r => console.log(`   - ${r}`));

  // Save results
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      passed: results.passed.length,
      failed: results.failed.length,
      issues: results.issues.length
    },
    details: results
  };
  
  fs.writeFileSync(path.join(SCREENSHOT_DIR, 'test-news-report.json'), JSON.stringify(report, null, 2));
  console.log(`\nReport saved to: ${SCREENSHOT_DIR}/test-news-report.json`);
}

runTests().catch(console.error);
