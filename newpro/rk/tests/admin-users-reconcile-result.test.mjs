import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('admin users page keeps visible reconcile result and refreshes scoped statistics', async () => {
  const source = await readFile(new URL('../src/views/admin/system/Users.vue', import.meta.url), 'utf8')

  assert.match(source, /lastReconcileResult/)
  assert.match(source, /data-testid="people-domain-reconcile-result"/)
  assert.match(source, /formatReconcileSummary/)
  assert.match(source, /createdMemberCount/)
  assert.match(source, /await fetchScopedStatistics\(\)/)
})
