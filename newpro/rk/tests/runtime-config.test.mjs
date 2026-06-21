import test from 'node:test'
import assert from 'node:assert/strict'

const moduleUrl = new URL('../src/utils/runtimeConfig.js', import.meta.url)

test('runtime config falls back to vite env values', async () => {
  delete globalThis.window
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {
    VITE_GATEWAY_BASE_URL: 'https://gateway.example.com',
    VITE_MINIO_BASE_URL: 'https://minio.example.com',
  }

  const runtimeConfig = await import(`${moduleUrl.href}?case=vite-env`)

  assert.equal(runtimeConfig.getGatewayBaseUrl(), 'https://gateway.example.com')
  assert.equal(runtimeConfig.getMinioBaseUrl(), 'https://minio.example.com')
})

test('runtime config prefers window injected values over vite env', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      gatewayBaseUrl: 'https://gw.runtime.local',
      minioBaseUrl: 'https://minio.runtime.local',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {
    VITE_GATEWAY_BASE_URL: 'https://gateway.example.com',
    VITE_MINIO_BASE_URL: 'https://minio.example.com',
  }

  const runtimeConfig = await import(`${moduleUrl.href}?case=runtime-window`)

  assert.equal(runtimeConfig.getGatewayBaseUrl(), 'https://gw.runtime.local')
  assert.equal(runtimeConfig.getMinioBaseUrl(), 'https://minio.runtime.local')
})

test('runtime config should preserve relative runtime paths for dev and preview proxies', async () => {
  globalThis.window = {
    __RK_RUNTIME_CONFIG__: {
      gatewayBaseUrl: '',
      minioBaseUrl: '/minio-files',
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {
    VITE_GATEWAY_BASE_URL: 'https://gateway.example.com',
    VITE_MINIO_BASE_URL: 'https://minio.example.com',
  }

  const runtimeConfig = await import(`${moduleUrl.href}?case=relative-runtime`)

  assert.equal(runtimeConfig.getGatewayBaseUrl(), '')
  assert.equal(runtimeConfig.getMinioBaseUrl(), '/minio-files')
})

test('runtime config resolves default tenant from host map before generic default', async () => {
  globalThis.window = {
    location: {
      host: 'example.com:30001',
      hostname: 'example.com',
    },
    __RK_RUNTIME_CONFIG__: {
      defaultTenantId: '1',
      tenantHostMap: {
        'example.com': '30',
      },
    },
  }
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {
    VITE_DEFAULT_TENANT_ID: '2',
  }

  const runtimeConfig = await import(`${moduleUrl.href}?case=tenant-host-map`)

  assert.equal(runtimeConfig.getDefaultTenantId(), '30')
})

test('runtime config falls back to vite default tenant when runtime is absent', async () => {
  delete globalThis.window
  globalThis.__RK_TEST_IMPORT_META_ENV__ = {
    VITE_DEFAULT_TENANT_ID: '30',
  }

  const runtimeConfig = await import(`${moduleUrl.href}?case=tenant-env`)

  assert.equal(runtimeConfig.getDefaultTenantId(), '30')
})
