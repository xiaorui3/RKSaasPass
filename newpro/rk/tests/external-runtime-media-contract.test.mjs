import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('dev frontend should proxy minio through same-origin https-safe path', async () => {
  const viteConfig = await readFile(new URL('../vite.config.js', import.meta.url), 'utf8')

  assert.match(
    viteConfig,
    /['"]\/minio-files['"]\s*:\s*\{/,
    'vite dev server should expose a /minio-files proxy entry for browser-safe media access'
  )
})

test('runtime-config should derive external media base from current origin', async () => {
  const runtimeConfig = await readFile(new URL('../public/runtime-config.js', import.meta.url), 'utf8')

  assert.match(
    runtimeConfig,
    /window\.location\.origin/,
    'runtime-config.js should derive runtime urls from the current origin for external access'
  )

  assert.match(
    runtimeConfig,
    /minioBaseUrl:\s*window\.location\.origin\s*\+\s*['"]\/minio-files['"]/,
    'runtime-config.js should map media urls to a same-origin /minio-files path'
  )
})
