import test from 'node:test'
import assert from 'node:assert/strict'

import {
  canAccessAdminRoute,
  canEnterAdmin,
  flattenAdminMenuPaths
} from '../src/utils/adminAccess.js'

const clubManagerMenus = [
  {
    path: '/admin/club',
    subMenus: [
      { path: '/admin/club/members' },
      { path: '/admin/activity/list' }
    ]
  }
]

test('allows admin home when a tenant custom role has backend menus', () => {
  const menuPaths = flattenAdminMenuPaths(clubManagerMenus)

  assert.equal(canEnterAdmin(153, clubManagerMenus), true)
  assert.equal(canAccessAdminRoute('/admin', 153, menuPaths), true)
})

test('allows configured child admin route by backend menu path', () => {
  const menuPaths = flattenAdminMenuPaths(clubManagerMenus)

  assert.equal(canAccessAdminRoute('/admin/club/members', 153, menuPaths), true)
  assert.equal(canAccessAdminRoute('/admin/system/users', 153, menuPaths), false)
})

test('does not block a tenant custom role when route has fixed legacy roles', () => {
  const menuPaths = flattenAdminMenuPaths(clubManagerMenus)

  assert.equal(canAccessAdminRoute('/admin/club/members', 153, menuPaths, [1, 3, 5, 7, 8]), true)
})
