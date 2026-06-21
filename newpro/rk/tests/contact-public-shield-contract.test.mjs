import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('contact api should expose public shield and public submit helpers', async () => {
  const source = await readSource('../src/api/contact.js')

  assert.match(source, /export function fetchContactPublicShield\s*\(/)
  assert.match(source, /url:\s*['"]\/api\/contact\/public\/shield['"]/)
  assert.match(source, /export function submitPublicContactMessage\s*\(/)
  assert.match(source, /url:\s*['"]\/api\/contact\/public\/submit['"]/)
})

test('contact page should initialize shield flow and submit honeypot protected payload', async () => {
  const source = await readSource('../src/views/Contact.vue')

  assert.match(source, /fetchContactPublicShield/)
  assert.match(source, /submitPublicContactMessage/)
  assert.match(source, /shieldToken/)
  assert.match(source, /issuedAt/)
  assert.match(source, /honeypot/)
})
