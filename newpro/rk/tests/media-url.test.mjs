import test from 'node:test'
import assert from 'node:assert/strict'

const moduleUrl = new URL('../src/utils/mediaUrl.js', import.meta.url)

test('resolveMediaUrl should map bucket scoped relative paths to runtime minio base url', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=relative`)

  assert.equal(
    mediaUrl.resolveMediaUrl('rk-user/avatar/2026/04/24/demo.jpg'),
    'https://minio.runtime.local/rk-user/avatar/2026/04/24/demo.jpg',
  )
})

test('resolveMediaUrl should rewrite internal minio urls to runtime minio base url', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=internal-absolute`)

  assert.equal(
    mediaUrl.resolveMediaUrl('http://127.0.0.1:9000/rk-user/avatar/2026/04/24/demo.jpg'),
    'https://minio.runtime.local/rk-user/avatar/2026/04/24/demo.jpg',
  )
})

test('resolveMediaUrl should strip same-origin minio proxy prefix before rewriting absolute urls', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://example.com/minio-files',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=minio-proxy-absolute`)

  assert.equal(
    mediaUrl.resolveMediaUrl('http://203.0.113.10:30001/minio-files/rk-user/avatar/2026/04/24/demo.jpg'),
    'https://example.com/minio-files/rk-user/avatar/2026/04/24/demo.jpg',
  )
})

test('resolveMediaUrl should keep default bucket prefix when runtime minio base is bucket root', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-bucket-prefix`)

  assert.equal(
    mediaUrl.resolveMediaUrl('rk-bucket/e376cf06341a4b29ad595f66d18c7a66.txt'),
    'https://minio.runtime.local/rk-bucket/e376cf06341a4b29ad595f66d18c7a66.txt',
  )
})

test('resolveMediaUrl should strip default bucket prefix when runtime minio base already includes bucket', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local/rk-bucket',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-bucket-prefix-in-base`)

  assert.equal(
    mediaUrl.resolveMediaUrl('rk-bucket/e376cf06341a4b29ad595f66d18c7a66.txt'),
    'https://minio.runtime.local/rk-bucket/e376cf06341a4b29ad595f66d18c7a66.txt',
  )
})

test('resolveMediaUrl should map bare object keys to runtime minio base url', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://example.com/minio-files',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=bare-object-key`)

  assert.equal(
    mediaUrl.resolveMediaUrl('4f1cc8bbd7bc4facb9b5e50a2446aba0.txt'),
    'https://example.com/minio-files/rk-bucket/4f1cc8bbd7bc4facb9b5e50a2446aba0.txt',
  )
})

test('resolveMediaUrl should repair same-origin minio proxy urls missing default bucket', async () => {
  globalThis.window = {
    location: { origin: 'https://example.com' },
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://example.com/minio-files',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=minio-proxy-bare-object-key`)

  assert.equal(
    mediaUrl.resolveMediaUrl('https://example.com/minio-files/4f1cc8bbd7bc4facb9b5e50a2446aba0.txt'),
    'https://example.com/minio-files/rk-bucket/4f1cc8bbd7bc4facb9b5e50a2446aba0.txt',
  )
})

test('normalizeUserInfoMedia should drop legacy rk-bucket avatar paths that no longer resolve', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-rk-bucket-avatar`)

  assert.deepEqual(
    mediaUrl.normalizeUserInfoMedia({
      icon: 'http://203.0.113.10:9000/rk-bucket/users/avatars/demo-avatar.png',
      avatar: 'http://203.0.113.10:9000/rk-bucket/users/avatars/demo-avatar.png',
    }),
    {
      icon: '',
      avatar: '',
    },
  )
})

test('normalizeUserInfoMedia should drop legacy slash-avatar paths that no longer resolve', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-slash-avatar`)

  assert.deepEqual(
    mediaUrl.normalizeUserInfoMedia({
      icon: '/avatar/b78b751c-c5dc-4153-b6a3-545111ff5eef.jpg',
      avatar: '/avatar/b78b751c-c5dc-4153-b6a3-545111ff5eef.jpg',
    }),
    {
      icon: '',
      avatar: '',
    },
  )
})

test('resolveMediaUrl should drop legacy slash-images paths that no longer resolve', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-slash-images`)

  assert.equal(
    mediaUrl.resolveMediaUrl('/images/news/00394793-8463-49bd-9462-17b1a2cc7c36.jpg'),
    '',
  )
})

test('resolveMediaUrl should drop legacy tmp media paths that no longer resolve', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-tmp-media`)

  assert.equal(
    mediaUrl.resolveMediaUrl('tmp/news/cover-update-1777223246160.png'),
    '',
  )
})

test('normalizeUserInfoMedia should drop relative users-avatar legacy paths', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=legacy-relative-users-avatar`)

  assert.deepEqual(
    mediaUrl.normalizeUserInfoMedia({
      icon: 'users/avatars/demo-avatar.png',
      avatar: 'users/avatars/demo-avatar.png',
    }),
    {
      icon: '',
      avatar: '',
    },
  )
})

test('normalizeUserInfoMedia should keep managed avatar paths and resolve them against runtime minio', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {}

  const mediaUrl = await import(`${moduleUrl.href}?case=managed-avatar`)

  assert.deepEqual(
    mediaUrl.normalizeUserInfoMedia({
      icon: 'rk-user/avatar/2026/04/24/demo.jpg',
      avatar: 'rk-user/avatar/2026/04/24/demo.jpg',
    }),
    {
      icon: 'https://minio.runtime.local/rk-user/avatar/2026/04/24/demo.jpg',
      avatar: 'https://minio.runtime.local/rk-user/avatar/2026/04/24/demo.jpg',
    },
  )
})
