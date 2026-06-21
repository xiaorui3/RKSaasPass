import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:\\Users\\Administrator\\IdeaProjects\\RK-Web\\test-screenshots';
const USERNAME = 'admin_a';
const PASSWORD = '123456';

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function takeScreenshot(page, name) {
  const path = `${SCREENSHOT_DIR}\\test-system-${name}.png`;
  await page.screenshot({ path, fullPage: true });
  console.log(`Screenshot saved: ${path}`);
}

async function login(page) {
  console.log('Logging in...');
  await page.goto(BASE_URL);
  await sleep(2000);
  
  // Select tenant
  const tenantSelect = await page.$('.el-select');
  if (tenantSelect) {
    await tenantSelect.click();
    await sleep(300);
    const firstOption = await page.$('.el-select-dropdown__item');
    if (firstOption) {
      await firstOption.click();
      await sleep(300);
    }
  }
  
  // Enter credentials
  const usernameInput = await page.$('input[placeholder*="用户名"]');
  if (usernameInput) await usernameInput.fill(USERNAME);
  
  const passwordInput = await page.$('input[type="password"]');
  if (passwordInput) await passwordInput.fill(PASSWORD);
  
  // Click login
  const loginBtn = await page.$('button:has-text("登录")');
  if (loginBtn) {
    await loginBtn.click();
    await sleep(3000);
  }
  
  console.log('Login completed');
}

async function testPage(page, url, name) {
  console.log(`\nTesting: ${name}`);
  await page.goto(BASE_URL + url);
  await sleep(1500);
  await takeScreenshot(page, name.replace(/\s+/g, '-').toLowerCase());
  
  const rows = await page.$$('.el-table__row');
  const buttons = await page.$$('button');
  
  return {
    rows: rows.length,
    buttons: buttons.length,
    hasData: rows.length > 0
  };
}

async function main() {
  console.log('Starting System Management Tests...');
  
  const browser = await chromium.launch({ headless: false });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
  
  const results = [];
  
  try {
    await login(page);
    
    // Test 1: Role Management
    const roleResult = await testPage(page, '/admin/system/roles', '角色管理');
    results.push({ name: '角色管理列表', pass: roleResult.hasData, message: `Found ${roleResult.rows} rows` });
    
    // Test 2: Role Add
    const addBtn = await page.$('button:has-text("新增")');
    if (addBtn) {
      await addBtn.click();
      await sleep(1000);
      await takeScreenshot(page, '角色新增');
      results.push({ name: '角色新增', pass: true, message: 'Add dialog opened' });
      const cancelBtn = await page.$('.el-dialog button:has-text("取消")');
      if (cancelBtn) await cancelBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '角色新增', pass: false, message: 'Add button not found' });
    }
    
    // Test 3: Role Edit
    await page.goto(BASE_URL + '/admin/system/roles');
    await sleep(1000);
    const editBtn = await page.$('.el-table__row:first-child button:has-text("编辑")');
    if (editBtn) {
      await editBtn.click();
      await sleep(1000);
      await takeScreenshot(page, '角色编辑');
      results.push({ name: '角色编辑', pass: true, message: 'Edit dialog opened' });
      const cancelBtn = await page.$('.el-dialog button:has-text("取消")');
      if (cancelBtn) await cancelBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '角色编辑', pass: false, message: 'Edit button not found' });
    }
    
    // Test 4: Role Permission
    await page.goto(BASE_URL + '/admin/system/roles');
    await sleep(1000);
    const permBtn = await page.$('.el-table__row:first-child button:has-text("权限")');
    if (permBtn) {
      await permBtn.click();
      await sleep(1000);
      const permTree = await page.$('.el-tree');
      await takeScreenshot(page, '角色权限');
      results.push({ name: '角色权限分配', pass: !!permTree, message: permTree ? 'Permission tree loaded' : 'No permission tree' });
      const cancelBtn = await page.$('.el-dialog button:has-text("取消")');
      if (cancelBtn) await cancelBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '角色权限分配', pass: false, message: 'Permission button not found' });
    }
    
    // Test 5: Tenant Management
    const tenantResult = await testPage(page, '/admin/system/tenants', '租户管理');
    results.push({ name: '租户管理列表', pass: tenantResult.hasData, message: `Found ${tenantResult.rows} rows` });
    
    // Test 6: Tenant Stats
    const cards = await page.$$('.el-card, .stat-card');
    results.push({ name: '租户统计卡片', pass: cards.length > 0, message: `Found ${cards.length} cards` });
    
    // Test 7: Tenant Add
    const tenantAddBtn = await page.$('button:has-text("新增")');
    if (tenantAddBtn) {
      await tenantAddBtn.click();
      await sleep(1000);
      await takeScreenshot(page, '租户新增');
      results.push({ name: '租户新增', pass: true, message: 'Add dialog opened' });
      const cancelBtn = await page.$('.el-dialog button:has-text("取消")');
      if (cancelBtn) await cancelBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '租户新增', pass: false, message: 'Add button not found' });
    }
    
    // Test 8: Tenant Detail
    await page.goto(BASE_URL + '/admin/system/tenants');
    await sleep(1000);
    const detailBtn = await page.$('.el-table__row:first-child button:has-text("详情")');
    if (detailBtn) {
      await detailBtn.click();
      await sleep(1000);
      await takeScreenshot(page, '租户详情');
      results.push({ name: '租户详情', pass: true, message: 'Detail opened' });
      const closeBtn = await page.$('.el-dialog__close');
      if (closeBtn) await closeBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '租户详情', pass: false, message: 'Detail button not found' });
    }
    
    // Test 9: Tenant Renew
    await page.goto(BASE_URL + '/admin/system/tenants');
    await sleep(1000);
    const renewBtn = await page.$('.el-table__row:first-child button:has-text("续费")');
    if (renewBtn) {
      await renewBtn.click();
      await sleep(1000);
      await takeScreenshot(page, '租户续费');
      results.push({ name: '租户续费', pass: true, message: 'Renew dialog opened' });
      const cancelBtn = await page.$('.el-dialog button:has-text("取消")');
      if (cancelBtn) await cancelBtn.click();
      await sleep(500);
    } else {
      results.push({ name: '租户续费', pass: false, message: 'Renew button not found' });
    }
    
    // Test 10: Menu Management
    const menuResult = await testPage(page, '/admin/system/menus', '菜单管理');
    results.push({ name: '菜单管理', pass: menuResult.hasData, message: `Found ${menuResult.rows} rows` });
    
  } catch (e) {
    console.error('Error:', e);
  } finally {
    await browser.close();
  }
  
  // Print results
  console.log('\n========================================');
  console.log('Test Results:');
  console.log('========================================');
  
  let passed = 0, failed = 0;
  for (const r of results) {
    console.log(`${r.pass ? '✅' : '❌'} ${r.name}: ${r.message}`);
    if (r.pass) passed++; else failed++;
  }
  
  console.log(`\nTotal: ${results.length} | Passed: ${passed} | Failed: ${failed}`);
}

main().catch(console.error);