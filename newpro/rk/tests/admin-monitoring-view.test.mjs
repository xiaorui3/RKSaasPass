import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('monitoring page source should render healthy instances and instance detail expansion', async () => {
  const source = await readSource('../src/views/admin/operation/Monitoring.vue')

  assert.equal(source.includes('healthyInstances'), true)
  assert.equal(source.includes('instanceDetails'), true)
  assert.equal(source.includes('type="expand"'), true)
})
