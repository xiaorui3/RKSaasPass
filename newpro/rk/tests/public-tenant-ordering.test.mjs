import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('public tenant entry pages should normalize tenant directory before rendering choices', async () => {
  const [loginSource, registerSource, joinSource, loginModalSource] = await Promise.all([
    readSource('../src/views/Login.vue'),
    readSource('../src/views/Register.vue'),
    readSource('../src/views/Join.vue'),
    readSource('../src/components/LoginModal.vue')
  ])

  for (const source of [loginSource, registerSource, joinSource, loginModalSource]) {
    assert.equal(source.includes("normalizeTenantDirectory"), true)
    assert.match(source, /tenantList\.value\s*=\s*normalizeTenantDirectory\((res\.data|res\.data \|\| \[\])\)/)
  }
})
