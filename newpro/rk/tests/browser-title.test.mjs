import test from 'node:test'
import assert from 'node:assert/strict'
import { resolveBrowserTabTitle } from '../src/utils/browserTitle.js'

test('shows portal title when user is not logged in on public pages', () => {
  assert.equal(resolveBrowserTabTitle({
    isLoggedIn: false,
    tenantId: '',
    tenantDirectory: [],
    routeTitle: '首页'
  }), '首页 - RK-Web')
})

test('shows current tenant name when tenant metadata exists', () => {
  assert.equal(resolveBrowserTabTitle({
    isLoggedIn: true,
    tenantId: '2',
    tenantDirectory: [
      { id: 1, tenantName: '摄影社' },
      { id: 2, tenantName: '软件项目开发社团' }
    ]
  }), '软件项目开发社团')
})

test('prefixes route title with current tenant when logged in', () => {
  assert.equal(resolveBrowserTabTitle({
    isLoggedIn: true,
    tenantId: '2',
    tenantDirectory: [
      { id: 2, tenantName: '软件项目开发社团' }
    ],
    routeTitle: '社团历程'
  }), '社团历程 - 软件项目开发社团')
})

test('falls back to tenant id when directory has not loaded yet', () => {
  assert.equal(resolveBrowserTabTitle({
    isLoggedIn: true,
    tenantId: '9',
    tenantDirectory: []
  }), '租户9')
})
