import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')

const readSource = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8')

test('logged-in users leaving guest routes are sent to admin console when their role can enter admin', () => {
  const source = readSource('src/router/index.js')

  assert.match(source, /const\s+resolveAuthenticatedLanding\s*=/)
  assert.match(source, /to\.meta\.guest[\s\S]*resolveAuthenticatedLanding/)
  assert.doesNotMatch(source, /to\.meta\.guest[\s\S]*next\(\{\s*name:\s*['"]Home['"]\s*\}\)/)
})

test('user store exposes the admin-console capability used by login and router guards', () => {
  const source = readSource('src/stores/user.js')

  assert.match(source, /function\s+canEnterAdminConsole\s*\(/)
  assert.match(source, /return\s*\{[\s\S]*canEnterAdminConsole[\s\S]*\}/)
})

test('applyAuthSession writes the login session into localStorage before bootstrap requests run', () => {
  const source = readSource('src/stores/user.js')

  assert.match(source, /function\s+applyAuthSession\s*\(/)
  assert.match(source, /function\s+applyAuthSession[\s\S]*localStorage\.setItem\('token'/)
  assert.match(source, /function\s+applyAuthSession[\s\S]*localStorage\.setItem\('tenantId'/)
  assert.match(source, /function\s+applyAuthSession[\s\S]*localStorage\.setItem\('userInfo'/)
})

test('login bootstrap probes admin menus silently for front-office users without backend access', () => {
  const source = readSource('src/stores/user.js')

  assert.match(source, /function\s+fetchAdminMenus\s*\(/)
  assert.match(source, /getMyMenuTreeApi\(\{\s*silentError:\s*true\s*\}\)/)
  assert.doesNotMatch(source, /ElMessage\.error\([^)]*权限不足/)
})

test('admin menu fetch failures preserve previous menu snapshot and expose error state', () => {
  const source = readSource('src/stores/user.js')
  const start = source.indexOf('async function fetchAdminMenus()')
  const end = source.indexOf('async function login(', start)
  const fetchAdminMenusBlock = source.slice(start, end)

  assert.match(source, /adminMenusError\s*=\s*ref\(/)
  assert.match(source, /adminMenusLastLoadedAt\s*=\s*ref\(/)
  assert.match(
    fetchAdminMenusBlock,
    /catch\s*\(error\)\s*\{[\s\S]*adminMenusError\.value\s*=/
  )
  assert.doesNotMatch(
    fetchAdminMenusBlock,
    /catch\s*\(error\)\s*\{[\s\S]*adminMenus\.value\s*=\s*\[\]/
  )
  assert.match(fetchAdminMenusBlock, /return\s+adminMenus\.value/)
})

test('login page reuses the same authenticated landing decision on mount', () => {
  const source = readSource('src/views/Login.vue')

  assert.match(source, /const\s+resolveAuthenticatedLanding\s*=/)
  assert.match(source, /if\s*\(\s*userStore\.isLoggedIn\s*\)\s*\{[\s\S]*router\.replace\(resolveAuthenticatedLanding\(\)\)/)
  assert.doesNotMatch(source, /if\s*\(\s*userStore\.isLoggedIn\s*\)\s*\{[\s\S]*router\.push\(['"]\/['"]\)/)
})

test('login forms keep username and password rows compact without left label gaps', () => {
  const loginPage = readSource('src/views/Login.vue')
  const loginModal = readSource('src/components/LoginModal.vue')

  assert.match(loginPage, /<el-form[^>]*label-position="top"[^>]*class="auth-form"/)
  assert.match(loginPage, /\.auth-form\s+:deep\(\.el-form-item__label\)\s*\{[\s\S]*margin-bottom:\s*6px/)
  assert.doesNotMatch(loginPage, /label-width=["']\d+px["']/)

  assert.match(loginModal, /<el-form[^>]*label-position="top"[^>]*class="modal-auth-form"/)
  assert.match(loginModal, /\.modal-auth-form\s+:deep\(\.el-form-item__label\)\s*\{[\s\S]*margin-bottom:\s*6px/)
  assert.doesNotMatch(loginModal, /label-width=["']\d+px["']/)
})

test('main page user menu is explicitly hover-triggered and keeps profile admin logout entries', () => {
  const source = readSource('src/layouts/MainLayout.vue')

  assert.match(source, /<el-dropdown[^>]*class="user-dropdown"[^>]*trigger="hover"[^>]*@command="handleUserCommand"/)
  assert.match(source, /command="profile"[\s\S]*t\('nav\.profile'\)/)
  assert.match(source, /v-if="canEnterAdmin"[\s\S]*command="admin"[\s\S]*t\('nav\.admin'\)/)
  assert.match(source, /command="logout"[\s\S]*t\('nav\.logout'\)/)
})
