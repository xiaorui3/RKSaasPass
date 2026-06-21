import test from 'node:test'
import assert from 'node:assert/strict'
import { manualChunks } from '../build/manualChunks.js'

test('manual chunk classifier isolates the large shared frontend libraries', () => {
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/vue-i18n/dist/vue-i18n.runtime.mjs'),
    'i18n'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/@intlify/core-base/dist/core-base.mjs'),
    'i18n'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/echarts/core.js'),
    undefined
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/xlsx/xlsx.mjs'),
    'spreadsheet'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/file-saver/dist/FileSaver.min.js'),
    'spreadsheet'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/cropperjs/dist/cropper.esm.js'),
    'cropper'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/@element-plus/icons-vue/dist/index.js'),
    'element-plus-icons'
  )
  assert.equal(
    manualChunks('/repo/newpro/rk/node_modules/pinia/dist/pinia.mjs'),
    'vue-vendor'
  )
  assert.equal(manualChunks('/repo/newpro/rk/src/views/Home.vue'), undefined)
})
