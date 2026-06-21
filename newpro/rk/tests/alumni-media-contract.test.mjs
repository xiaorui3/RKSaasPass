import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('alumni page should resolve avatar paths through managed media resolver', async () => {
  const source = await readFile(new URL('../src/views/Alumni.vue', import.meta.url), 'utf8')

  assert.match(
    source,
    /<el-avatar\s+:size="80"\s+:src="resolveMediaUrl\(alumni\.avatar\)"/,
    'Alumni avatar should use resolveMediaUrl(alumni.avatar) so managed relative paths and rewritten MinIO URLs render correctly'
  )
})
