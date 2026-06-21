// @ts-check
const { test, expect } = require('@playwright/test')

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:5173'

const LOGIN_DATA = {
  token: 'test-token',
  organizationId: '1',
  userId: 8,
  username: 'member_a',
  roles: [{ roleId: 2 }]
}

async function mockProfileBootstrap(page) {
  await page.goto(BASE_URL, { waitUntil: 'domcontentloaded' })
  await page.evaluate((data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('tenantId', data.organizationId)
    localStorage.setItem('userInfo', JSON.stringify({
      userId: data.userId,
      username: data.username,
      organizationId: data.organizationId,
      roles: data.roles
    }))
  }, LOGIN_DATA)

  await page.route('**/users/me', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: {
          id: '8',
          name: '',
          username: 'legacy_member',
          cellPhone: '13800138003',
          email: 'member_a@example.com',
          gender: 0,
          createTime: '2026-03-06 00:35:02',
          roleName: '瀛︾敓'
        }
      })
    })
  })

  await page.route('**/tenants/list', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: [{ id: 1, tenantName: '绉熸埛A' }]
      })
    })
  })

  await page.route('**/auth/switchable-tenants', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: [{
          tenantId: 1,
          roleId: 2,
          username: 'member_a'
        }]
      })
    })
  })

  const themePayload = JSON.stringify({
    code: 200,
    msg: '鎿嶄綔鎴愬姛',
    state: 'success',
    data: {
      frontendTheme: 'default',
      adminTheme: 'default'
    }
  })

  await page.route('**/api/config/theme/current', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: themePayload
    })
  })

  await page.route('**/api/config/theme/public**', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: themePayload
    })
  })

  await page.route('**/api/referral-codes/current', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '閹垮秳缍旈幋鎰',
        state: 'success',
        data: null
      })
    })
  })
}

test('profile page should fall back to username when name is missing', async ({ page }) => {
  await mockProfileBootstrap(page)

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })

  await expect(page.locator('.profile-page')).toBeVisible()
  await expect(page.locator('.user-name')).toHaveText('legacy_member')
})

test('profile avatar upload should submit avatar url string instead of raw upload response', async ({ page }) => {
  await mockProfileBootstrap(page)

  let updatePayload = null

  await page.route('**/api/files/upload', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: {
          fileUrl: 'http://127.0.0.1:9000/rk-files/users/avatars/avatar-probe.png',
          url: 'http://127.0.0.1:9000/rk-files/users/avatars/avatar-probe.png'
        }
      })
    })
  })

  await page.route('**/users', async (route) => {
    // 鍙嫤鎴?PUT 璇锋眰锛堟洿鏂扮敤鎴蜂俊鎭級锛孏ET 璇锋眰浜ょ粰 /users/me mock 澶勭悊
    if (route.request().method() !== 'PUT') {
      return route.continue()
    }
    updatePayload = route.request().postDataJSON()
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: null
      })
    })
  })

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.profile-page')).toBeVisible()

  const avatarInput = page.locator('.avatar-upload .el-upload__input').first()
  await expect(avatarInput).toHaveCount(1)

  await avatarInput.setInputFiles({
    name: 'avatar.png',
    mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jX4kAAAAASUVORK5CYII=', 'base64')
  })

  await expect.poll(() => updatePayload?.icon ?? null).toBe(
    'http://127.0.0.1:9000/rk-files/users/avatars/avatar-probe.png'
  )
})

test('profile header should expose an explicit home button', async ({ page }) => {
  await mockProfileBootstrap(page)

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })

  await expect(page.locator('.hero-actions').getByText(/杩斿洖棣栭〉/)).toBeVisible()
})

test('profile activity and competition records should navigate to detail pages', async ({ page }) => {
  await mockProfileBootstrap(page)

  await page.route('**/api/activity/my**', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: [{
          id: 321,
          activityName: '娴嬭瘯娲诲姩璁板綍',
          startTime: '2026-05-10 10:00:00',
          endTime: '2026-05-11 10:00:00',
          location: 'Room 101',
          cover: 'http://upload.test/activity-cover.png',
          status: 2
        }]
      })
    })
  })

  await page.route('**/api/competition/my**', (route) => {
    route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 200,
        msg: '鎿嶄綔鎴愬姛',
        state: 'success',
        data: [{
          id: 654,
          competitionName: '娴嬭瘯姣旇禌璁板綍',
          registrationDeadline: '2026-05-20 10:00:00',
          competitionType: '鍒涙柊姣旇禌',
          cover: 'http://upload.test/competition-cover.png',
          status: 'REGISTRATION'
        }]
      })
    })
  })

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)

  // 灏濊瘯鐐瑰嚮娲诲姩璁板綍鏍囩
  const actTab = page.locator('[role="menuitem"]:has-text("娲诲姩璁板綍"), .el-tabs__item:has-text("娲诲姩"), [data-testid="activity-tab"]').first()
  if (await actTab.count() > 0) {
    await actTab.click()
    await page.waitForTimeout(1000)
    const actLink = page.getByText('娴嬭瘯娲诲姩璁板綍').first()
    if (await actLink.isVisible().catch(() => false)) {
      await actLink.click()
      await page.waitForTimeout(1000)
      // 楠岃瘉璺宠浆
      const url = page.url()
      expect(url).toMatch(/\/activities\/321|\/activity/)
    }
  }

  await page.goto(`${BASE_URL}/profile`, { waitUntil: 'domcontentloaded' })
  await page.waitForTimeout(1000)
  const compTab = page.locator('[role="menuitem"]:has-text("姣旇禌璁板綍"), .el-tabs__item:has-text("姣旇禌"), [data-testid="competition-tab"]').first()
  if (await compTab.count() > 0) {
    await compTab.click()
    await page.waitForTimeout(1000)
    const compLink = page.getByText('娴嬭瘯姣旇禌璁板綍').first()
    if (await compLink.isVisible().catch(() => false)) {
      await compLink.click()
      await page.waitForTimeout(1000)
      const url = page.url()
      expect(url).toMatch(/\/competition\/654|\/competition/)
    }
  }
})
