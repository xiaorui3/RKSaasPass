import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('traffic center page should initialize a globe.gl scene with local earth assets', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')

  assert.equal(source.includes("import Globe from 'globe.gl'"), true)
  assert.equal(source.includes('globeContainer'), true)
  assert.equal(source.includes('/assets/traffic-globe/earth-blue-marble.jpg'), true)
  assert.equal(source.includes('/assets/traffic-globe/night-sky.png'), true)
  assert.equal(source.includes('initGlobe'), true)
  assert.equal(source.includes('refreshGlobeData'), true)
})

test('traffic center page should expose country and province drilldown flows', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')

  assert.equal(source.includes('selectedCountry'), true)
  assert.equal(source.includes('selectedProvince'), true)
  assert.equal(source.includes('handleCountryClick'), true)
  assert.equal(source.includes('handleProvinceClick'), true)
  assert.equal(source.includes('getTrafficDrilldown'), true)
  assert.equal(source.includes('countryRankings'), true)
  assert.equal(source.includes('provinceRankings'), true)
  assert.equal(source.includes('recentIps'), true)
})

test('traffic center page should include controls for tenant default selection and fallback earth', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')

  assert.equal(source.includes('saveTrafficDefaultTenant'), true)
  assert.equal(source.includes('defaultTenantId'), true)
  assert.equal(source.includes('fallback-earth'), true)
  assert.equal(source.includes('webglReady'), true)
  assert.equal(source.includes('traffic-stage'), true)
  assert.equal(source.includes('tenantOptions'), true)
})

test('traffic center page should expose online users and desktop mobile breakdown', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')

  assert.equal(source.includes('onlineUsers'), true)
  assert.equal(source.includes('onlineUserCount'), true)
  assert.equal(source.includes('onlinePcCount'), true)
  assert.equal(source.includes('onlineMobileCount'), true)
  assert.equal(source.includes('getTrafficOnlineUsers'), true)
  assert.equal(source.includes('deviceType'), true)
  assert.equal(source.includes('clientType'), true)
})

test('traffic center globe should draw animated route arcs from current origin instead of tall cylinders', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')
  const api = await readSource('../src/api/admin-ops.js')

  assert.equal(source.includes('currentOrigin'), true)
  assert.equal(source.includes('currentOriginLabel'), true)
  assert.equal(source.includes('buildTrafficArcs'), true)
  assert.equal(source.includes('buildTrafficRings'), true)
  assert.equal(source.includes('.arcsData(buildTrafficArcs())'), true)
  assert.equal(source.includes(".arcStartLat('startLat')"), true)
  assert.equal(source.includes(".arcEndLat('endLat')"), true)
  assert.equal(source.includes('.arcDashAnimateTime'), true)
  assert.equal(source.includes('.ringsData(buildTrafficRings())'), true)
  assert.equal(source.includes(".pointAltitude('altitude')"), false)
  assert.equal(source.includes('getTrafficCurrentOrigin'), true)
  assert.equal(api.includes('getTrafficCurrentOrigin'), true)
  assert.equal(api.includes('/admin/ops/traffic/current-origin'), true)
})

test('traffic center globe should ignore missing origin coordinates during WebGL initialization', async () => {
  const source = await readSource('../src/views/admin/operation/TrafficCenter.vue')

  assert.equal(source.includes('function hasValidCoordinate'), true)
  assert.equal(source.includes('if (hasValidCoordinate(origin))'), true)
  assert.equal(source.includes('origin?.lat !== null && origin?.lng !== null'), false)
  assert.equal(source.includes('.filter((item) => hasValidCoordinate(item))'), true)
})

test('traffic backend should enrich public IPs through a free geolocation service before marking unknown', async () => {
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(service.includes('ipwho.is'), true)
  assert.equal(service.includes('resolvePublicIpLocation'), true)
  assert.equal(service.includes('trafficGeoCache'), true)
  assert.equal(service.includes('readIpWhoLocation'), true)
})

test('traffic backend should expose current origin and map provinces to distinct coordinates', async () => {
  const controller = await readSource('../../../rk-user/src/main/java/com/tianji/user/controller/AdminOpsController.java')
  const serviceApi = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/IAdminOpsService.java')
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')
  const overview = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/vo/adminops/AdminTrafficOverviewVO.java')

  assert.equal(controller.includes('/traffic/current-origin'), true)
  assert.equal(serviceApi.includes('getTrafficCurrentOrigin()'), true)
  assert.equal(overview.includes('private AdminTrafficLocationVO currentOrigin'), true)
  assert.equal(service.includes('buildCurrentTrafficOrigin'), true)
  assert.equal(service.includes('readCurrentTrafficClientIp'), true)
  assert.equal(service.includes('resolveTrafficCoordinates'), true)
  assert.equal(service.includes('黑龙江'), true)
  assert.equal(service.includes('45.742'), true)
  assert.equal(service.includes('126.642'), true)
})

test('traffic backend current origin should fall back when public IP lookup is unknown', async () => {
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(service.includes('readRecentTrafficRawLocation'), true)
  assert.equal(service.includes('isUnknownTrafficLocation'), true)
  assert.equal(service.includes('resolveTrafficLocation(clientIp, recentRawLocation)'), true)
  assert.equal(service.includes('isUnknownTrafficLocation(location)'), true)
  assert.equal(service.includes('isUnknownTrafficLocation(egress)'), true)
})

test('traffic backend should resolve common live China IP segments without blocking on external lookup', async () => {
  const service = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  assert.equal(service.includes('resolveKnownTrafficIpSegment'), true)
  assert.equal(service.includes('119.4.'), true)
  assert.equal(service.includes('101.204.'), true)
  assert.equal(service.includes('111.42.'), true)
  assert.equal(service.includes('123.167.'), true)
  assert.equal(service.includes('1.188.'), true)
})
