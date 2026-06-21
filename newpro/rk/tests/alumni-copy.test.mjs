import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const alumniViewPath = path.resolve(currentDir, '../src/views/Alumni.vue')

test('Alumni view uses readable Chinese labels instead of mojibake', () => {
  const source = fs.readFileSync(alumniViewPath, 'utf8')

  assert.match(source, /校友风采/)
  assert.match(source, /档案总人数/)
  assert.match(source, /在册未毕业/)
  assert.match(source, /已毕业校友/)
  assert.match(source, /暂无校友信息/)
  assert.doesNotMatch(source, /[\uE000-\uF8FF]/)
  assert.doesNotMatch(source, /\?{4,}/)
})
