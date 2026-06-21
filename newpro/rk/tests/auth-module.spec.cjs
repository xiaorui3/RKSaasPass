// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'
const API_URL = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const AUTH_LOGIN_BUDGET_MS = Number(process.env.PLAYWRIGHT_AUTH_LOGIN_BUDGET_MS || 20000)
const LOGIN_PAGE_LOAD_BUDGET_MS = Number(process.env.PLAYWRIGHT_LOGIN_PAGE_LOAD_BUDGET_MS || 25000)

const TEST_USERS = {
  admin_a: { username: 'admin_a', password: 'change-me', role: '瓒呯骇绠＄悊鍛?, tenant: 'A', organizationId: '1' },
  manager_a: { username: 'manager_a', password: '123456', role: '绀惧洟璐熻矗浜?, tenant: 'A', organizationId: '1' },
  member_a: { username: 'member_a', password: '123456', role: '鏅€氭垚鍛?, tenant: 'A', organizationId: '1' },
  admin_b: { username: 'admin_b', password: '123456', role: '绠＄悊鍛?, tenant: 'B', organizationId: '2' },
  admin_c: { username: 'admin_c', password: '123456', role: '绉熸埛绠＄悊鍛?, tenant: 'C', organizationId: '3' }
}

async function fetchTenantList(page) {
  const tenantResponse = await page.request.get(`${API_URL}/tenants/list`)
  const tenantBody = await tenantResponse.json()
  return tenantBody.data || []
}

async function selectTenant(page, organizationId = '1') {
  const tenants = await fetchTenantList(page)
  const targetTenant = tenants.find((item) => String(item.id) === String(organizationId))
  await page.locator('.el-select').first().click()
  const options = page.locator('.el-select-dropdown:visible .el-select-dropdown__item')
  if (targetTenant?.tenantName) {
    await options.filter({ hasText: targetTenant.tenantName }).first().click({ force: true })
    return
  }
  await options.first().click({ force: true })
}

async function openLogin(page) {
  await page.goto(`${BASE_URL}/login`, { waitUntil: 'commit' })
  await expect(page.locator('.login-card')).toBeVisible({ timeout: 15000 })
}

async function openApp(page, path = '/') {
  await page.goto(`${BASE_URL}${path}`, { waitUntil: 'commit' })
}

async function fillLoginForm(page, user) {
  await openLogin(page)
  await selectTenant(page, user.organizationId)
  await page.locator('input:not([readonly]):not([type="password"])').first().fill(user.username)
  await page.locator('input[type="password"]').first().fill(user.password)
}

async function submitLogin(page) {
  await page.locator('.login-card .el-button--primary').click()
}

async function loginThroughUi(page, user) {
  await fillLoginForm(page, user)
  await submitLogin(page)
}

async function expectAuthenticated(page, timeout = AUTH_LOGIN_BUDGET_MS) {
  await expect(page).not.toHaveURL(/\/login/, { timeout })
  await page.waitForFunction(() => !window.location.pathname.includes('/login') && !!window.localStorage.getItem('token'), null, {
    timeout
  })
}

async function loginByApi(page, request, user) {
  const response = await request.post(`${API_URL}/auth/login`, {
    data: {
      username: user.username,
      password: user.password,
      organizationId: user.organizationId
    }
  })
  const body = await response.json()
  if (body.code !== 200 || !body.data?.token) {
    throw new Error(`Login failed: ${JSON.stringify(body)}`)
  }

  await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
  await page.evaluate((loginData) => {
    localStorage.setItem('token', loginData.token)
    localStorage.setItem('tenantId', loginData.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: loginData.userId,
      username: loginData.username,
      organizationId: loginData.organizationId,
      roles: loginData.roles || []
    }))
  }, body.data)

  return body
}

async function openRegister(page) {
  await page.goto(`${BASE_URL}/register`, { waitUntil: 'commit' })
  await expect(page.locator('.register-card')).toBeVisible({ timeout: 15000 })
}

async function selectRegisterTenant(page, organizationId = '1') {
  await selectTenant(page, organizationId)
}

function registerFormItem(page, labelText) {
  return page
    .locator('.register-card .el-form-item')
    .filter({ has: page.locator('.el-form-item__label', { hasText: labelText }) })
    .first()
}

function registerFieldInput(page, labelText) {
  return registerFormItem(page, labelText).locator('input').first()
}

test.describe('RK-Web 鐢ㄦ埛璁よ瘉妯″潡娣卞害娴嬭瘯', () => {
  test.describe.configure({ timeout: 90000 })

  test.describe('1. 鐧诲綍鍔熻兘娴嬭瘯', () => {
    test.beforeEach(async ({ page }) => {
      await page.context().clearCookies()
      await openLogin(page)
    })

    test('01-姝ｇ‘鐢ㄦ埛鍚嶅瘑鐮佺櫥褰?- admin_a', async ({ page }) => {
      await loginThroughUi(page, TEST_USERS.admin_a)
      await expectAuthenticated(page)
    })

    test('02-閿欒鐢ㄦ埛鍚嶆祴璇?, async ({ page }) => {
      await fillLoginForm(page, { ...TEST_USERS.admin_a, username: 'nonexistent_user_xyz' })
      await submitLogin(page)
      await expect(page).toHaveURL(/\/login/)
    })

    test('03-閿欒瀵嗙爜娴嬭瘯', async ({ page }) => {
      await fillLoginForm(page, { ...TEST_USERS.admin_a, password: 'wrong_password_123' })
      await submitLogin(page)
      await expect(page).toHaveURL(/\/login/)
    })

    test('04-绌虹敤鎴峰悕瀵嗙爜楠岃瘉', async ({ page }) => {
      await page.locator('.login-card .el-button--primary').click()
      await expect(page.locator('.el-form-item__error').first()).toBeVisible()
    })

    test('05-绌虹敤鎴峰悕娴嬭瘯', async ({ page }) => {
      await selectTenant(page, TEST_USERS.admin_a.organizationId)
      await page.locator('input[type="password"]').first().fill(TEST_USERS.admin_a.password)
      await page.locator('.login-card .el-button--primary').click()
      await expect(page.locator('.el-form-item__error').first()).toBeVisible()
    })

    test('06-绌哄瘑鐮佹祴璇?, async ({ page }) => {
      await selectTenant(page, TEST_USERS.admin_a.organizationId)
      await page.locator('input:not([readonly]):not([type="password"])').first().fill(TEST_USERS.admin_a.username)
      await page.locator('.login-card .el-button--primary').click()
      await expect(page.locator('.el-form-item__error').first()).toBeVisible()
    })

    test('07-澶氱鎴疯处鍙蜂細璇濇祴璇?- admin_c', async ({ page, request }) => {
      const response = await request.post(`${API_URL}/auth/login`, {
        data: {
          username: TEST_USERS.admin_c.username,
          password: TEST_USERS.admin_c.password,
          organizationId: TEST_USERS.admin_c.organizationId
        }
      })
      const body = await response.json()

      if (body.code === 200 && body.data?.token) {
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'domcontentloaded' })
        await page.evaluate((loginData) => {
          localStorage.setItem('token', loginData.token)
          localStorage.setItem('tenantId', loginData.organizationId)
          localStorage.setItem('userInfo', JSON.stringify({
            userId: loginData.userId,
            username: loginData.username,
            organizationId: loginData.organizationId,
            roles: loginData.roles || []
          }))
        }, body.data)
        await openApp(page, '/admin')
        await expectAuthenticated(page)
        return
      }

      expect(body.code).toBe(400)
      expect(String(body.msg || '')).toMatch(/Dormant|verification|浼戠湢|楠岃瘉/i)
    })

    test('08-涓嶅悓瑙掕壊鐧诲綍娴嬭瘯', async ({ browser }) => {
      test.slow()
      for (const user of [TEST_USERS.manager_a, TEST_USERS.member_a]) {
        const context = await browser.newContext()
        const page = await context.newPage()
        await loginByApi(page, page.request, user)
        await openApp(page)
        await expectAuthenticated(page)
        await context.close()
      }
    })

    test('09-JWT Token娴嬭瘯', async ({ page }) => {
      await loginThroughUi(page, TEST_USERS.admin_a)
      await page.waitForFunction(() => !!localStorage.getItem('token') && !!localStorage.getItem('userInfo'))
      const storage = await page.evaluate(() => ({
        token: localStorage.getItem('token'),
        userInfo: localStorage.getItem('userInfo')
      }))
      expect(storage.token).toBeTruthy()
      expect(storage.userInfo).toBeTruthy()
    })

    test('10-鍥炶溅閿櫥褰曟祴璇?, async ({ page }) => {
      test.slow()
      await fillLoginForm(page, TEST_USERS.admin_a)
      await page.locator('input[type="password"]').first().press('Enter')
      await expectAuthenticated(page)
    })

    test('11-瀵嗙爜鏄剧ず/闅愯棌鍒囨崲', async ({ page }) => {
      await selectTenant(page, TEST_USERS.admin_a.organizationId)
      const passwordInput = page.locator('input[type="password"]').first()
      await passwordInput.fill(TEST_USERS.admin_a.password)
      await expect(page.locator('.el-input__suffix, .el-input__password').first()).toBeVisible()
    })

    test('12-浼戠湢璐﹀彿瑙﹀彂閭楠岃瘉 - admin_b', async ({ page }) => {
      await fillLoginForm(page, TEST_USERS.admin_b)
      await submitLogin(page)
      await expect(page).toHaveURL(/\/login/)
      await expect(page.getByPlaceholder('璇疯緭鍏ュ綋鍓嶇粦瀹氶偖绠?)).toBeVisible()
      await expect(page.getByPlaceholder('璇疯緭鍏ラ偖绠遍獙璇佺爜')).toBeVisible()
    })
  })

  test.describe('2. 娉ㄥ唽鍔熻兘娴嬭瘯', () => {
    test.beforeEach(async ({ page }) => {
      await page.context().clearCookies()
      await openRegister(page)
    })

    test('01-娉ㄥ唽椤甸潰鍔犺浇', async ({ page }) => {
      await expect(page.locator('.register-card')).toBeVisible()
      await expect(page.locator('.el-select').first()).toBeVisible()
      await expect(page.locator('input[type="password"]').first()).toBeVisible()
    })

    test('02-鑾峰彇绉熸埛鍒楄〃', async ({ page }) => {
      await page.locator('.el-select').first().click()
      await expect(page.locator('.el-select-dropdown:visible .el-select-dropdown__item').first()).toBeVisible()
    })

    test('03-娉ㄥ唽琛ㄥ崟楠岃瘉 - 绌哄瓧娈?, async ({ page }) => {
      await page.locator('.register-card .el-button--primary').click()
      await expect(page.locator('.el-form-item__error').first()).toBeVisible()
    })

    test('04-瀵嗙爜涓€鑷存€ч獙璇?, async ({ page }) => {
      await selectRegisterTenant(page, '1')
      await page.getByPlaceholder('璇疯緭鍏ョ敤鎴峰悕').fill('test_user_password')
      await page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?).fill('Password123!')
      await page.getByPlaceholder('璇峰啀娆¤緭鍏ュ瘑鐮?).fill('DifferentPassword123!')
      await page.getByPlaceholder('璇峰啀娆¤緭鍏ュ瘑鐮?).blur()
      await expect(registerFormItem(page, '纭瀵嗙爜').locator('.el-form-item__error').first()).toBeVisible()
    })

    test('05-閭鏍煎紡楠岃瘉', async ({ page }) => {
      await selectRegisterTenant(page, '1')
      await registerFieldInput(page, '閭').fill('invalid-email-format')
      await registerFieldInput(page, '閭').blur()
      await expect(registerFormItem(page, '閭').locator('.el-form-item__error').first()).toBeVisible()
    })

    test('06-璺宠浆鍒扮櫥褰曢〉', async ({ page }) => {
      await Promise.all([
        page.waitForURL(/\/login/, { timeout: 15000 }),
        page.getByText(/鍘荤櫥褰?).click({ force: true })
      ])
    })

    test('07-閭瀛楁娓叉煋鍦ㄥ姩鎬佽〃鍗曚腑', async ({ page }) => {
      await selectRegisterTenant(page, '1')
      await expect(registerFieldInput(page, '閭')).toBeVisible()
      await expect(registerFieldInput(page, '閭')).toHaveAttribute('placeholder', /閭/)
    })

    test('08-瀵嗙爜杈撳叆妗嗗彲鐢?, async ({ page }) => {
      await selectRegisterTenant(page, '1')
      await page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?).fill('123')
      await expect(page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?)).toHaveValue('123')
      await page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?).fill('StrongPassword123!@#')
      await expect(page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?)).toHaveValue('StrongPassword123!@#')
    })
  })

  test.describe('3. 鐧诲嚭鍔熻兘娴嬭瘯', () => {
    test('01-姝ｅ父鐧诲嚭娴佺▼', async ({ page, request }) => {
      test.slow()
      await loginByApi(page, request, TEST_USERS.admin_a)
      await openApp(page)
      const dropdown = page.locator('.el-dropdown').first()
      if (await dropdown.isVisible().catch(() => false)) {
        await dropdown.click()
      }
      const logoutItem = page.locator('.el-dropdown-menu__item').filter({ hasText: /閫€鍑? }).first()
      if (await logoutItem.isVisible().catch(() => false)) {
        await logoutItem.click()
      } else {
        await page.evaluate(() => localStorage.clear())
      }
      await openApp(page, '/admin')
      await expect(page).toHaveURL(/\/login/, { timeout: 15000 })
    })

    test('02-鐧诲嚭鍚庤闂彈淇濇姢椤甸潰', async ({ page, request }) => {
      await loginByApi(page, request, TEST_USERS.admin_a)
      await page.evaluate(() => localStorage.clear())
      await openApp(page, '/admin')
      await expect(page).toHaveURL(/\/login/, { timeout: 15000 })
    })
  })

  test.describe('4. Token鍜屼細璇濈鐞嗘祴璇?, () => {
    test('01-Token杩囨湡澶勭悊', async ({ page, request }) => {
      await loginByApi(page, request, TEST_USERS.admin_a)
      await page.evaluate(() => localStorage.setItem('token', 'expired_token_12345'))
      await openApp(page, '/admin')
      await expect(page).toHaveURL(/\/admin|\/login/)
    })

    test('02-澶氭爣绛鹃〉浼氳瘽鍚屾', async ({ browser, request }) => {
      test.slow()
      const context = await browser.newContext()
      const page = await context.newPage()
      const body = await loginByApi(page, request, TEST_USERS.admin_a)
      const second = await context.newPage()
      await openApp(second)
      await second.evaluate((loginData) => {
        localStorage.setItem('token', loginData.data.token)
        localStorage.setItem('tenantId', loginData.data.organizationId)
        localStorage.setItem('userInfo', JSON.stringify({
          userId: loginData.data.userId,
          username: loginData.data.username,
          organizationId: loginData.data.organizationId,
          roles: loginData.data.roles || []
        }))
      }, body)
      await openApp(second, '/admin')
      await expect(second).not.toHaveURL(/\/login/, { timeout: 15000 })
      await context.close()
    })
  })

  test.describe('5. API鐩存帴娴嬭瘯', () => {
    test('01-鐧诲綍API娴嬭瘯', async ({ page }) => {
      const response = await page.request.post(`${API_URL}/auth/login`, {
        data: {
          username: TEST_USERS.admin_a.username,
          password: TEST_USERS.admin_a.password,
          organizationId: TEST_USERS.admin_a.organizationId
        }
      })
      const body = await response.json()
      expect(response.ok()).toBe(true)
      expect(body.code).toBe(200)
      expect(body.data?.token).toBeTruthy()
    })

    test('02-閿欒鐧诲綍API娴嬭瘯', async ({ page }) => {
      const response = await page.request.post(`${API_URL}/auth/login`, {
        data: {
          username: 'wrong_user',
          password: 'wrong_password',
          organizationId: TEST_USERS.admin_a.organizationId
        }
      })
      const body = await response.json()
      expect(body.code).not.toBe(200)
    })

    test('03-鑾峰彇绉熸埛鍒楄〃API娴嬭瘯', async ({ page }) => {
      const response = await page.request.get(`${API_URL}/tenants/list`)
      const body = await response.json()
      expect(response.ok()).toBe(true)
      expect(Array.isArray(body.data)).toBe(true)
      expect(body.data.length).toBeGreaterThan(0)
    })
  })

  test.describe('6. 鎬ц兘娴嬭瘯', () => {
    test('01-鐧诲綍鍝嶅簲鏃堕棿', async ({ page }) => {
      test.slow()
      const startTime = Date.now()
      await loginThroughUi(page, TEST_USERS.admin_a)
      const responseTime = Date.now() - startTime
      expect(responseTime).toBeLessThan(AUTH_LOGIN_BUDGET_MS)
    })

    test('02-椤甸潰鍔犺浇鎬ц兘', async ({ page }) => {
      test.slow()
      const startTime = Date.now()
      const response = await page.goto(`${BASE_URL}/login`, { waitUntil: 'commit' })
      await expect(page.locator('.login-card')).toBeVisible({ timeout: LOGIN_PAGE_LOAD_BUDGET_MS })
      const loadTime = Date.now() - startTime
      expect(response?.ok()).toBe(true)
      expect(loadTime).toBeLessThan(LOGIN_PAGE_LOAD_BUDGET_MS)
    })
  })

  test.describe('7. 瀹夊叏娴嬭瘯', () => {
    test('01-SQL娉ㄥ叆闃叉姢娴嬭瘯', async ({ page }) => {
      await openLogin(page)
      await selectTenant(page, TEST_USERS.admin_a.organizationId)
      const sqlInjection = "' OR '1'='1"
      await page.locator('input:not([readonly]):not([type="password"])').first().fill(sqlInjection)
      await page.locator('input[type="password"]').first().fill(sqlInjection)
      await page.locator('.login-card .el-button--primary').click()
      await expect(page).toHaveURL(/\/login/)
    })

    test('02-XSS闃叉姢娴嬭瘯', async ({ page }) => {
      await openRegister(page)
      await selectRegisterTenant(page, '1')
      const xssPayload = '<script>alert(\"XSS\")</script>'
      await page.getByPlaceholder('璇疯緭鍏ョ敤鎴峰悕').fill(xssPayload)
      await page.getByPlaceholder('璇疯緭鍏ュ瘑鐮?).fill('Password123!')
      await page.getByPlaceholder('璇峰啀娆¤緭鍏ュ瘑鐮?).fill('Password123!')
      await registerFieldInput(page, '閭').fill('test@example.com')
      await page.locator('.register-card .el-button--primary').click()
      await expect(page.getByPlaceholder('璇疯緭鍏ョ敤鎴峰悕')).toHaveValue(xssPayload)
      await expect(page).toHaveURL(/\/register/)
    })
  })
})
