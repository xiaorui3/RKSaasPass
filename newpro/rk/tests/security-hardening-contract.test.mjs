import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(currentDir, '../../..')

const readRepoFile = (relativePath) =>
  readFileSync(path.join(repoRoot, relativePath), 'utf8')

const nginxFiles = [
  'newpro/rk/nginx.conf'
]

test('gateway CORS does not reflect arbitrary origins when credentials are enabled', () => {
  const bootstrap = readRepoFile('rk-gateway/src/main/resources/bootstrap.yml')

  assert.doesNotMatch(
    bootstrap,
    /allowedOriginPatterns:\s*["']?\*["']?/,
    'gateway must not use wildcard origin patterns with credentialed CORS'
  )
  assert.match(
    bootstrap,
    /allowedOrigins:\s*\n\s*-\s*["']?\$\{RK_WEB_PUBLIC_ORIGIN:http:\/\/localhost:5173\}["']?/,
    'gateway should read the deploy public origin from RK_WEB_PUBLIC_ORIGIN'
  )
  assert.match(
    bootstrap,
    /allowedOrigins:[\s\S]*http:\/\/localhost:5173/,
    'gateway should allow the local frontend dev server'
  )
  assert.match(
    bootstrap,
    /allowedOrigins:[\s\S]*http:\/\/127\.0\.0\.1:30080/,
    'gateway should allow the local frontend container origin without publishing a private host'
  )
})

test('frontend nginx blocks sensitive and API documentation paths before SPA fallback', () => {
  for (const relativePath of nginxFiles) {
    const config = readRepoFile(relativePath)

    assert.match(
      config,
      /location\s+~\*\s+\^\/\?\(\?:\\\.git\|\\\.svn\|\\\.hg\|\\\.env\|backup\|config\)\(\/\|\$\)/,
      `${relativePath} should block sensitive dot/config/backup paths before try_files`
    )
    assert.match(
      config,
      /location\s+~\*\s+\^\/\?\(\?:swagger\|api-docs\|doc\|doc\\\.html\|swagger-resources\|v2\/api-docs\|webjars\)\(\/\|\$\)/,
      `${relativePath} should block public Swagger/API documentation routes`
    )
    assert.ok(
      config.indexOf('location ~* ^/?(?:\\.git|\\.svn|\\.hg|\\.env|backup|config)(/|$)') <
        config.indexOf('try_files $uri $uri/ /index.html'),
      `${relativePath} should evaluate sensitive-path deny rules before SPA fallback`
    )
  }
})

test('frontend nginx sets security headers for static pages and proxied responses', () => {
  for (const relativePath of nginxFiles) {
    const config = readRepoFile(relativePath)

    for (const header of [
      'X-Frame-Options',
      'X-Content-Type-Options',
      'X-XSS-Protection',
      'Referrer-Policy',
      'Permissions-Policy',
      'Content-Security-Policy'
    ]) {
      assert.match(
        config,
        new RegExp(`add_header\\s+${header}\\s+["'][^\\n]+["']\\s+always;`),
        `${relativePath} should add ${header} with always`
      )
    }
  }
})

test('frontend CSP stays compatible with the current Vue runtime bundle', () => {
  for (const relativePath of nginxFiles) {
    const config = readRepoFile(relativePath)
    const cspLine = config
      .split(/\r?\n/)
      .find((line) => line.includes('add_header Content-Security-Policy'))

    assert.ok(cspLine, `${relativePath} should define a Content-Security-Policy header`)
    assert.match(
      cspLine,
      /script-src[^;]*'unsafe-eval'/,
      `${relativePath} should allow unsafe-eval because the current built frontend still uses new Function at runtime`
    )
  }
})

test('MinIO public URLs stay same-origin and do not publish raw infrastructure addresses', () => {
  const localNginx = readRepoFile('newpro/rk/nginx.conf')
  const fileBootstrap = readRepoFile('rk-file/src/main/resources/bootstrap.yml')

  for (const [label, config] of [
    ['local nginx', localNginx]
  ]) {
    assert.match(
      config,
      /absolute_redirect\s+off;/,
      `${label} should keep canonical slash redirects relative so upstream host/IP is not exposed`
    )
    assert.match(
      config,
      /port_in_redirect\s+off;/,
      `${label} should not append internal ports to generated redirects`
    )
  }

  assert.match(
    localNginx,
    /location\s*=\s*\/minio-files\s*\{[\s\S]*return\s+308\s+\/minio-files\/;/,
    'local nginx should redirect /minio-files to same-origin /minio-files/'
  )
  assert.doesNotMatch(
    fileBootstrap,
    /publicBaseUrl:\s*http:\/\/\d{1,3}(?:\.\d{1,3}){3}/,
    'committed MinIO configuration should not expose raw public or private IP addresses'
  )
  assert.match(
    fileBootstrap,
    /rk-shared-minio\.yaml/,
    'MinIO shared configuration should be supplied through the deployment Nacos namespace'
  )
})
