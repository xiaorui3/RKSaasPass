import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

const SRC_ROOT = new URL('../src/', import.meta.url)
const ALLOWED_FORM_DATA_FILES = new Set([
  'api/alumni.js',
  'api/common.js',
])
const ALLOWED_MULTIPART_FILES = new Set([
  'api/alumni.js',
  'api/common.js',
  'api/news.js',
])

function load(relativePath) {
  return readFileSync(new URL(relativePath, SRC_ROOT), 'utf8')
}

test('frontend source should keep new FormData usage restricted to unified upload and alumni import entrypoints', () => {
  const files = [
    'api/alumni.js',
    'api/common.js',
    'api/news.js',
    'api/user.js',
    'views/Profile.vue',
    'views/admin/activity/ActivityList.vue',
    'views/admin/activity/Competition.vue',
    'views/admin/activity/PhotoGallery.vue',
    'views/admin/club/EmailCenter.vue',
    'views/admin/club/Finance.vue',
    'views/admin/content/News.vue',
    'views/admin/content/Notices.vue',
    'views/admin/content/Works.vue',
    'views/admin/system/Tenants.vue',
    'views/admin/system/ThemeConfig.vue',
  ]

  for (const relativePath of files) {
    const source = load(relativePath)
    if (source.includes('new FormData(')) {
      assert.ok(
        ALLOWED_FORM_DATA_FILES.has(relativePath),
        `${relativePath} should not create FormData directly`,
      )
    }
  }
})

test('frontend source should keep multipart headers restricted to the approved import and unified upload api files', () => {
  const files = [
    'api/alumni.js',
    'api/common.js',
    'api/news.js',
    'api/user.js',
    'views/Profile.vue',
    'views/admin/activity/ActivityList.vue',
    'views/admin/activity/Competition.vue',
    'views/admin/activity/PhotoGallery.vue',
    'views/admin/club/EmailCenter.vue',
    'views/admin/club/Finance.vue',
    'views/admin/content/News.vue',
    'views/admin/content/Notices.vue',
    'views/admin/content/Works.vue',
    'views/admin/system/Tenants.vue',
    'views/admin/system/ThemeConfig.vue',
  ]

  for (const relativePath of files) {
    const source = load(relativePath)
    if (source.includes('multipart/form-data')) {
      assert.ok(
        ALLOWED_MULTIPART_FILES.has(relativePath),
        `${relativePath} should not set multipart/form-data headers directly`,
      )
    }
  }
})
