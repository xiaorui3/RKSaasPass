import test from 'node:test'
import assert from 'node:assert/strict'

const galleryModuleUrl = new URL('../src/utils/activityGallery.js', import.meta.url)
const financeModuleUrl = new URL('../src/utils/adminFinance.js', import.meta.url)
const k8sModuleUrl = new URL('../src/utils/k8sTopology.js', import.meta.url)

test('activity gallery helpers should fuzzy search by keyword and date range', async () => {
  const gallery = await import(`${galleryModuleUrl.href}?case=filter`)
  const activities = [
    {
      id: 1,
      activityName: '浜旀湀缂栫▼璁粌钀?,
      organizer: '杞欢椤圭洰寮€鍙戠ぞ鍥?,
      location: '鍒涙柊妤?,
      activityStatus: 2,
      startTime: '2026-05-18T09:00:00',
      endTime: '2026-05-18T18:00:00'
    },
    {
      id: 2,
      activityName: '鍥涙湀绡悆璧?,
      organizer: '浣撹偛閮?,
      location: '浣撹偛棣?,
      activityStatus: 4,
      startTime: '2026-04-11T10:00:00',
      endTime: '2026-04-11T12:00:00'
    },
    {
      id: 3,
      activityName: '浜旀湀绠楁硶鍒嗕韩',
      organizer: '杞欢椤圭洰寮€鍙戠ぞ鍥?,
      location: '绾夸笂浼氳',
      activityStatus: 2,
      startTime: '2026-05-21T19:30:00',
      endTime: '2026-05-21T21:00:00'
    }
  ]

  const result = gallery.filterGalleryActivities(activities, {
    keyword: '杞欢 浜旀湀',
    dateRange: ['2026-05-01', '2026-05-31'],
    status: 2
  })

  assert.deepEqual(result.map((item) => item.id), [3, 1])

  const query = gallery.buildGalleryActivityQuery({
    keyword: '绠楁硶',
    dateRange: ['2026-05-01', '2026-05-31'],
    status: 2,
    recentMode: 'recent30',
    size: 500
  })
  assert.equal(query.keyword, '绠楁硶')
  assert.equal(query.title, '绠楁硶')
  assert.equal(query.recentDays, 30)
  assert.equal(query.size, 500)
  assert.equal(query.startTimeBegin, '2026-05-01 00:00:00')
  assert.equal(query.startTimeEnd, '2026-05-31 23:59:59')
})

test('finance helpers should build enterprise dashboard summary', async () => {
  const finance = await import(`${financeModuleUrl.href}?case=enterprise-summary`)

  const summary = finance.buildFinanceDashboardSummary({
    account: { balance: 6200, totalIncome: 12000, totalExpense: 5800 },
    records: [
      { id: 1, type: 1, amount: 3000, category: 'grant', status: 1, createTime: '2026-05-02 10:00:00' },
      { id: 2, type: 2, amount: 800, category: 'reimbursement', status: 0, createTime: '2026-05-03 10:00:00' },
      { id: 3, type: 2, amount: 2600, category: 'activity', status: 1, createTime: '2026-05-04 10:00:00' }
    ]
  })

  assert.equal(summary.balance, 6200)
  assert.equal(summary.pendingAmount, 800)
  assert.equal(summary.budgetUsageRate, 48.33)
  assert.equal(summary.riskAlerts.length, 1)
  assert.equal(summary.categoryBreakdown.find((item) => item.category === 'activity').expense, 2600)

  const moduleCards = finance.buildFinanceModuleCards({
    account: { balance: 6200, totalIncome: 12000, totalExpense: 5800 },
    records: [
      { id: 1, type: 1, amount: 3000, category: 'grant', businessType: 'INCOME', status: 1, recordNo: 'SR1' },
      { id: 2, type: 2, amount: 800, category: 'reimbursement', businessType: 'REIMBURSEMENT', status: 0, recordNo: 'BX1' },
      { id: 3, type: 2, amount: 2600, category: 'supplies', businessType: 'ASSET', status: 1, recordNo: 'ZC1', proofImageUrl: 'x.png' }
    ]
  })
  assert.equal(moduleCards.length >= 8, true)
  assert.equal(moduleCards.find((item) => item.key === 'asset').count, 1)
  assert.equal(moduleCards.find((item) => item.key === 'archive').count, 1)

  const vouchers = finance.buildFinanceVoucherRows([
    { id: 3, type: 2, amount: 2600, status: 1, recordNo: 'ZC1', budgetItem: '鐗╄祫閲囪喘', title: '璁惧' }
  ])
  assert.equal(vouchers[0].voucherNo, 'PZ-ZC1')
  assert.equal(vouchers[0].creditSubject, '閾惰瀛樻')
})

test('k8s topology helpers should build node pod container port graph', async () => {
  const k8s = await import(`${k8sModuleUrl.href}?case=graph`)

  const graph = k8s.buildK8sTopologyGraph({
    nodes: [
      { name: 'master01', internalIp: '127.0.0.1', status: 'Ready' }
    ],
    pods: [
      {
        namespace: 'shetuanguanlixitong',
        nodeName: 'master01',
        podName: 'rk-gateway-abc',
        status: 'Running',
        containerName: 'rk-gateway',
        image: 'rk-gateway:latest',
        ports: '10010/TCP, 8080/TCP'
      }
    ]
  })

  assert.equal(graph.nodes.some((item) => item.id === 'node:master01'), true)
  assert.equal(graph.nodes.some((item) => item.id === 'pod:shetuanguanlixitong/rk-gateway-abc'), true)
  assert.equal(graph.nodes.some((item) => item.id === 'port:shetuanguanlixitong/rk-gateway-abc/rk-gateway/10010/TCP'), true)
  assert.equal(graph.nodes.some((item) => item.id === 'cluster:rk-web'), true)
  assert.equal(graph.links.some((item) => item.source === 'cluster:rk-web' && item.target === 'node:master01'), true)
  assert.equal(graph.links.some((item) => item.source === 'node:master01' && item.target === 'pod:shetuanguanlixitong/rk-gateway-abc'), true)
})

test('admin layout should expose finance as standalone top-level menu', async () => {
  const fs = await import('node:fs/promises')
  const path = new URL('../src/layouts/AdminLayout.vue', import.meta.url)
  const text = await fs.readFile(path, 'utf8')

  assert.match(text, /path:\s*'\/admin\/finance'/)
  assert.doesNotMatch(text, /\{\s*name:\s*t\('admin\.finance'\),\s*path:\s*'\/admin\/club\/finance'/)
  assert.match(text, /ensureFinanceMenuEntry/)
})

test('enterprise finance should expose dedicated child routes and persisted menu seeds', async () => {
  const fs = await import('node:fs/promises')
  const layoutPath = new URL('../src/layouts/AdminLayout.vue', import.meta.url)
  const routerPath = new URL('../src/router/index.js', import.meta.url)
  const authSeedPath = new URL('../../../rk-auth/rk-auth-service/src/main/java/com/tianji/auth/config/AuthMenuCatalogUpdater.java', import.meta.url)
  const sqlSeedPath = new URL('../../../basedata/05_insert_menu_data.sql', import.meta.url)
  const financePagePath = new URL('../src/views/admin/club/Finance.vue', import.meta.url)
  const financeUtilsPath = new URL('../src/utils/adminFinance.js', import.meta.url)
  const layoutText = await fs.readFile(layoutPath, 'utf8')
  const routerText = await fs.readFile(routerPath, 'utf8')
  const authSeedText = await fs.readFile(authSeedPath, 'utf8')
  const sqlSeedText = await fs.readFile(sqlSeedPath, 'utf8')
  const financePageText = await fs.readFile(financePagePath, 'utf8')
  const financeUtilsText = await fs.readFile(financeUtilsPath, 'utf8')

  const expectedModules = [
    ['dashboard', '/admin/finance', 'admin_finance_dashboard'],
    ['budget', '/admin/finance/budget', 'admin_finance_budget'],
    ['allocation', '/admin/finance/allocation', 'admin_finance_allocation'],
    ['reimbursement', '/admin/finance/reimbursement', 'admin_finance_reimbursement'],
    ['voucher', '/admin/finance/voucher', 'admin_finance_voucher'],
    ['ledger', '/admin/finance/ledger', 'admin_finance_ledger'],
    ['report', '/admin/finance/report', 'admin_finance_report'],
    ['audit', '/admin/finance/audit', 'admin_finance_audit']
  ]

  for (const [key, path, code] of expectedModules) {
    assert.match(layoutText, new RegExp(`path:\\s*'${path.replaceAll('/', '\\/')}'`), `layout missing ${path}`)
    assert.match(routerText, new RegExp(`path:\\s*'finance${path === '/admin/finance' ? '\'' : `\\/${path.split('/').pop()}'`}`), `router missing ${path}`)
    assert.match(authSeedText, new RegExp(`"${path.replaceAll('/', '\\/')}"`), `auth seed missing ${path}`)
    assert.match(authSeedText, new RegExp(`"${code}"`), `auth seed missing ${code}`)
    assert.match(sqlSeedText, new RegExp(`'${path.replaceAll('/', '\\/')}'`), `sql seed missing ${path}`)
    assert.match(sqlSeedText, new RegExp(`'${code}'`), `sql seed missing ${code}`)
    assert.match(financeUtilsText, new RegExp(`key:\\s*'${key}'`), `finance module missing ${key}`)
  }

  assert.match(financePageText, /useRoute\(\)/)
  assert.match(financePageText, /financeRouteTabMap/)
  assert.match(financePageText, /watch\(\s*\(\)\s*=>\s*route\.path/)
})

test('menu tree select popper should be constrained instead of expanding the page', async () => {
  const fs = await import('node:fs/promises')
  const menusPath = new URL('../src/views/admin/system/Menus.vue', import.meta.url)
  const rolesPath = new URL('../src/views/admin/system/Roles.vue', import.meta.url)
  const layoutPath = new URL('../src/layouts/AdminLayout.vue', import.meta.url)
  const stylesPath = new URL('../src/styles/index.scss', import.meta.url)
  const menusText = await fs.readFile(menusPath, 'utf8')
  const rolesText = await fs.readFile(rolesPath, 'utf8')
  const layoutText = await fs.readFile(layoutPath, 'utf8')
  const stylesText = await fs.readFile(stylesPath, 'utf8')

  assert.match(menusText, /popper-class="admin-menu-tree-select-popper"/)
  assert.match(menusText, /:teleported="true"/)
  assert.match(stylesText, /\.admin-menu-tree-select-popper/)
  assert.match(stylesText, /max-height:\s*min\(360px,\s*calc\(100vh - 96px\)\)/)
  assert.match(stylesText, /\.el-select__popper\s+\.el-select-dropdown__wrap/)
  assert.match(stylesText, /\.el-dropdown__popper\s+\.el-dropdown-menu/)
  assert.match(rolesText, /class="role-menu-tree-panel"/)
  assert.match(rolesText, /max-height:\s*min\(420px,\s*62vh\)/)
  assert.match(layoutText, /\.admin-layout\s*\{[\s\S]*overflow:\s*hidden;/)
  assert.match(layoutText, /\.sidebar\s*\{[\s\S]*overflow:\s*hidden;/)
  assert.match(layoutText, /\.sidebar-menu\s*\{[\s\S]*min-height:\s*0;[\s\S]*overflow-y:\s*auto;/)
})

test('admin sidebar nested active menu items should keep readable contrast', async () => {
  const fs = await import('node:fs/promises')
  const layoutPath = new URL('../src/layouts/AdminLayout.vue', import.meta.url)
  const layoutText = await fs.readFile(layoutPath, 'utf8')

  const transparentNestedRule = layoutText.indexOf('.sidebar-menu :deep(.el-sub-menu .el-menu-item),')
  const nestedActiveRule = layoutText.indexOf('.sidebar-menu :deep(.el-sub-menu .el-menu-item.is-active),')

  assert.notEqual(nestedActiveRule, -1, 'nested active sidebar rule is missing')
  assert.ok(
    nestedActiveRule > transparentNestedRule,
    'nested active sidebar rule must appear after transparent nested item overrides'
  )

  const nestedActiveMatch = layoutText.match(/\.sidebar-menu :deep\(\.el-sub-menu \.el-menu-item\.is-active\),\s*\.sidebar-menu :deep\(\.el-menu--inline \.el-menu-item\.is-active\)\s*\{(?<body>[^}]+)\}/)
  assert.ok(nestedActiveMatch?.groups?.body, 'nested active sidebar block is missing')

  const nestedActiveBlock = nestedActiveMatch.groups.body
  assert.match(nestedActiveBlock, /background:\s*linear-gradient/)
  assert.match(nestedActiveBlock, /color:\s*#fff\s*!important/)
})

test('admin function selection overlays should stay fixed within the viewport', async () => {
  const fs = await import('node:fs/promises')
  const rolesPath = new URL('../src/views/admin/system/Roles.vue', import.meta.url)
  const stylesPath = new URL('../src/styles/index.scss', import.meta.url)
  const rolesText = await fs.readFile(rolesPath, 'utf8')
  const stylesText = await fs.readFile(stylesPath, 'utf8')

  assert.match(rolesText, /class="admin-permission-dialog"/)
  assert.match(stylesText, /\.admin-permission-dialog\s+\.el-dialog__body/)
  assert.match(stylesText, /\.admin-bounded-popper/)
  assert.match(stylesText, /\.el-cascader__dropdown/)
  assert.match(stylesText, /\.el-picker__popper/)
  assert.match(stylesText, /max-height:\s*min\(360px,\s*calc\(100vh - 96px\)\)/)
  assert.match(stylesText, /overflow-y:\s*auto/)
})
