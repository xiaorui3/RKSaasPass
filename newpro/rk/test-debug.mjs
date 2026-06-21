import { chromium } from 'playwright';

const BASE_URL = 'http://localhost:5173';
const SCREENSHOT_DIR = 'C:\\Users\\Administrator\\IdeaProjects\\RK-Web\\test-screenshots';

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
  console.log('Starting browser...');
  
  const browser = await chromium.launch({ headless: false });
  const context = await browser.newContext({ viewport: { width: 1920, height: 1080 } });
  const page = await context.newPage();
  
  try {
    console.log('Navigating to:', BASE_URL);
    await page.goto(BASE_URL);
    await sleep(3000);
    
    // 截图初始状态
    await page.screenshot({ path: `${SCREENSHOT_DIR}\\test-system-00-initial.png`, fullPage: true });
    console.log('Screenshot saved: test-system-00-initial.png');
    
    // 打印页面HTML结构
    const html = await page.content();
    console.log('\n=== Page HTML (first 5000 chars) ===');
    console.log(html.substring(0, 5000));
    
    // 查找所有输入框
    const inputs = await page.$$('input');
    console.log(`\nFound ${inputs.length} input elements`);
    
    for (let i = 0; i < inputs.length; i++) {
      const input = inputs[i];
      const type = await input.getAttribute('type');
      const placeholder = await input.getAttribute('placeholder');
      const name = await input.getAttribute('name');
      const className = await input.getAttribute('class');
      console.log(`Input ${i}: type=${type}, placeholder=${placeholder}, name=${name}, class=${className}`);
    }
    
    // 查找所有按钮
    const buttons = await page.$$('button');
    console.log(`\nFound ${buttons.length} button elements`);
    
    for (let i = 0; i < buttons.length; i++) {
      const btn = buttons[i];
      const text = await btn.textContent();
      console.log(`Button ${i}: ${text}`);
    }
    
    // 检查是否有登录模态框
    const dialogs = await page.$$('.el-dialog, .ant-modal, .login-modal, [role="dialog"]');
    console.log(`\nFound ${dialogs.length} dialog/modal elements`);
    
    // 尝试点击登录按钮
    const loginBtn = await page.$('button:has-text("登录")');
    if (loginBtn) {
      console.log('\nClicking login button...');
      await loginBtn.click();
      await sleep(2000);
      
      await page.screenshot({ path: `${SCREENSHOT_DIR}\\test-system-00-after-login-click.png`, fullPage: true });
      console.log('Screenshot saved: test-system-00-after-login-click.png');
      
      // 再次查找输入框
      const inputsAfter = await page.$$('input');
      console.log(`\nAfter login click - Found ${inputsAfter.length} input elements`);
      
      for (let i = 0; i < inputsAfter.length; i++) {
        const input = inputsAfter[i];
        const type = await input.getAttribute('type');
        const placeholder = await input.getAttribute('placeholder');
        const visible = await input.isVisible();
        const enabled = await input.isEnabled();
        console.log(`Input ${i}: type=${type}, placeholder=${placeholder}, visible=${visible}, enabled=${enabled}`);
      }
    }
    
    await sleep(5000);
    
  } catch (e) {
    console.error('Error:', e);
  } finally {
    await browser.close();
  }
}

main().catch(console.error);
