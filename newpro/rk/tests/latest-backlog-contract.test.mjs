import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const rootUrl = new URL('../../../', import.meta.url)

async function readRepoFile(relativePath) {
  return fs.readFile(new URL(relativePath, rootUrl), 'utf8')
}

test('new admin routes should be persisted in menu seed data', async () => {
  const sql = await readRepoFile('basedata/05_insert_menu_data.sql')
  const updater = await readRepoFile('rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java')
  const requiredMenus = [
    ['/admin/operation/logs', 'admin_operation_logs'],
    ['/admin/operation/monitoring', 'admin_operation_monitoring'],
    ['/admin/operation/tasks', 'admin_operation_tasks'],
    ['/admin/operation/jenkins', 'admin_operation_jenkins'],
    ['/admin/operation/backup', 'admin_operation_backup'],
    ['/admin/operation/k8s', 'admin_operation_k8s'],
    ['/admin/operation/nacos', 'admin_operation_nacos'],
    ['/admin/operation/topology', 'admin_operation_topology'],
    ['/admin/operation/traffic-center', 'admin_operation_traffic_center'],
    ['/admin/operation/notification-center', 'admin_operation_notification_center'],
    ['/admin/operation/api-workbench', 'admin_operation_api_workbench'],
    ['/admin/operation/rainbond', 'admin_operation_rainbond'],
    ['/admin/operation/deploy-package', 'admin_operation_deploy_package'],
    ['/admin/operation/service-monitor', 'admin_operation_service_monitor'],
    ['/admin/operation/data-export', 'admin_operation_data_export'],
    ['/admin/finance', 'admin_finance'],
    ['/admin/finance/budget', 'admin_finance_budget'],
    ['/admin/finance/allocation', 'admin_finance_allocation'],
    ['/admin/finance/reimbursement', 'admin_finance_reimbursement'],
    ['/admin/finance/voucher', 'admin_finance_voucher'],
    ['/admin/finance/ledger', 'admin_finance_ledger'],
    ['/admin/finance/report', 'admin_finance_report'],
    ['/admin/finance/audit', 'admin_finance_audit'],
    ['/admin/content/wiki', 'admin_content_wiki'],
    ['/admin/activity/photo-gallery', 'admin_activity_photo_gallery'],
    ['/admin/club/member-graph', 'admin_club_member_graph'],
    ['/admin/activity/vote', 'admin_activity_vote'],
    ['/admin/club/resources', 'admin_club_resources'],
    ['/admin/content/data-diff', 'admin_content_data_diff'],
    ['/admin/activity/credit', 'admin_activity_credit'],
    ['/admin/activity/volunteer', 'admin_activity_volunteer']
  ]

  for (const [path, code] of requiredMenus) {
    assert.match(sql, new RegExp(`'${path.replaceAll('/', '\\/')}'`), `missing menu path ${path}`)
    assert.match(sql, new RegExp(`'${code}'`), `missing menu code ${code}`)
    assert.match(updater, new RegExp(`"${path.replaceAll('/', '\\/')}"`), `updater missing menu path ${path}`)
    assert.match(updater, new RegExp(`"${code}"`), `updater missing menu code ${code}`)
  }

  assert.match(sql, /INSERT IGNORE INTO role_menu[\s\S]*\(1,\s*724\)/)
  assert.match(updater, /deactivateKnownLegacyAdminMenus/)
  assert.match(updater, /path IN \('\/admin\/club\/finance'\)/)
})

test('BIP style review prototypes should exist under temp before production rewrite', async () => {
  const frontend = await readRepoFile('temp/bip-style-frontend.html')
  const admin = await readRepoFile('temp/bip-style-admin.html')

  for (const html of [frontend, admin]) {
    assert.match(html, /YonBIP/)
    assert.match(html, /四叶草/)
    assert.match(html, /应用中心/)
    assert.match(html, /工作台/)
    assert.match(html, /#e53935/)
  }
  assert.match(frontend, /社团门户/)
  assert.match(admin, /财务管理/)
  assert.match(admin, /K8s 集群/)
})
