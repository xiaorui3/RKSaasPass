import test from 'node:test'
import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(currentDir, '..')
const scanRoots = [
  path.resolve(repoRoot, 'src'),
  path.resolve(repoRoot, 'tests'),
  path.resolve(repoRoot, '..', '..', 'docs')
]
const allowedExtensions = new Set(['.vue', '.js', '.ts', '.mjs', '.cjs', '.md'])
const ignoredRelativePaths = new Set([
  'tests/admin-user-provisioning.spec.cjs',
  'tests/all-tenant-admission-copy.spec.cjs',
  'tests/front-copy-locale.test.mjs',
  'tests/source-mojibake-contract.test.mjs'
])
const mojibakeMarkers = [
  '\u93bc\u6ec5\u50a8',
  '\u9354\u72ba\u6d47\u6d93',
  '\u9427\u8bf2\u7d8d',
  '\u5a09\u3125\u553d',
  '\u7035\u55d9\u721c',
  '\u93c2\u56e6\u6b22\u6d93\u5a41\u7d36',
  '\u5a34\u5b2d\u762f',
  '\u9365\u5267\u5896',
  '\u9350\u546e\u5e39\u942e',
  '\u95ad\ue1bb\u6b22',
  '\u9359\u6226\u20ac'
]

async function collectFiles(rootDir) {
  const files = []
  const entries = await readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name)
    if (entry.isDirectory()) {
      files.push(...await collectFiles(fullPath))
      continue
    }
    if (allowedExtensions.has(path.extname(entry.name))) {
      if (entry.name === 'text-encoding-sanity.test.mjs') {
        continue
      }
      if (entry.name.endsWith('.spec.cjs')) {
        continue
      }
      files.push(fullPath)
    }
  }
  return files
}

function findTextIssues(relativePath, source) {
  const issues = []
  if (source.includes('?'.repeat(4))) {
    issues.push('contains placeholder question marks')
  }
  if (source.includes('\uFFFD')) {
    issues.push('contains replacement characters')
  }
  if ([...source].some((char) => char >= '\uE000' && char <= '\uF8FF')) {
    issues.push('contains private-use Unicode characters')
  }
  if (mojibakeMarkers.some((marker) => source.includes(marker))) {
    issues.push('contains mojibake marker text')
  }
  return issues.length ? `${relativePath}: ${issues.join(', ')}` : null
}

test('project text fixtures and docs should not contain mojibake sentinels', async () => {
  const issues = []

  for (const rootDir of scanRoots) {
    for (const filePath of await collectFiles(rootDir)) {
      const relativePath = path.relative(repoRoot, filePath).replace(/\\/g, '/')
      if (ignoredRelativePaths.has(relativePath)) {
        continue
      }
      const source = await readFile(filePath, 'utf8')
      const issue = findTextIssues(relativePath, source)
      if (issue) {
        issues.push(issue)
      }
    }
  }

  assert.deepEqual(issues, [])
})
