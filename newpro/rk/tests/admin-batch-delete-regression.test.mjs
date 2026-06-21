import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

const pagesWithSingleDelete = [
  '../src/views/admin/activity/ActivityList.vue',
  '../src/views/admin/activity/Competition.vue',
  '../src/views/admin/activity/CreditConfig.vue',
  '../src/views/admin/club/Alumni.vue',
  '../src/views/admin/club/ContactMessages.vue',
  '../src/views/admin/club/EmailCenter.vue',
  '../src/views/admin/club/HistoryManage.vue',
  '../src/views/admin/club/Members.vue',
  '../src/views/admin/club/ReferralCodes.vue',
  '../src/views/admin/content/Achievements.vue',
  '../src/views/admin/content/News.vue',
  '../src/views/admin/content/Notices.vue',
  '../src/views/admin/content/Works.vue',
  '../src/views/admin/operation/Backup.vue',
  '../src/views/admin/system/Config.vue',
  '../src/views/admin/system/Menus.vue',
  '../src/views/admin/system/Roles.vue',
  '../src/views/admin/system/Tenants.vue',
  '../src/views/admin/system/Users.vue'
]

const cardPagesWithSingleDelete = [
  '../src/views/admin/activity/PhotoGallery.vue'
]

test('admin delete-capable tables expose unified batch delete controls', async () => {
  for (const page of pagesWithSingleDelete) {
    const source = await readSource(page)

    assert.equal(source.includes('type="selection"'), true, `${page} should allow selecting rows`)
    assert.equal(source.includes('handleSelectionChange'), true, `${page} should track selected rows`)
    assert.equal(source.includes('handleBatchDelete'), true, `${page} should provide batch delete handler`)
    assert.equal(source.includes('selectedRows'), true, `${page} should disable batch delete when nothing is selected`)
    assert.equal(source.includes('批量删除'), true, `${page} should show a batch delete action`)
  }
})

test('admin delete-capable card pages expose batch delete controls', async () => {
  for (const page of cardPagesWithSingleDelete) {
    const source = await readSource(page)

    assert.equal(source.includes('handleBatchDelete'), true, `${page} should provide batch delete handler`)
    assert.equal(source.includes('selectedPhotos'), true, `${page} should track selected photos`)
    assert.equal(source.includes('批量删除'), true, `${page} should show a batch delete action`)
    assert.equal(source.includes('togglePhotoSelection'), true, `${page} should allow selecting cards`)
  }
})

test('shared batch delete composable keeps partial failures visible', async () => {
  const source = await readSource('../src/utils/batchDelete.js')

  assert.equal(source.includes('Promise.allSettled'), true)
  assert.equal(source.includes('failureCount'), true)
  assert.equal(source.includes('successCount'), true)
  assert.equal(source.includes('handleBatchDelete'), true)
})
