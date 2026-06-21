import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const srcDir = path.resolve(currentDir, '../src')

function readSource(relativePath) {
  return readFileSync(path.join(srcDir, relativePath), 'utf8')
}

test('public alumni profile form route is available without login', () => {
  const routerSource = readSource('router/index.js')

  assert.match(routerSource, /path:\s*['"]\/alumni\/profile-form['"]/)
  assert.match(routerSource, /name:\s*['"]AlumniProfileForm['"]/)
  assert.doesNotMatch(
    routerSource,
    /path:\s*['"]\/alumni\/profile-form['"][\s\S]{0,220}requiresAuth:\s*true/,
    'alumni profile form is opened from an email link and must not require login'
  )
})

test('alumni profile form API goes through the managed alumni gateway path', () => {
  const apiSource = readSource('api/alumni.js')

  assert.match(apiSource, /getAlumniProfileForm/)
  assert.match(apiSource, /submitAlumniProfileForm/)
  assert.match(apiSource, /\/api\/alumni\/profile-form\/\$\{encodeURIComponent\(token\)\}/)
})

test('alumni profile form page reads token and submits the one-time form', () => {
  const viewPath = path.join(srcDir, 'views/AlumniProfileForm.vue')

  assert.equal(existsSync(viewPath), true)
  const source = readFileSync(viewPath, 'utf8')

  assert.match(source, /route\.query\.token/)
  assert.match(source, /getAlumniProfileForm/)
  assert.match(source, /submitAlumniProfileForm/)
  assert.match(source, /校友信息/)
  assert.match(source, /只能提交一次/)
})
