import test from 'node:test'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const testDir = dirname(fileURLToPath(import.meta.url))
const repoRoot = resolve(testDir, '../../..')

async function readFrontendSource(relativePath) {
  return readFile(resolve(repoRoot, 'newpro/rk', relativePath), 'utf8')
}

function trackedFiles() {
  return new Set(
    execFileSync('git', ['ls-files', 'newpro/rk/src/api/*.js'], {
      cwd: repoRoot,
      encoding: 'utf8'
    })
      .split(/\r?\n/)
      .filter(Boolean)
  )
}

test('admin wiki page should only import API modules that are tracked in git', async () => {
  const source = await readFrontendSource('src/views/admin/content/Wiki.vue')
  const imports = [...source.matchAll(/from ['"]@\/api\/([^'"]+)['"]/g)].map((match) => match[1])
  const files = trackedFiles()

  for (const moduleName of imports) {
    assert.equal(
      files.has(`newpro/rk/src/api/${moduleName}.js`),
      true,
      `@/api/${moduleName} must be committed so Jenkins can build from Gitee`
    )
  }
})
