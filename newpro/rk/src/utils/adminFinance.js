export function toFinanceApiType(type) {
  if (type === 'income' || type === 1 || type === '1') {
    return 1
  }
  if (type === 'expense' || type === 2 || type === '2') {
    return 2
  }
  return null
}

export function fromFinanceApiType(type) {
  if (type === 1 || type === '1') {
    return 'income'
  }
  if (type === 2 || type === '2') {
    return 'expense'
  }
  if (type === 'income' || type === 'expense') {
    return type
  }
  return ''
}

export function normalizeFinanceRecord(record = {}) {
  return {
    ...record,
    type: fromFinanceApiType(record.type),
  }
}

function money(value) {
  const amount = Number(value)
  return Number.isFinite(amount) ? amount : 0
}

function round2(value) {
  return Math.round(value * 100) / 100
}

export const financeModuleDefinitions = [
  { key: 'dashboard', title: '财务工作台', subtitle: '余额、收支、预算执行和风险预警' },
  { key: 'budget', title: '全面预算', subtitle: '预算占用、执行率、超支预警' },
  { key: 'allocation', title: '经费拨款', subtitle: '高校拨款、经费入账、资金来源和对账' },
  { key: 'reimbursement', title: '报账费控', subtitle: '报销申请、票据凭证、审批闭环' },
  { key: 'voucher', title: '凭证中心', subtitle: '单据编号、凭证影像、入账状态' },
  { key: 'ledger', title: '总账核算', subtitle: '收支台账、期间核算、自动入账' },
  { key: 'report', title: '报表中心', subtitle: '收支报表、预算执行报表和监管导出' },
  { key: 'audit', title: '审计合规', subtitle: '操作轨迹、异常单据、合规检查和留痕' },
  { key: 'fund', title: '资金账户', subtitle: '余额、拨款、支付与对账' },
  { key: 'payable', title: '往来付款', subtitle: '应收应付、待付款、供应商往来' },
  { key: 'asset', title: '资产台账', subtitle: '设备物资、采购验收、折旧提示' },
  { key: 'tax', title: '合规税务', subtitle: '票据合规、风险扫描、审计留痕' },
  { key: 'archive', title: '电子档案', subtitle: '凭证影像、附件归档、长期留存' }
]

function countBy(records, predicate) {
  return records.filter(predicate).length
}

export function buildFinanceModuleCards({ account = {}, records = [] } = {}) {
  const normalizedRecords = records.map(normalizeFinanceRecord)
  const pendingRecords = normalizedRecords.filter((record) => Number(record.status) === 0)
  const approvedRecords = normalizedRecords.filter((record) => Number(record.status) === 1)
  const expenseRecords = normalizedRecords.filter((record) => toFinanceApiType(record.type) === 2)
  const incomeRecords = normalizedRecords.filter((record) => toFinanceApiType(record.type) === 1)
  const reimbursementRecords = normalizedRecords.filter((record) =>
    record.businessType === 'REIMBURSEMENT' || record.category === 'reimbursement'
  )
  const assetRecords = normalizedRecords.filter((record) =>
    record.businessType === 'ASSET' || record.category === 'supplies'
  )
  const payableRecords = normalizedRecords.filter((record) =>
    record.businessType === 'PAYMENT' || (toFinanceApiType(record.type) === 2 && Number(record.status) === 0)
  )
  const archivedRecords = normalizedRecords.filter((record) => record.proofImageUrl)

  const metrics = {
    dashboard: { count: normalizedRecords.length, amount: money(account.balance), status: '总览' },
    ledger: { count: normalizedRecords.length, amount: money(account.balance), status: '余额' },
    budget: { count: countBy(expenseRecords, () => true), amount: money(account.totalExpense), status: '支出执行' },
    allocation: { count: incomeRecords.length, amount: money(account.totalIncome), status: '拨款/收入' },
    reimbursement: { count: reimbursementRecords.length, amount: reimbursementRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '报销' },
    fund: { count: incomeRecords.length, amount: money(account.totalIncome), status: '累计入账' },
    voucher: { count: approvedRecords.length, amount: approvedRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '已入账' },
    payable: { count: payableRecords.length, amount: payableRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '待付款' },
    asset: { count: assetRecords.length, amount: assetRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '资产/物资' },
    tax: { count: pendingRecords.length, amount: pendingRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '待检查' },
    archive: { count: archivedRecords.length, amount: archivedRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '已归档' },
    report: { count: approvedRecords.length, amount: round2(money(account.totalIncome) - money(account.totalExpense)), status: '报表' },
    audit: { count: pendingRecords.length, amount: pendingRecords.reduce((sum, record) => sum + money(record.amount), 0), status: '审计' }
  }

  return financeModuleDefinitions.map((module) => ({
    ...module,
    count: metrics[module.key]?.count || 0,
    amount: round2(metrics[module.key]?.amount || 0),
    status: metrics[module.key]?.status || ''
  }))
}

export function buildFinanceVoucherRows(records = []) {
  return records
    .map(normalizeFinanceRecord)
    .filter((record) => Number(record.status) === 1)
    .map((record) => {
      const isIncome = toFinanceApiType(record.type) === 1
      return {
        id: record.id,
        voucherNo: `PZ-${record.recordNo || record.id}`,
        period: record.period || '',
        summary: record.title || record.description || '-',
        debitSubject: isIncome ? '银行存款' : (record.budgetItem || '业务支出'),
        creditSubject: isIncome ? (record.budgetItem || '收入') : '银行存款',
        amount: money(record.amount),
        proofImageUrl: record.proofImageUrl || '',
        postedStatus: record.postedStatus
      }
    })
}

export function buildFinanceAuditRows(records = []) {
  return records
    .map(normalizeFinanceRecord)
    .map((record) => ({
      id: record.id,
      time: record.reviewTime || record.updateTime || record.createTime || '',
      action: Number(record.status) === 1 ? '审核通过/入账' : Number(record.status) === 2 ? '审核驳回' : '提交待审',
      target: record.recordNo || record.title || `记录 ${record.id}`,
      operator: record.reviewerId || record.operatorId || '-',
      result: Number(record.status) === 2 ? (record.rejectReason || '已驳回') : '正常',
      risk: Number(record.status) === 0 && money(record.amount) > 1000 ? '大额待审' : '低'
    }))
}

export function buildFinanceDashboardSummary({ account = {}, records = [], budgets = [] } = {}) {
  const approvedRecords = records.filter((record) => Number(record.status) === 1)
  const income = approvedRecords
    .filter((record) => toFinanceApiType(record.type) === 1)
    .reduce((sum, record) => sum + money(record.amount), 0)
  const expense = approvedRecords
    .filter((record) => toFinanceApiType(record.type) === 2)
    .reduce((sum, record) => sum + money(record.amount), 0)
  const pendingAmount = records
    .filter((record) => Number(record.status) === 0)
    .reduce((sum, record) => sum + money(record.amount), 0)

  const totalBudget = budgets.length
    ? budgets.reduce((sum, item) => sum + money(item.amount ?? item.budgetAmount), 0)
    : money(account.totalIncome)
  const budgetUsageRate = totalBudget > 0 ? round2((money(account.totalExpense ?? expense) / totalBudget) * 100) : 0

  const categories = new Map()
  records.forEach((record) => {
    const category = record.category || 'other'
    if (!categories.has(category)) {
      categories.set(category, { category, income: 0, expense: 0, pending: 0 })
    }
    const item = categories.get(category)
    const amount = money(record.amount)
    if (Number(record.status) === 0) {
      item.pending += amount
    }
    if (toFinanceApiType(record.type) === 1) {
      item.income += amount
    }
    if (toFinanceApiType(record.type) === 2) {
      item.expense += amount
    }
  })

  const riskAlerts = []
  if (money(account.balance) < 0) {
    riskAlerts.push({ type: 'danger', title: '账户余额为负', message: '请检查支出审批和资金入账。' })
  }
  if (budgetUsageRate >= 80) {
    riskAlerts.push({ type: 'warning', title: '预算使用率偏高', message: `当前预算使用率 ${budgetUsageRate}%。` })
  }
  if (pendingAmount > 0) {
    riskAlerts.push({ type: 'info', title: '存在待审单据', message: `待审核金额 ${pendingAmount.toFixed(2)} 元。` })
  }

  return {
    balance: money(account.balance),
    totalIncome: money(account.totalIncome ?? income),
    totalExpense: money(account.totalExpense ?? expense),
    pendingAmount: round2(pendingAmount),
    budgetUsageRate,
    categoryBreakdown: Array.from(categories.values()).map((item) => ({
      ...item,
      income: round2(item.income),
      expense: round2(item.expense),
      pending: round2(item.pending)
    })),
    riskAlerts
  }
}
