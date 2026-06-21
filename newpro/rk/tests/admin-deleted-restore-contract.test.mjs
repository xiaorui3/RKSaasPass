import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('member admin page should expose deleted member restore flow', async () => {
  const api = await readSource('../src/api/member.js')
  const page = await readSource('../src/views/admin/club/Members.vue')

  assert.equal(api.includes('/api/members/deleted'), true)
  assert.equal(api.includes('/api/members/${id}/restore'), true)
  assert.equal(page.includes('getDeletedMembers'), true)
  assert.equal(page.includes('restoreMember'), true)
  assert.equal(page.includes('deletedDialogVisible'), true)
  assert.equal(page.includes('fetchDeletedMembers'), true)
  assert.equal(page.includes('handleRestoreMember'), true)
  assert.equal(page.includes('查看已删除'), true)
  assert.equal(page.includes('恢复'), true)
})

test('alumni admin page should expose deleted alumni restore flow', async () => {
  const api = await readSource('../src/api/alumni.js')
  const page = await readSource('../src/views/admin/club/Alumni.vue')

  assert.equal(api.includes('/api/alumni/deleted'), true)
  assert.equal(api.includes('/api/alumni/${id}/restore'), true)
  assert.equal(page.includes('getDeletedAlumni'), true)
  assert.equal(page.includes('restoreAlumni'), true)
  assert.equal(page.includes('deletedDialogVisible'), true)
  assert.equal(page.includes('fetchDeletedAlumni'), true)
  assert.equal(page.includes('handleRestoreAlumni'), true)
  assert.equal(page.includes('查看已删除'), true)
  assert.equal(page.includes('恢复'), true)
})

test('menu management should refresh active sidebar menu cache after mutations', async () => {
  const menusPage = await readSource('../src/views/admin/system/Menus.vue')
  const layout = await readSource('../src/layouts/AdminLayout.vue')

  assert.equal(menusPage.includes("import { useUserStore } from '@/stores/user'"), true)
  assert.equal(menusPage.includes('refreshAdminMenus'), true)
  assert.equal(menusPage.includes('userStore.fetchAdminMenus()'), true)
  assert.equal(layout.includes('userStore.fetchAdminMenus()'), true)
})
