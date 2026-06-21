// @ts-check
import { test, expect } from '@playwright/test'

const BASE_URL = process.env.PLAYWRIGHT_BASE_URL || 'http://localhost:53681'

const overviewPayload = {
  code: 200,
  data: {
    defaultTenantId: 1,
    selectedTenantId: 1,
    totalVisits: 17,
    totalLogins: 9,
    totalOperations: 8,
    countryCount: 3,
    abnormalIpCount: 1,
    tenantOptions: [
      { tenantId: 1, tenantName: 'admin_a 租户', status: 1 },
      { tenantId: 2, tenantName: '演示租户', status: 1 }
    ],
    countryRankings: [
      { tenantId: 1, tenantName: 'admin_a 租户', country: '美国', province: 'California', visitCount: 9, loginCount: 4, operationCount: 5, lat: 37.0902, lng: -95.7129 },
      { tenantId: 1, tenantName: 'admin_a 租户', country: '中国', province: '广东', visitCount: 6, loginCount: 4, operationCount: 2, lat: 35.8617, lng: 104.1954 },
      { tenantId: 1, tenantName: 'admin_a 租户', country: '内网', province: '内网', visitCount: 2, loginCount: 1, operationCount: 1, lat: 35.8617, lng: 104.1954 }
    ],
    provinceRankings: [
      { tenantId: 1, tenantName: 'admin_a 租户', country: '美国', province: 'California', visitCount: 9, loginCount: 4, operationCount: 5, lat: 37.0902, lng: -95.7129 }
    ],
    recentIps: [
      { tenantId: 1, tenantName: 'admin_a 租户', country: '美国', province: 'California', ip: '203.0.113.8', userName: 'admin_a', visitCount: 9, loginCount: 4, operationCount: 5, lastSeenAt: '2026-05-19 10:00:00', samplePath: '/admin/operation/logs', lat: 37.0902, lng: -95.7129 },
      { tenantId: 1, tenantName: 'admin_a 租户', country: '中国', province: '广东', ip: '198.51.100.8', userName: 'admin_a', visitCount: 6, loginCount: 4, operationCount: 2, lastSeenAt: '2026-05-19 10:05:00', samplePath: '/admin/dashboard', lat: 35.8617, lng: 104.1954 },
      { tenantId: 1, tenantName: 'admin_a 租户', country: '内网', province: '内网', ip: '10.0.0.5', userName: 'admin_a', visitCount: 2, loginCount: 1, operationCount: 1, lastSeenAt: '2026-05-19 10:07:00', samplePath: '/admin/operation/traffic-center', lat: 35.8617, lng: 104.1954 }
    ],
    onlineUsers: [
      { tenantId: 1, tenantName: 'admin_a 租户', userName: 'admin_a', deviceType: 'pc', clientType: 'Chrome / Windows', ip: '198.51.100.8', country: '中国', province: '广东', activityCount: 3, lastSeenAt: '2026-05-19 10:08:00', samplePath: '/admin/operation/traffic-center', lat: 35.8617, lng: 104.1954 }
    ],
    onlineUserCount: 1,
    onlinePcCount: 1,
    onlineMobileCount: 0,
    currentOrigin: { country: '中国', province: '四川', city: '成都', ip: '198.51.100.14', lat: 30.572816, lng: 104.066801 }
  }
}

async function mockTrafficApis(page) {
  await page.addInitScript(() => {
    localStorage.setItem('token', 'traffic-test-token')
    localStorage.setItem('tenantId', '1')
    localStorage.setItem('userInfo', JSON.stringify({
      userId: 1,
      username: 'admin_a',
      organizationId: 1,
      roles: [{ roleId: 1, id: 1, roleName: '超级管理员' }]
    }))
    localStorage.setItem('adminMenus', '[]')
  })
  await page.route('**/admin/ops/traffic/default-tenant', (route) => route.fulfill({ json: overviewPayload }))
  await page.route('**/admin/ops/traffic/current-origin', (route) => route.fulfill({ json: { code: 200, data: overviewPayload.data.currentOrigin } }))
  await page.route('**/admin/ops/traffic/overview**', (route) => route.fulfill({ json: overviewPayload }))
  await page.route('**/admin/ops/traffic/drilldown**', (route) => route.fulfill({ json: overviewPayload }))
  await page.route('**/admin/ops/traffic/online-users**', (route) => route.fulfill({ json: { code: 200, data: overviewPayload.data.onlineUsers } }))
}

async function expectNonBlankGlobeCanvas(page) {
  const canvas = page.locator('.globe-container canvas').first()
  await expect(canvas).toBeVisible({ timeout: 20000 })
  await page.waitForFunction(() => {
    const canvas = document.querySelector('.globe-container canvas')
    if (!canvas) return false
    const rect = canvas.getBoundingClientRect()
    if (rect.width < 240 || rect.height < 320) return false
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl') || canvas.getContext('experimental-webgl')
    if (!gl) return canvas.toDataURL('image/png').length > 2000
    const width = gl.drawingBufferWidth
    const height = gl.drawingBufferHeight
    if (width < 240 || height < 320) return false
    const pixels = new Uint8Array(8 * 8 * 4)
    gl.readPixels(Math.max(0, Math.floor(width / 2) - 4), Math.max(0, Math.floor(height / 2) - 4), 8, 8, gl.RGBA, gl.UNSIGNED_BYTE, pixels)
    for (let index = 0; index < pixels.length; index += 4) {
      if (pixels[index] || pixels[index + 1] || pixels[index + 2]) {
        return true
      }
    }
    return false
  }, null, { timeout: 20000 })
}

test('traffic center renders an interactive nonblank globe on desktop', async ({ page }) => {
  await mockTrafficApis(page)
  await page.setViewportSize({ width: 1440, height: 980 })
  await page.goto(`${BASE_URL}/admin/operation/traffic-center`, { waitUntil: 'domcontentloaded' })

  await expect(page.locator('.traffic-center-page')).toBeVisible()
  await expect(page.locator('.globe-wrap.webgl-ready')).toBeVisible({ timeout: 20000 })
  await expectNonBlankGlobeCanvas(page)
  await page.getByRole('button', { name: /美国/ }).click()
  await expect(page.getByText('California').first()).toBeVisible()
})

test('traffic center keeps the globe stage visible on mobile', async ({ page }) => {
  await mockTrafficApis(page)
  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(`${BASE_URL}/admin/operation/traffic-center`, { waitUntil: 'domcontentloaded' })

  await expect(page.locator('.traffic-stage')).toBeVisible({ timeout: 20000 })
  await expect(page.locator('.fallback-earth')).toBeVisible()
  await expectNonBlankGlobeCanvas(page)
})
