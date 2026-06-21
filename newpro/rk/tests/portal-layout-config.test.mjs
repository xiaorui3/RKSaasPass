import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = resolve(__dirname, '..')

const readSource = (relativePath) => readFileSync(resolve(root, relativePath), 'utf8')

test('portal home layout config utility defines tenant-scoped sections and quick-entry modules', () => {
  const source = readSource('src/utils/portalLayoutConfig.js')

  assert.match(source, /export const PORTAL_HOME_LAYOUT_CONFIG_KEY = 'portal\.home\.layout\.config'/)
  assert.match(source, /export const DEFAULT_HOME_SECTIONS = \[/)
  assert.match(source, /key: 'quickEntry'[\s\S]*title: '快捷入口'/)
  assert.match(source, /key: 'activities'[\s\S]*title: '近期活动'/)
  assert.match(source, /export const DEFAULT_HOME_ENTRY_MODULES = \[/)
  assert.match(source, /key: 'competition'[\s\S]*title: '学科竞赛'/)
  assert.match(source, /export function normalizePortalHomeLayoutConfig/)
  assert.match(source, /export function orderedVisibleItems/)
})

test('theme config page persists homepage section and entry-module ordering through system config', () => {
  const source = readSource('src/views/admin/system/ThemeConfig.vue')

  assert.match(source, /前台首页模块编排/)
  assert.match(source, /首页区域/)
  assert.match(source, /快捷入口卡片/)
  assert.match(source, /PORTAL_HOME_LAYOUT_CONFIG_KEY/)
  assert.match(source, /normalizePortalHomeLayoutConfig/)
  assert.match(source, /savePortalLayoutConfig/)
  assert.match(source, /getSystemConfigList/)
  assert.match(source, /createSystemConfig/)
  assert.match(source, /updateSystemConfig/)
})

test('home page renders configurable homepage sections instead of fixed section order', () => {
  const source = readSource('src/views/Home.vue')

  assert.match(source, /getSystemConfig/)
  assert.match(source, /normalizePortalHomeLayoutConfig/)
  assert.match(source, /orderedVisibleItems/)
  assert.match(source, /v-for="section in orderedHomeSections"/)
  assert.match(source, /section\.key === 'quickEntry'/)
  assert.match(source, /section\.key === 'activities'/)
  assert.match(source, /section\.title/)
  assert.match(source, /orderedEntryModules/)
})
