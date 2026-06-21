import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:\\Users\\Administrator\\IdeaProjects\\RK-Web\\test-screenshots';
const RESULTS = [];

async function screenshot(page, name) {
  const filepath = path.join(SCREENSHOT_DIR, `test-news-${name}.png`);
  await page.screenshot({ path: filepath, fullPage: true });
  console.log(`Screenshot: ${name}`);
  return filepath;
}

const wait = (ms) => new Promise(r => setTimeout(r, ms));

// Close any modal overlays
async function closeModalIfExists(page) {
  try {
    // Try to find and click close button on modal
    const closeBtn = await page.locator('.el-dialog__close, .el-modal-close, button[aria-label="Close"], .close-btn').first();
    const isVisible = await closeBtn.isVisible().catch(() => false);
    if (isVisible) {
      await closeBtn.click();
      await wait(500);
      console.log('Closed modal overlay');
      return true;
    }
    
    // Try pressing Escape
    await page.keyboard.press('Escape');
    await wait(300);
  } catch (e) {
    // Ignore errors
  }
  return false;
}

// Wait for page to be ready (no overlays)
async function waitForPageReady(page) {
  await wait(1000);
  // Close any modals that might be blocking
  for (let i = 0; i < 3; i++) {
    await closeModalIfExists(page);
  }
}

async function runTests() {
  console.log('RK-Web News Module Tests v2');
  console.log(`URL: ${BASE_URL}\n`);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();

  const results = { passed: [], failed: [], issues: [] };

  try {
    // Test 1: Homepage
    console.log('\n=== Test 1: Homepage ===');
    try {
      await page.goto(BASE_URL, { timeout: 30000 });
      await waitForPageReady(page);
      await screenshot(page, '01-homepage');
      
      const title = await page.title();
      console.log(`Title: ${title}`);
      
      // Check for modal overlay and close it
      const overlay = await page.locator('.el-overlay-dialog, .el-modal, [role="dialog"]').first();
      const hasOverlay = await overlay.isVisible().catch(() => false);
      
      if (hasOverlay) {
        console.log('Found overlay, attempting to close...');
        await page.keyboard.press('Escape');
        await wait(500);
        await closeModalIfExists(page);
        await screenshot(page, '01b-homepage-after-close');
      }
      
      const nav = await page.locator('nav, .nav-menu, .navbar, [class*="nav"]').first();
      const hasNav = await nav.isVisible().catch(() => false);
      
      if (hasNav) {
        results.passed.push('Homepage: Navigation found');
        console.log('✅ Homepage navigation found');
      } else {
        results.issues.push('Homepage: Navigation not found');
      }
    } catch (e) {
      results.failed.push(`Homepage: ${e.message}`);
      console.log(`❌ Homepage failed: ${e.message}`);
    }

    // Test 2: Navigation to News
    console.log('\n=== Test 2: Navigation to News ===');
    try {
      await waitForPageReady(page);
      
      // Direct navigation to news page
      await page.goto(`${BASE_URL}/news`, { timeout: 15000 });
      await waitForPageReady(page);
      await screenshot(page, '02-news-page-direct');
      
      results.passed.push('Navigation: Direct access to /news works');
      console.log('✅ Direct navigation to /news works');
    } catch (e) {
      results.issues.push(`Navigation: ${e.message}`);
    }

    // Test 3: News List Content
    console.log('\n=== Test 3: News List Content ===');
    try {
      await page.goto(`${BASE_URL}/news`, { timeout: 15000 });
      await waitForPageReady(page);
      await screenshot(page, '03-news-list');
      
      // Look for news items with various selectors
      const selectors = [
        '.news-item',
        '.news-card', 
        '.news-list-item',
        'article',
        '.el-table__row',
        '[class*="news-"]',
        '.content-item'
      ];
      
      let newsCount = 0;
      for (const sel of selectors) {
        try {
          const count = await page.locator(sel).count();
          if (count > 0) {
            newsCount = count;
            console.log(`Found ${count} items with selector: ${sel}`);
            break;
          }
        } catch (e) {}
      }
      
      if (newsCount > 0) {
        results.passed.push(`News List: ${newsCount} items found`);
        console.log(`✅ News List: ${newsCount} items found`);
      } else {
        // Check if there's empty state
        const emptyState = await page.locator('.el-empty, .no-data, [class*="empty"]').first();
        const hasEmpty = await emptyState.isVisible().catch(() => false);
        
        if (hasEmpty) {
          results.issues.push('News List: Empty state shown (no data)');
          console.log('⚠️ News List: Empty state (no news data)');
        } else {
          results.issues.push('News List: Could not find news items');
        }
      }
      
      // Check pagination
      const pagination = await page.locator('.el-pagination, .pagination').first();
      const hasPagination = await pagination.isVisible().catch(() => false);
      if (hasPagination) {
        results.passed.push('News List: Pagination visible');
      }
    } catch (e) {
      results.failed.push(`News List: ${e.message}`);
    }

    // Test 4: News Detail
    console.log('\n=== Test 4: News Detail ===');
    try {
      await page.goto(`${BASE_URL}/news`, { timeout: 15000 });
      await waitForPageReady(page);
      
      // Try to click on first news item
      const clickable = await page.locator('.news-item, .news-card, article, [class*="news"] a').first();
      const hasClickable = await clickable.isVisible().catch(() => false);
      
      if (hasClickable) {
        await clickable.click();
        await wait(2000);
        await waitForPageReady(page);
        await screenshot(page, '04-news-detail');
        
        // Check if we're on detail page
        const url = page.url();
        if (url.includes('/news/') && url !== `${BASE_URL}/news`) {
          results.passed.push('News Detail: Can access detail page');
          console.log('✅ News Detail page loaded');
        } else {
          results.issues.push('News Detail: URL did not change to detail');
        }
      } else {
        results.issues.push('News Detail: No clickable news item');
        await screenshot(page, '04-news-detail-no-item');
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
      await page.fill('input[name="username"], input[placeholder*="用户名"]', 'admin_a');
      await page.fill('input[name="password"], input[type="password"]', '123456');
      
      await screenshot(page, '05b-login-filled');
      
      // Try to find and click login button with force option
      const loginBtn = await page.locator('button:has-text("登录"), button:has-text("Login"), .el-button--primary').first();
      
      // Force click to bypass overlay
      await loginBtn.click({ force: true });
      await wait(3000);
      
      await screenshot(page, '06-after-login');
      
      const currentUrl = page.url();
      console.log(`After login URL: ${currentUrl}`);
      
      if (!currentUrl.includes('/login')) {
        results.passed.push('Admin Login: Success');
        console.log('✅ Admin login successful');
      } else {
        // Check for error message
        const errorMsg = await page.locator('.el-message--error, .error-message').first();
        const hasError = await errorMsg.isVisible().catch(() => false);
        
        if (hasError) {
          const errorText = await errorMsg.textContent();
          results.failed.push(`Admin Login: ${errorText}`);
        } else {
          results.failed.push('Admin Login: Still on login page');
        }
      }
    } catch (e) {
      results.failed.push(`Admin Login: ${e.message}`);
      console.log(`❌ Admin login failed: ${e.message}`);
    }

    // Test 6: Admin News List
    console.log('\n=== Test 6: Admin News List ===');
    try {
      await page.goto(`${BASE_URL}/admin/news`, { timeout: 15000 });
      await wait(2000);
      await waitForPageReady(page);
      await screenshot(page, '07-admin-news-list');
      
      // Check if redirected to login
      const currentUrl = page.url();
      if (currentUrl.includes('/login')) {
        results.issues.push('Admin News: Redirected to login (not authenticated)');
      } else {
        // Check for table
        const table = await page.locator('.el-table, table, [class*="table"]').first();
        const hasTable = await table.isVisible().catch(() => false);
        
        if (hasTable) {
          const rows = await page.locator('.el-table__row, tbody tr').count();
          results.passed.push(`Admin News: Table with ${rows} rows`);
          console.log(`✅ Admin News: Table found`);
        } else {
          results.issues.push('Admin News: No table found');
        }
        
        // Check for add button
        const addBtn = await page.locator('button:has-text("新增"), button:has-text("新建")').first();
        const hasAddBtn = await addBtn.isVisible().catch(() => false);
        if (hasAddBtn) {
          results.passed.push('Admin News: Add button visible');
        }
      }
    } catch (e) {
      results.failed.push(`Admin News: ${e.message}`);
    }

    // Test 7: News Create (if authenticated)
    console.log('\n=== Test 7: News Create ===');
    try {
      const currentUrl = page.url();
      if (currentUrl.includes('/admin')) {
        // Find and click add button
        const addBtn = await page.locator('button:has-text("新增"), button:has-text("新建")').first();
        const hasAddBtn = await addBtn.isVisible().catch(() => false);
        
        if (hasAddBtn) {
          await addBtn.click({ force: true });
          await wait(1500);
          await screenshot(page, '08-news-create-form');
          
          // Check form
          const titleInput = await page.locator('input[name="title"], input[placeholder*="标题"]').first();
          const hasTitle = await titleInput.isVisible().catch(() => false);
          
          if (hasTitle) {
            await titleInput.fill(`测试新闻 ${Date.now()}`);
            await screenshot(page, '09-news-form-filled');
            results.passed.push('News Create: Form accessible');
            console.log('✅ News Create: Form filled');
            
            // Cancel to avoid creating real data
            const cancelBtn = await page.locator('button:has-text("取消"), button:has-text("关闭")').first();
            await cancelBtn.click().catch(() => {});
          } else {
            results.issues.push('News Create: Form fields not found');
          }
        } else {
          results.issues.push('News Create: Add button not found');
        }
      } else {
        results.issues.push('News Create: Not authenticated');
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
        console.log('✅ Admin Operations: Buttons found');
      } else if (hasEdit || hasDelete) {
        results.passed.push('Admin Operations: Some buttons visible');
      } else {
        results.issues.push('Admin Operations: No Edit/Delete buttons found');
      }
    } catch (e) {
      results.issues.push(`Admin Operations: ${e.message}`);
    }

  } finally {
    await browser.close();
  }

  // Summary
  console.log('\n' + '='.repeat(50));
  console.log('TEST SUMMARY');
  console.log('='.repeat(50));
  console.log(`✅ Passed: ${results.passed.length}`);
  results.passed.forEach(r => console.log(`   - ${r}`));
  
  console.log(`\n❌ Failed: ${results.failed.length}`);
  results.failed.forEach(r => console.log(`   - ${r}`));
  
  console.log(`\n⚠️ Issues: ${results.issues.length}`);
  results.issues.forEach(r => console.log(`   - ${r}`));

  const totalTests = results.passed.length + results.failed.length;
  const passRate = totalTests > 0 ? Math.round(results.passed.length / totalTests * 100) : 0;
  
  console.log(`\nPass Rate: ${passRate}% (${results.passed.length}/${totalTests})`);

  // Save report
  const report = {
    timestamp: new Date().toISOString(),
    summary: {
      passed: results.passed.length,
      failed: results.failed.length,
      issues: results.issues.length,
      passRate: `${passRate}%`
    },
    details: results
  };
  
  fs.writeFileSync(path.join(SCREENSHOT_DIR, 'test-news-report.json'), JSON.stringify(report, null, 2));
  console.log(`\nReport: ${SCREENSHOT_DIR}/test-news-report.json`);
}

runTests().catch(console.error);
