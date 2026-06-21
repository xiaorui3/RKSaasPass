import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('theme DTO and store should persist both admin and frontend background images', async () => {
  const dto = await readSource('../../../rk-user/src/main/java/com/tianji/user/domain/dto/TenantThemeConfigDTO.java')
  const store = await readSource('../src/stores/theme.js')
  const styles = await readSource('../src/styles/index.scss')
  const app = await readSource('../src/App.vue')

  assert.equal(dto.includes('frontendBgColor'), true)
  assert.equal(dto.includes('frontendBgImage'), true)
  assert.equal(dto.includes('frontendBgOpacity'), true)
  assert.equal(store.includes('resolveMediaUrl'), true)
  assert.equal(store.includes('applyFrontendBg'), true)
  assert.equal(store.includes('removeProperty(\'--rk-admin-bg-image\')'), true)
  assert.equal(styles.includes('--rk-frontend-bg-image'), true)
  assert.equal(app.includes('themeStore.clearTheme()'), true)
})

test('theme config page should save background fields with normal theme saves and expose frontend upload controls', async () => {
  const page = await readSource('../src/views/admin/system/ThemeConfig.vue')

  assert.equal(page.includes('前台背景自定义'), true)
  assert.equal(page.includes('frontendBgForm'), true)
  assert.equal(page.includes('handleFrontendBgUpload'), true)
  assert.equal(page.includes('frontendBgColor: frontendBgForm.bgColor'), true)
  assert.equal(page.includes('adminBgColor: bgForm.bgColor'), true)
  assert.equal(page.includes('localStorage.setItem(\'rk-admin-bg-config\''), false)
  assert.equal(page.includes('catch {}'), false)
})
