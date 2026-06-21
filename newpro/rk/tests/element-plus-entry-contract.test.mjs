import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const projectRoot = resolve(import.meta.dirname, '..')

test('frontend entry installs the Element Plus plugin so login/register forms render on live runtime', () => {
  const source = readFileSync(resolve(projectRoot, 'src/main.js'), 'utf8')

  assert.match(
    source,
    /import\s+ElementPlus(?:\s*,\s*\{[\s\S]*?\})?\s+from\s+['"]element-plus['"]/,
    'src/main.js should import the Element Plus plugin'
  )
  assert.match(
    source,
    /app\.use\(ElementPlus\)/,
    'src/main.js should install the Element Plus plugin before mounting'
  )
})
