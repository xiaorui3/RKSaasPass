import test from 'node:test'
import assert from 'node:assert/strict'
import { readdir, readFile } from 'node:fs/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(currentDir, '..', '..', '..')

const resourceRoots = [
  'rk-activity/src/main/resources',
  'rk-content/src/main/resources',
  'rk-file/src/main/resources',
  'rk-message/rk-message-service/src/main/resources',
  'rk-trade/src/main/resources'
].map((relativePath) => path.resolve(repoRoot, relativePath))

const allowedExtensions = new Set(['.yml', '.yaml', '.properties'])
const classicMojibakeMarkers = [
  [0x00c3],
  [0x00c2],
  [0x00ef, 0x00bf, 0x00bd],
  [0x9225],
  [0x922e],
  [0x9241],
  [0x6506],
  [0x651c],
  [0x7ec0, 0x60e7],
  [0x68e3, 0x682d],
  [0x93b4, 0x621c],
  [0x6d93, 0xe043, 0x6c49],
  [0x7ec9, 0x71b8, 0x57db],
  [0x9366, 0x3126, 0x724e],
  [0x9422, 0x3126, 0x57db],
  [0x93cd, 0x2033, 0x5f38],
  [0x93c8, 0xe046, 0x7161],
  [0x8930, 0x64b3, 0x58a0]
].map((codes) => String.fromCodePoint(...codes))
const mojibakePatterns = [
  { name: 'replacement character', pattern: /\uFFFD/ },
  { name: 'private-use Unicode', pattern: /[\uE000-\uF8FF]/ },
  { name: 'classic UTF-8 mojibake', pattern: new RegExp(classicMojibakeMarkers.join('|')) }
]

async function collectConfigFiles(rootDir) {
  const files = []
  const entries = await readdir(rootDir, { withFileTypes: true })
  for (const entry of entries) {
    const fullPath = path.join(rootDir, entry.name)
    if (entry.isDirectory()) {
      files.push(...await collectConfigFiles(fullPath))
      continue
    }
    if (allowedExtensions.has(path.extname(entry.name))) {
      files.push(fullPath)
    }
  }
  return files
}

test('backend runtime config files should not contain mojibake', async () => {
  const issues = []

  for (const rootDir of resourceRoots) {
    for (const filePath of await collectConfigFiles(rootDir)) {
      const source = await readFile(filePath, 'utf8')
      const relativePath = path.relative(repoRoot, filePath).replace(/\\/g, '/')
      for (const { name, pattern } of mojibakePatterns) {
        if (pattern.test(source)) {
          issues.push(`${relativePath}: ${name}`)
        }
      }
    }
  }

  assert.deepEqual(issues, [])
})
