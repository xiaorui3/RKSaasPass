import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

function section(source, startMarker, endMarker) {
  const start = source.indexOf(startMarker)
  const end = source.indexOf(endMarker, start)
  assert.notEqual(start, -1)
  assert.notEqual(end, -1)
  return source.slice(start, end)
}

test('public portal shell keeps announcements outside the two-column frame and renders side nav as a template element', async () => {
  const source = await readSource('../src/layouts/MainLayout.vue')
  const template = section(source, '<template>', '</template>')
  const style = section(source, '<style', '</style>')

  assert.match(template, /<GlobalAnnouncement\s*\/>\s*<SystemAnnouncement\s*\/>\s*<div class="portal-layout-frame">/)
  assert.match(template, /<nav class="portal-side-nav" aria-label="前台导航">/)
  assert.match(template, /<aside class="portal-sidebar">[\s\S]*<\/aside>\s*<div class="portal-workspace">/)
  assert.match(style, /\.portal-side-nav\s*\{/)
  assert.equal(style.includes('<nav class="portal-side-nav"'), false)
  assert.equal(template.includes('.portal-side-nav {'), false)
})

test('shared shells avoid BOM and clean user-facing names before rendering', async () => {
  const [mainLayout, adminLayout, tenantBranding] = await Promise.all([
    readSource('../src/layouts/MainLayout.vue'),
    readSource('../src/layouts/AdminLayout.vue'),
    readSource('../src/utils/tenantBranding.js')
  ])

  for (const [label, source] of [
    ['MainLayout.vue', mainLayout],
    ['AdminLayout.vue', adminLayout],
    ['tenantBranding.js', tenantBranding]
  ]) {
    assert.notEqual(source.charCodeAt(0), 0xfeff, `${label} should not start with a UTF-8 BOM`)
  }

  assert.match(mainLayout, /const\s+switchTenantOptions\s*=\s*computed\(\(\)\s*=>\s*\(userStore\.switchableTenantViews/)
  assert.match(mainLayout, /tenantName:\s*cleanDisplayText\(item\?\.tenantName/)
  assert.match(mainLayout, /roleName:\s*cleanDisplayText\(item\?\.roleName/)
  assert.match(adminLayout, /const\s+userInitial\s*=\s*computed/)
  assert.match(adminLayout, /const\s+roleName\s*=\s*computed\(\(\)\s*=>\s*cleanDisplayText/)
  assert.match(adminLayout, /tenantName:\s*cleanDisplayText\(item\?\.tenantName/)
  assert.match(adminLayout, /roleName:\s*cleanDisplayText\(item\?\.roleName/)
})

test('public portal keeps document scrolling available for tall front pages', async () => {
  const source = await readSource('../src/App.vue')
  const globalStyle = section(source, '<style>', '</style>')
  const htmlBodyRule = globalStyle.match(/html,\s*body\s*\{[\s\S]*?\}/)?.[0] || ''

  assert.match(htmlBodyRule, /min-height:\s*100%/)
  assert.doesNotMatch(htmlBodyRule, /(?:^|[\s;])height:\s*100%/)
})
