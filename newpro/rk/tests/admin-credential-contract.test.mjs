import test from 'node:test'
import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'

const TEST_ROOT = new URL('./', import.meta.url)
const ALLOWED_SUFFIXES = new Set(['.cjs', '.mjs'])
const IGNORED_SUFFIXES = ['.bak']

async function collectFiles(dirUrl) {
  const entries = await readdir(dirUrl, { withFileTypes: true })
  const files = []
  for (const entry of entries) {
    const entryUrl = new URL(`${entry.name}${entry.isDirectory() ? '/' : ''}`, dirUrl)
    if (entry.isDirectory()) {
      files.push(...await collectFiles(entryUrl))
      continue
    }
    const extension = path.extname(entry.name)
    if (!ALLOWED_SUFFIXES.has(extension)) {
      continue
    }
    if (IGNORED_SUFFIXES.some((suffix) => entry.name.endsWith(suffix))) {
      continue
    }
    files.push(entryUrl)
  }
  return files
}

test('playwright fixtures should not keep retired admin_a default password', async () => {
  const files = await collectFiles(TEST_ROOT)
  const offenders = []
  const stalePatterns = [
    /username\s*[:=]\s*['"]admin_a['"]\s*,\s*password\s*[:=]\s*['"]123456['"]/,
    /admin_a\s*:\s*\{\s*username\s*:\s*['"]admin_a['"]\s*,\s*password\s*:\s*['"]123456['"]/,
    /ADMIN_USERNAME\s*=\s*['"]admin_a['"]\s*;\s*[\r\n]+\s*const\s+ADMIN_PASSWORD\s*=\s*['"]123456['"]/,
    /async function login\s*\([^)]*username\s*=\s*['"]admin_a['"]\s*,\s*password\s*=\s*['"]123456['"]/
  ]

  for (const fileUrl of files) {
    const source = await readFile(fileUrl, 'utf8')
    if (stalePatterns.some((pattern) => pattern.test(source))) {
      offenders.push(path.basename(fileUrl.pathname))
    }
  }

  assert.deepEqual(offenders, [])
})

test('high-traffic admin smoke suites should use shared super-admin credentials helper', async () => {
  const files = [
    'admin-credit-volunteer-routes.spec.cjs',
    'admin-people-pages-real.spec.cjs',
    'admin-route-presence-real.spec.cjs',
    'admin-logs-real-data.spec.cjs'
  ]

  const offenders = []
  const helperRequirePattern = /require\(['"]\.\/helpers\/test-users\.cjs['"]\)/
  const inlineSuperAdminPattern = /const\s+SUPER_ADMIN\s*=\s*\{\s*username\s*:\s*['"]admin_a['"]\s*,\s*password\s*:\s*['"]change-me['"]\s*,\s*organizationId\s*:\s*['"]1['"]\s*\}/

  for (const fileName of files) {
    const fileUrl = new URL(fileName, TEST_ROOT)
    const source = await readFile(fileUrl, 'utf8')
    if (!helperRequirePattern.test(source) || inlineSuperAdminPattern.test(source)) {
      offenders.push(fileName)
    }
  }

  assert.deepEqual(offenders, [])
})

test('selected smoke suites should use shared test user helper instead of inline tenant fixtures', async () => {
  const fileExpectations = new Map([
    ['admin-backup-real.spec.cjs', ['SUPER_ADMIN']],
    ['admin-monitoring-real.spec.cjs', ['SUPER_ADMIN']],
    ['admin-monitoring-network.spec.cjs', ['SUPER_ADMIN']],
    ['admin-monitoring-response-time.spec.cjs', ['SUPER_ADMIN']],
    ['admin-monitoring-services-online.spec.cjs', ['SUPER_ADMIN']],
    ['admin-ops-real.spec.cjs', ['SUPER_ADMIN']],
    ['admin-tasks-real.spec.cjs', ['SUPER_ADMIN']],
    ['admin-k8s-real.spec.cjs', ['SUPER_ADMIN']],
    ['tenant-admin-system-and-members.spec.cjs', ['SUPER_ADMIN', 'TENANT2_ADMIN']],
    ['registered-user-cross-tenant-join.spec.cjs', ['TENANT1_MEMBER', 'TENANT2_ADMIN']]
  ])

  const offenders = []

  for (const [fileName, exportedKeys] of fileExpectations) {
    const fileUrl = new URL(fileName, TEST_ROOT)
    const source = await readFile(fileUrl, 'utf8')
    const hasHelperRequire = /require\(['"]\.\/helpers\/test-users\.cjs['"]\)/.test(source)
    const missingExports = exportedKeys.filter((key) => !new RegExp(`\\b${key}\\b`).test(source))
    const hasInlineCredentials = /(const\s+(SUPER_ADMIN|TENANT_ADMIN|TARGET_ADMIN|MEMBER)\s*=\s*\{)|(admin_a['"]\s*,\s*password\s*:\s*['"]change-me['"])|(admin_b['"]\s*,\s*password\s*:\s*['"]123456['"])|(member_a['"]\s*,\s*password\s*:\s*['"]123456['"])/.test(source)

    if (!hasHelperRequire || missingExports.length || hasInlineCredentials) {
      offenders.push(fileName)
    }
  }

  assert.deepEqual(offenders, [])
})
