import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const profileSource = readFileSync(new URL('../src/views/Profile.vue', import.meta.url), 'utf8')
const routerSource = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8')

test('renders profile overview cards before the main content panels', () => {
  const overviewIndex = profileSource.indexOf('class="profile-overview-grid"')
  const firstPanelIndex = profileSource.indexOf('<user-hub-panel')

  assert.notEqual(overviewIndex, -1, 'expected a top overview grid for profile quick access')
  assert.notEqual(firstPanelIndex, -1, 'expected profile content panels to exist')
  assert.ok(overviewIndex < firstPanelIndex, 'overview grid should appear before the first content panel')
})

test('keeps module navigation in the main flow instead of the aside slot', () => {
  assert.equal(profileSource.includes('<template #aside>'), false, 'aside slot should not own profile navigation')
  assert.match(profileSource, /class="profile-nav-panel(?: [^"]+)?"/, 'expected a dedicated navigation panel in the main flow')
})

test('keeps quick navigation inside the top overview region so it stays visible earlier', () => {
  assert.match(
    profileSource,
    /\.profile-overview-grid\s*\{[\s\S]*grid-template-columns:\s*repeat\(3,\s*minmax\(0,\s*1fr\)\)/,
    'overview grid should expose three top-level cards so navigation stays earlier in the viewport'
  )
  assert.equal(profileSource.includes('class="profile-nav-panel profile-overview-card"'), true)
})

test('uses Chinese-first copy for the current profile shell instead of stale English labels', () => {
  assert.equal(profileSource.includes('Account Overview'), false, 'profile overview header should be localized')
  assert.equal(profileSource.includes('Quick Navigation'), false, 'profile navigation header should be localized')
  assert.equal(profileSource.includes('Tenant Memberships'), false, 'tenant membership header should be localized')
  assert.equal(profileSource.includes('当前模块'), true, 'profile shell should render the current Chinese-first module copy')
})

test('profile route should render inside the public MainLayout like news pages', () => {
  const layoutIndex = routerSource.indexOf("name: 'Layout'")
  const profileIndex = routerSource.indexOf("name: 'Profile'")
  const adminIndex = routerSource.indexOf('const adminRoutes')

  assert.notEqual(layoutIndex, -1, 'MainLayout route should exist')
  assert.notEqual(profileIndex, -1, 'Profile route should exist')
  assert.ok(profileIndex > layoutIndex, 'Profile route should be nested under the public layout')
  assert.ok(adminIndex === -1 || profileIndex < adminIndex, 'Profile route should remain in the frontend route group')
  assert.match(routerSource, /path:\s*['"]profile['"][\s\S]*name:\s*['"]Profile['"][\s\S]*@\/views\/Profile\.vue/)
  assert.doesNotMatch(routerSource.slice(0, layoutIndex), /path:\s*['"]\/profile['"][\s\S]*name:\s*['"]Profile['"]/)
})
