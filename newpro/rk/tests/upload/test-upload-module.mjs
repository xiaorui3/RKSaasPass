/**
 * 鏂囦欢涓婁紶妯″潡娣卞害娴嬭瘯
 * 娴嬭瘯鍚勭鏂囦欢涓婁紶鍦烘櫙鍜岃竟鐣屾潯浠躲€? */

import { chromium } from 'playwright';
import fs from 'fs';
import path from 'path';

const BASE_URL = 'http://localhost:5173';
const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010';
const SCREENSHOT_DIR = './test-screenshots/upload-test';

// 娴嬭瘯璐﹀彿
const TEST_ACCOUNTS = {
  admin: { username: 'admin_a', password: 'change-me' },
  member: { username: 'member_a', password: '123456' }
};

// 鍒涘缓娴嬭瘯鍥剧墖鏂囦欢
function createTestImage(size, format = 'png') {
  const testDir = './test-files';
  if (!fs.existsSync(testDir)) {
    fs.mkdirSync(testDir, { recursive: true });
  }

  const fileName = `test-${size}x${size}.${format}`;
  const filePath = path.join(testDir, fileName);

  // 鍒涘缓涓€涓畝鍗曠殑 PNG 鍥剧墖缂撳啿鍖?  const content = Buffer.alloc(size * size * 4); // RGBA

  fs.writeFileSync(filePath, content);
  return filePath;
}

// 鍒涘缓娴嬭瘯鏂囨湰鏂囦欢锛堟ā鎷熸伓鎰忔枃浠讹級
function createMaliciousFile() {
  const testDir = './test-files';
  if (!fs.existsSync(testDir)) {
    fs.mkdirSync(testDir, { recursive: true });
  }

  const filePath = path.join(testDir, 'malicious.js');
  fs.writeFileSync(filePath, '<script>alert("XSS")</script>');
  return filePath;
}

// 鍒涘缓瓒呭ぇ鏂囦欢
function createLargeFile(sizeInMB) {
  const testDir = './test-files';
  if (!fs.existsSync(testDir)) {
    fs.mkdirSync(testDir, { recursive: true });
  }

  const filePath = path.join(testDir, `large-${sizeInMB}mb.dat`);
  const buffer = Buffer.alloc(sizeInMB * 1024 * 1024);
  fs.writeFileSync(filePath, buffer);
  return filePath;
}

// 鍒涘缓娴嬭瘯鐢ㄧ殑鐪熷疄鍥剧墖
async function createRealTestImage(width, height) {
  const testDir = './test-files';
  if (!fs.existsSync(testDir)) {
    fs.mkdirSync(testDir, { recursive: true });
  }

  // 鍒涘缓涓€涓畝鍗曠殑褰╄壊PNG
  const filePath = path.join(testDir, `image-${width}x${height}.png`);

  // 绠€鍗曠殑PNG鏂囦欢澶村拰鏁版嵁
  const pngSignature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);

  // 鍒涘缓鏈€灏忕殑PNG鏂囦欢
  const IHDR = Buffer.from([
    0, 0, 0, 13, // Length
    73, 72, 68, 82, // IHDR
    (width >> 24) & 0xff, (width >> 16) & 0xff, (width >> 8) & 0xff, width & 0xff,
    (height >> 24) & 0xff, (height >> 16) & 0xff, (height >> 8) & 0xff, height & 0xff,
    8, // bit depth
    2, // color type (RGB)
    0, 0, 0, // compression, filter, interlace
    0, 0, 0, 0 // CRC
  ]);

  const IDAT = Buffer.from([
    0, 0, 0, 14, // Length
    73, 68, 65, 84, // IDAT
    120, 1, // compression method and flags
    ...Buffer.alloc(width * height * 3 + 6), // image data
    0, 0, 0, 0 // CRC
  ]);

  const IEND = Buffer.from([0, 0, 0, 0, 73, 69, 78, 68, 174, 66, 96, 130]);

  fs.writeFileSync(filePath, Buffer.concat([pngSignature, IHDR, IDAT, IEND]));
  return filePath;
}

class UploadTestRunner {
  constructor() {
    this.browser = null;
    this.page = null;
    this.context = null;
    this.testResults = [];
    this.uploadedFiles = [];
  }

  async init() {
    console.log('鍚姩娴忚鍣?..');
    this.browser = await chromium.launch({
      headless: false,
      slowMo: 500
    });
    this.context = await this.browser.newContext({
      viewport: { width: 1920, height: 1080 }
    });
    this.page = await this.context.newPage();

    // 鍒涘缓鎴浘鐩綍
    if (!fs.existsSync(SCREENSHOT_DIR)) {
      fs.mkdirSync(SCREENSHOT_DIR, { recursive: true });
    }
  }

  async cleanup() {
    if (this.page) {
      await this.page.close();
    }
    if (this.browser) {
      await this.browser.close();
    }
  }

  async login(account) {
    console.log(`鐧诲綍鐢ㄦ埛: ${account.username}`);
    await this.page.goto(`${BASE_URL}/login`);
    await this.page.waitForLoadState('networkidle');

    await this.page.fill('input[placeholder="璇疯緭鍏ョ敤鎴峰悕"]', account.username);
    await this.page.fill('input[type="password"]', account.password);
    await this.page.click('button[type="submit"]');

    await this.page.waitForLoadState('networkidle');
    await this.page.screenshot({ path: `${SCREENSHOT_DIR}/after-login.png` });
    console.log('鐧诲綍鎴愬姛');
  }

  async testNewsImageUpload(imagePath) {
    console.log('\n娴嬭瘯鏂伴椈鍥剧墖涓婁紶...');
    try {
      await this.page.goto(`${BASE_URL}/#/admin/content/news`);
      await this.page.waitForLoadState('networkidle');

      // 鐐瑰嚮娣诲姞鎸夐挳
      await this.page.click('button:has-text("娣诲姞")');
      await this.page.waitForTimeout(500);

      // 鏌ユ壘鏂囦欢涓婁紶鍏冪礌
      const fileInput = await this.page.$('input[type="file"]');

      if (fileInput) {
        await fileInput.setInputFiles(imagePath);
        await this.page.waitForTimeout(2000);

        const fileName = path.basename(imagePath);
        const success = await this.page.isVisible(`text=${fileName}`);

        this.testResults.push({
          test: '鏂伴椈鍥剧墖涓婁紶',
          file: fileName,
          status: success ? 'PASS' : 'FAIL',
          message: success ? '鍥剧墖涓婁紶鎴愬姛' : '鍥剧墖涓婁紶澶辫触'
        });

        await this.page.screenshot({ path: `${SCREENSHOT_DIR}/news-upload-${fileName}.png` });
      } else {
        this.testResults.push({
          test: '鏂伴椈鍥剧墖涓婁紶',
          file: path.basename(imagePath),
          status: 'FAIL',
          message: '鏈壘鍒版枃浠朵笂浼犲厓'
        });
      }
    } catch (error) {
      this.testResults.push({
        test: '鏂伴椈鍥剧墖涓婁紶',
        file: path.basename(imagePath),
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testWorksCoverUpload(imagePath) {
    console.log('\n娴嬭瘯浣滃搧灏侀潰涓婁紶...');
    try {
      await this.page.goto(`${BASE_URL}/#/works`);
      await this.page.waitForLoadState('networkidle');

      // 鐐瑰嚮娣诲姞鎸夐挳
      const addButton = await this.page.$('button:has-text("娣诲姞")');
      if (addButton) {
        await addButton.click();
        await this.page.waitForTimeout(500);

        const fileInput = await this.page.$('input[type="file"]');

        if (fileInput) {
          await fileInput.setInputFiles(imagePath);
          await this.page.waitForTimeout(2000);

          const fileName = path.basename(imagePath);
          const success = await this.page.isVisible(`text=${fileName}`);

          this.testResults.push({
            test: '浣滃搧灏侀潰涓婁紶',
            file: fileName,
            status: success ? 'PASS' : 'FAIL',
            message: success ? '灏侀潰涓婁紶鎴愬姛' : '灏侀潰涓婁紶澶辫触'
          });

          await this.page.screenshot({ path: `${SCREENSHOT_DIR}/works-upload-${fileName}.png` });
        }
      }
    } catch (error) {
      this.testResults.push({
        test: '浣滃搧灏侀潰涓婁紶',
        file: path.basename(imagePath),
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testAvatarUpload(imagePath) {
    console.log('\n娴嬭瘯澶村儚涓婁紶...');
    try {
      await this.page.goto(`${BASE_URL}/#/profile`);
      await this.page.waitForLoadState('networkidle');

      // 鏌ユ壘澶村儚涓婁紶鍖哄煙
      const avatarUpload = await this.page.$('.avatar-uploader');

      if (avatarUpload) {
        const fileInput = await avatarUpload.$('input[type="file"]');

        if (fileInput) {
          await fileInput.setInputFiles(imagePath);
          await this.page.waitForTimeout(2000);

          const fileName = path.basename(imagePath);
          const success = await avatarUpload.isVisible('.avatar-image');

          this.testResults.push({
            test: '澶村儚涓婁紶',
            file: fileName,
            status: success ? 'PASS' : 'FAIL',
            message: success ? '澶村儚涓婁紶鎴愬姛' : '澶村儚涓婁紶澶辫触'
          });

          await this.page.screenshot({ path: `${SCREENSHOT_DIR}/avatar-upload-${fileName}.png` });
        }
      } else {
        this.testResults.push({
          test: '澶村儚涓婁紶',
          file: path.basename(imagePath),
          status: 'SKIP',
          message: '鏈壘鍒板ご鍍忎笂浼犲厓'
        });
      }
    } catch (error) {
      this.testResults.push({
        test: '澶村儚涓婁紶',
        file: path.basename(imagePath),
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testFileSizeLimit(imagePath) {
    console.log('\n娴嬭瘯鏂囦欢澶у皬闄愬埗...');
    try {
      const stats = fs.statSync(imagePath);
      const fileSizeMB = (stats.size / (1024 * 1024)).toFixed(2);

      const response = await this.page.request.post(`${API_BASE}/api/news/upload/image`, {
        multipart: {
          file: fs.createReadStream(imagePath)
        }
      });

      const result = await response.json();

      const isOverLimit = stats.size > 5 * 1024 * 1024;
      const hasError = result.code !== 200;

      this.testResults.push({
        test: '鏂囦欢澶у皬闄愬埗',
        file: path.basename(imagePath),
        fileSize: `${fileSizeMB}MB`,
        status: (isOverLimit && hasError) || (!isOverLimit && !hasError) ? 'PASS' : 'FAIL',
        message: result.msg || '鏂囦欢澶у皬闄愬埗娴嬭瘯瀹屾垚'
      });

    } catch (error) {
      this.testResults.push({
        test: '鏂囦欢澶у皬闄愬埗',
        file: path.basename(imagePath),
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testFileTypeValidation(imagePath, expectedType) {
    console.log('\n娴嬭瘯鏂囦欢绫诲瀷楠岃瘉...');
    try {
      const response = await this.page.request.post(`${API_BASE}/api/news/upload/image`, {
        multipart: {
          file: fs.createReadStream(imagePath)
        }
      });

      const result = await response.json();

      const fileName = path.basename(imagePath);
      const ext = path.extname(fileName).toLowerCase();
      const allowedExts = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.bmp'];
      const isAllowed = allowedExts.includes(ext);

      this.testResults.push({
        test: '鏂囦欢绫诲瀷楠岃瘉',
        file: fileName,
        expectedType,
        status: (isAllowed && result.code === 200) || (!isAllowed && result.code !== 200) ? 'PASS' : 'FAIL',
        message: result.msg || '鏂囦欢绫诲瀷楠岃瘉瀹屾垚'
      });

    } catch (error) {
      this.testResults.push({
        test: '鏂囦欢绫诲瀷楠岃瘉',
        file: path.basename(imagePath),
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testBatchUpload(imagePaths) {
    console.log('\n娴嬭瘯鎵归噺涓婁紶...');
    try {
      const formData = new FormData();
      imagePaths.forEach(path => {
        formData.append('files', fs.createReadStream(path));
      });

      const response = await this.page.request.post(`${API_BASE}/api/news/upload/images`, {
        multipart: {
          files: imagePaths.map(p => fs.createReadStream(p))
        }
      });

      const result = await response.json();

      this.testResults.push({
        test: '鎵归噺涓婁紶',
        fileCount: imagePaths.length,
        status: result.code === 200 ? 'PASS' : 'FAIL',
        message: result.data?.message || '鎵归噺涓婁紶娴嬭瘯瀹屾垚'
      });

    } catch (error) {
      this.testResults.push({
        test: '鎵归噺涓婁紶',
        fileCount: imagePaths.length,
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testMaliciousFileUpload(filePath) {
    console.log('\n娴嬭瘯鎭舵剰鏂囦欢涓婁紶...');
    try {
      const response = await this.page.request.post(`${API_BASE}/api/news/upload/image`, {
        multipart: {
          file: fs.createReadStream(filePath)
        }
      });

      const result = await response.json();

      this.testResults.push({
        test: '鎭舵剰鏂囦欢涓婁紶',
        file: path.basename(filePath),
        status: result.code !== 200 ? 'PASS' : 'FAIL',
        message: result.msg || '鎭舵剰鏂囦欢琚嫤'
      });

    } catch (error) {
      this.testResults.push({
        test: '鎭舵剰鏂囦欢涓婁紶',
        file: path.basename(filePath),
        status: 'PASS',
        message: '鎭舵剰鏂囦欢琚嫤鎴? ' + error.message
      });
    }
  }

  async testFileDeletion(fileUrl) {
    console.log('\n娴嬭瘯鏂囦欢鍒犻櫎...');
    try {
      const response = await this.page.request.delete(`${API_BASE}/api/news/upload/image?url=${encodeURIComponent(fileUrl)}`);

      const result = await response.json();

      this.testResults.push({
        test: '鏂囦欢鍒犻櫎',
        file: fileUrl,
        status: result.code === 200 ? 'PASS' : 'FAIL',
        message: result.msg || '鏂囦欢鍒犻櫎娴嬭瘯瀹屾垚'
      });

    } catch (error) {
      this.testResults.push({
        test: '鏂囦欢鍒犻櫎',
        file: fileUrl,
        status: 'ERROR',
        message: error.message
      });
    }
  }

  async testFileExistsCheck(fileUrl) {
    console.log('\n娴嬭瘯鏂囦欢瀛樺湪鎬ф鏌?..');
    try {
      const response = await this.page.request.get(`${API_BASE}/api/news/upload/check?url=${encodeURIComponent(fileUrl)}`);

      const result = await response.json();

      this.testResults.push({
        test: '鏂囦欢瀛樺湪鎬ф',
        file: fileUrl,
        status: result.code === 200 ? 'PASS' : 'FAIL',
        message: result.msg || '鏂囦欢瀛樺湪鎬ф鏌ュ畬'
      });

    } catch (error) {
      this.testResults.push({
        test: '鏂囦欢瀛樺湪鎬ф',
        file: fileUrl,
        status: 'ERROR',
        message: error.message
      });
    }
  }

  generateReport() {
    console.log('\n========== 娴嬭瘯鎶ュ憡 ==========');
    console.log(`鎬绘祴璇曟暟: ${this.testResults.length}`);
    console.log(`閫氳繃: ${this.testResults.filter(r => r.status === 'PASS').length}`);
    console.log(`澶辫触: ${this.testResults.filter(r => r.status === 'FAIL').length}`);
    console.log(`閿欒: ${this.testResults.filter(r => r.status === 'ERROR').length}`);
    console.log(`璺宠繃: ${this.testResults.filter(r => r.status === 'SKIP').length}`);
    console.log('\n璇︾粏缁撴灉:');
    console.table(this.testResults);

    return {
      summary: {
        total: this.testResults.length,
        passed: this.testResults.filter(r => r.status === 'PASS').length,
        failed: this.testResults.filter(r => r.status === 'FAIL').length,
        error: this.testResults.filter(r => r.status === 'ERROR').length,
        skipped: this.testResults.filter(r => r.status === 'SKIP').length
      },
      details: this.testResults
    };
  }

  saveReport() {
    const report = this.generateReport();
    const reportPath = './ai-memory/test-results/upload-test-report.json';

    if (!fs.existsSync('./ai-memory/test-results')) {
      fs.mkdirSync('./ai-memory/test-results', { recursive: true });
    }

    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
    console.log(`\n鎶ュ憡宸蹭繚瀛樺埌: ${reportPath}`);
  }
}

async function runUploadTests() {
  const runner = new UploadTestRunner();

  try {
    await runner.init();

    // 鍑嗗娴嬭瘯鏂囦欢
    console.log('鍑嗗娴嬭瘯鏂囦欢...');
    const testFiles = {
      smallImage: await createRealTestImage(100, 100),
      mediumImage: await createRealTestImage(800, 600),
      largeImage: await createRealTestImage(1920, 1080),
      hugeImage: createLargeFile(6), // 6MB - 瓒呰繃5MB闄愬埗
      maliciousFile: createMaliciousFile()
    };

    // 鐧诲綍
    await runner.login(TEST_ACCOUNTS.admin);

    // 娴嬭瘯鍥剧墖涓婁紶
    await runner.testNewsImageUpload(testFiles.smallImage);
    await runner.testNewsImageUpload(testFiles.mediumImage);
    await runner.testNewsImageUpload(testFiles.largeImage);

    // 娴嬭瘯浣滃搧灏侀潰涓婁紶
    await runner.testWorksCoverUpload(testFiles.mediumImage);

    // 娴嬭瘯澶村儚涓婁紶
    await runner.testAvatarUpload(testFiles.smallImage);

    // 娴嬭瘯鏂囦欢澶у皬闄愬埗
    await runner.testFileSizeLimit(testFiles.smallImage);
    await runner.testFileSizeLimit(testFiles.hugeImage);

    // 娴嬭瘯鏂囦欢绫诲瀷楠岃瘉
    await runner.testFileTypeValidation(testFiles.smallImage, 'image/png');

    // 娴嬭瘯鎭舵剰鏂囦欢涓婁紶
    await runner.testMaliciousFileUpload(testFiles.maliciousFile);

    // 娴嬭瘯鎵归噺涓婁紶
    await runner.testBatchUpload([
      testFiles.smallImage,
      testFiles.mediumImage
    ]);

    // 鐢熸垚鎶ュ憡
    const report = runner.generateReport();
    runner.saveReport();

  } catch (error) {
    console.error('娴嬭瘯鎵ц澶辫触:', error);
  } finally {
    await runner.cleanup();
  }
}

// 鎵ц娴嬭瘯
runUploadTests().catch(console.error);
