import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const emojiPattern = /[\p{Extended_Pictographic}\u2600-\u27BF]/u

const uiFiles = [
  '../src/layouts/MainLayout.vue',
  '../src/components/KeyboardShortcuts.vue',
  '../src/views/Home.vue',
  '../src/views/Join.vue',
  '../src/views/Achievements.vue',
  '../src/views/admin/system/ThemeConfig.vue',
  '../src/views/admin/operation/ServiceMonitor.vue',
  '../src/views/admin/operation/DataExport.vue',
  '../src/views/admin/club/ResourceBooking.vue'
]

for (const file of uiFiles) {
  test(`ui file should not contain emoji glyphs: ${file}`, async () => {
    const source = await readFile(new URL(file, import.meta.url), 'utf8')
    assert.equal(
      emojiPattern.test(source),
      false,
      `Expected ${file} to avoid emoji glyphs in current-branch UI source`
    )
  })
}
