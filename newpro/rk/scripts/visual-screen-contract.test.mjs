import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const view = readFileSync(resolve(root, 'src/views/admin/operation/VisualScreen.vue'), 'utf8')
const api = readFileSync(resolve(root, 'src/api/data.js'), 'utf8')

function assert(condition, message) {
  if (!condition) {
    throw new Error(message)
  }
}

assert(
  api.includes('getSaasVisualScreen') && api.includes("url: '/data/saas-visual-screen'"),
  'data api must expose getSaasVisualScreen against /data/saas-visual-screen'
)

assert(
  view.includes('getSaasVisualScreen'),
  'VisualScreen.vue must call the rk-data SaaS visual screen API'
)

assert(
  !view.includes("@/api/admin-ops") &&
    !view.includes('getMonitoringOverview') &&
    !view.includes('getK8sOverview') &&
    !view.includes('listDeployPackages'),
  'VisualScreen.vue must not keep depending on the admin ops-only API contract'
)

assert(
  view.includes('tenantId') && view.includes('cache'),
  'VisualScreen.vue must render or consume tenantId and cache metadata'
)

console.log('visual screen contract ok')
