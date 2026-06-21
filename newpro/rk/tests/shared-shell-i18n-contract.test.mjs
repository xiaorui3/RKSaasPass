import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('shared layouts and auth entry views should wire visible copy through vue-i18n', async () => {
  const [mainLayout, adminLayout, loginView, loginModal] = await Promise.all([
    readSource('../src/layouts/MainLayout.vue'),
    readSource('../src/layouts/AdminLayout.vue'),
    readSource('../src/views/Login.vue'),
    readSource('../src/components/LoginModal.vue')
  ])

  assert.equal(mainLayout.includes("useI18n"), true)
  assert.equal(mainLayout.includes("t('nav.login')"), true)
  assert.equal(mainLayout.includes("t('common.search')"), true)

  assert.equal(adminLayout.includes("useI18n"), true)
  assert.equal(adminLayout.includes("t('admin.dashboard')"), true)
  assert.equal(adminLayout.includes("t('nav.profile')"), true)

  assert.equal(loginView.includes("useI18n"), true)
  assert.equal(loginView.includes("t('auth.username')"), true)
  assert.equal(loginView.includes("t('auth.password')"), true)

  assert.equal(loginModal.includes("useI18n"), true)
  assert.equal(loginModal.includes("t('loginModal.title')"), true)
  assert.equal(loginModal.includes("t('auth.goRegister')"), true)
})
