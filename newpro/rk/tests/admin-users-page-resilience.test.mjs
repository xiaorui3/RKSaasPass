import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resolve, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))

async function readSource(relativePath) {
  return readFile(resolve(__dirname, relativePath), 'utf8')
}

test('admin users page should tolerate partial statistics failures without rejecting the whole load', async () => {
  const source = await readSource('../src/views/admin/system/Users.vue')

  assert.match(source, /Promise\.allSettled\(\[/)
  assert.match(source, /const pageRes = res\.status === 'fulfilled' \? res\.value : null/)
  assert.match(source, /const memberRes = memberStatsRes\.status === 'fulfilled' \? memberStatsRes\.value : null/)
  assert.match(source, /const statsRes = userStatsRes\.status === 'fulfilled' \? userStatsRes\.value : null/)
})

test('admin users page should show explicit people-domain definitions for accounts, members, alumni, and scope', async () => {
  const source = await readSource('../src/views/admin/system/Users.vue')

  assert.match(source, /data-testid="people-domain-definitions"/)
  assert.match(source, /:data-definition-key="definition\.key"/)
  assert.match(source, /key: 'accounts-ledger'/)
  assert.match(source, /key: 'members-ledger'/)
  assert.match(source, /key: 'alumni-ledger'/)
  assert.match(source, /key: 'scope-rule'/)
  assert.match(source, /const peopleDomainDefinitions = computed\(\(\) => \(\[/)
})
