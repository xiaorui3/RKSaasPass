import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

test('news detail should resolve video urls through managed media resolver', async () => {
  const source = await readFile(new URL('../src/views/NewsDetail.vue', import.meta.url), 'utf8')

  assert.match(
    source,
    /<video\s+:src="resolveMediaUrl\(news\.videoUrl\)"/,
    'NewsDetail video should use resolveMediaUrl(news.videoUrl) so relative managed paths render correctly'
  )
})
