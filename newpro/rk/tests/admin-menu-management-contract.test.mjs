import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const __dirname = path.dirname(fileURLToPath(import.meta.url))

async function readSource(relativePath) {
  return readFile(path.resolve(__dirname, relativePath), 'utf8')
}

test('menu management search should expand matched child menu rows', async () => {
  const source = await readSource('../src/views/admin/system/Menus.vue')

  assert.match(source, /:expand-row-keys="expandedRowKeys"/)
  assert.match(source, /const\s+expandedRowKeys\s*=\s*computed/)
  assert.match(source, /collectExpandableIds\(filteredMenuList\.value\)/)
})
