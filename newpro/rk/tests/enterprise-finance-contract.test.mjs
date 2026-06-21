import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'

const rootUrl = new URL('../../../', import.meta.url)

async function read(relativePath) {
  return fs.readFile(new URL(relativePath, rootUrl), 'utf8')
}

test('enterprise finance backend exposes dedicated tables and APIs', async () => {
  const schema = await read('rk-user/src/main/java/com/tianji/user/config/FinanceSchemaUpdater.java')
  const controller = await read('rk-user/src/main/java/com/tianji/user/controller/FinanceController.java')

  for (const table of [
    'finance_budget',
    'finance_budget_item',
    'finance_budget_ledger',
    'finance_allocation',
    'finance_reimbursement',
    'finance_reimbursement_item',
    'finance_voucher',
    'finance_voucher_entry',
    'finance_ledger',
    'finance_attachment',
    'finance_audit_log',
    'finance_period_close',
    'finance_reconciliation',
    'finance_subject',
    'finance_approval_action',
    'finance_approval_template',
    'finance_approval_template_node'
  ]) {
    assert.match(schema, new RegExp(`CREATE TABLE ${table}`), `schema updater should create ${table}`)
  }

  for (const path of [
    '/budgets',
    '/allocations',
    '/reimbursements',
    '/vouchers',
    '/ledger',
    '/attachments',
    '/audit-logs',
    '/enterprise/overview',
    '/periods',
    '/periods/close',
    '/periods/reopen',
    '/reconciliations',
    '/subjects',
    '/approval-templates',
    '/approval-templates/default',
    '/governance-approvals',
    '/reports/export'
  ]) {
    assert.match(controller, new RegExp(`"${path.replaceAll('/', '\\/')}"`), `controller missing ${path}`)
  }

  for (const path of [
    '/vouchers/{id}/post',
    '/vouchers/{id}/reverse',
    '/subjects/{id}',
    '/subjects/{id}/status',
    '/approval-templates/{id}/enable',
    '/periods/reopen/request',
    '/periods/reopen/{actionId}/approve',
    '/vouchers/{id}/reverse/request',
    '/vouchers/reverse/{actionId}/approve',
    '/reimbursements/pending-approvals',
    '/reimbursements/{id}/approve',
    '/reimbursements/{id}/reject',
    '/reimbursements/{id}/transfer',
    '/reimbursements/{id}/add-sign',
    '/reimbursements/{id}/withdraw'
  ]) {
    assert.match(controller, new RegExp(`"${path.replaceAll('/', '\\/').replaceAll('{', '\\{').replaceAll('}', '\\}')}"`), `controller missing ${path}`)
  }

  assert.match(controller, /insertFinanceAuditLog/)
  assert.match(controller, /insertFinanceLedgerEntry/)
  assert.match(controller, /resolveTenantId\(\)/)
  assert.match(controller, /queryPendingReimbursementApprovals/)
  assert.match(controller, /current_approver_id = \?/)
  assert.match(controller, /pendingApprovalReimbursements/)
  assert.match(controller, /pendingApprovalCount/)
  assert.match(controller, /pendingApprovalAmount/)
  assert.match(controller, /updateFinanceSubject/)
  assert.match(controller, /deleteFinanceSubject/)
  assert.match(controller, /is_deleted=1/)
  assert.match(controller, /resolveActiveApprovalTemplate/)
  assert.match(controller, /queryApprovalTemplateNodes/)
  assert.match(controller, /applyApprovalTemplateToReimbursement/)
  assert.match(controller, /advanceReimbursementApproval/)
  assert.match(controller, /approval_template_id/)
  assert.match(controller, /current_step_no/)
  assert.match(schema, /original_voucher_id/, 'voucher schema should keep the original voucher id on reversal rows')
  assert.match(schema, /reverse_reason/, 'voucher schema should persist the reversal reason')
  assert.match(controller, /validateSubjectParent/, 'subject create/update must validate parent hierarchy')
  assert.match(controller, /findSubjectByCode/, 'subject hierarchy validation should resolve parent subject rows')
  assert.match(controller, /parentCode\.equals\(subjectCode\)/, 'subject parent cannot point to itself')
  assert.match(controller, /凭证未过账，不能冲销/, 'reversal must reject unposted vouchers')
  assert.match(controller, /postingStatus != 1/, 'reversal must only accept posted vouchers')
  assert.match(controller, /original_voucher_id/, 'reversal voucher should store original_voucher_id')
  assert.match(controller, /reverse_reason/, 'reversal voucher should store reverse_reason')
  assert.match(controller, /insertReverseVoucherEntries/, 'reversal should create traceable opposite voucher entries')
  assert.match(controller, /listFinanceGovernanceApprovals/, 'high-risk finance approvals should be queryable')
  assert.match(controller, /requestReopenFinancePeriod/, 'period reopen should support approval request')
  assert.match(controller, /approveReopenFinancePeriod/, 'period reopen should execute after approval')
  assert.match(controller, /requestReverseFinanceVoucher/, 'voucher reversal should support approval request')
  assert.match(controller, /approveReverseFinanceVoucher/, 'voucher reversal should execute after approval')
  assert.match(controller, /REOPEN_REQUEST/, 'period reopen request should be persisted as approval action')
  assert.match(controller, /REVERSE_REQUEST/, 'voucher reversal request should be persisted as approval action')
  assert.match(controller, /findPendingGovernanceAction/, 'approval execution should validate pending governance action')

  for (const path of ['/budgets', '/allocations', '/reimbursements', '/vouchers']) {
    const createEndpointPattern = new RegExp(`@PostMapping\\("${path.replaceAll('/', '\\/')}"\\)[\\s\\S]{0,80}@Transactional`)
    assert.match(controller, createEndpointPattern, `${path} creates multiple finance rows and must be transactional`)
  }

  assert.doesNotMatch(
    controller,
    /keyHolder\.getKey\(\)/,
    'GeneratedKeyHolder#getKey can fail when JDBC returns multiple generated-key columns'
  )
  assert.match(controller, /keyHolder\.getKeyList\(\)/)
})

test('enterprise finance frontend consumes dedicated APIs instead of only finance_record', async () => {
  const api = await read('newpro/rk/src/api/finance.js')
  const page = await read('newpro/rk/src/views/admin/club/Finance.vue')
  const router = await read('newpro/rk/src/router/index.js')

  for (const fn of [
    'getFinanceEnterpriseOverview',
    'getFinanceBudgets',
    'createFinanceBudget',
    'getFinanceAllocations',
    'createFinanceAllocation',
    'getFinanceReimbursements',
    'createFinanceReimbursement',
    'getFinanceVouchers',
    'createFinanceVoucher',
    'postFinanceVoucher',
    'reverseFinanceVoucher',
    'requestReverseFinanceVoucher',
    'approveReverseFinanceVoucher',
    'getFinanceLedger',
    'getFinanceAuditLogs',
    'getFinancePeriods',
    'closeFinancePeriod',
    'reopenFinancePeriod',
    'requestReopenFinancePeriod',
    'approveReopenFinancePeriod',
    'getFinanceGovernanceApprovals',
    'getFinanceReconciliations',
    'createFinanceReconciliation',
    'getFinanceSubjects',
    'createFinanceSubject',
    'updateFinanceSubject',
    'deleteFinanceSubject',
    'updateFinanceSubjectStatus',
    'getFinanceApprovalTemplates',
    'createFinanceApprovalTemplate',
    'enableFinanceApprovalTemplate',
    'createDefaultFinanceApprovalTemplate',
    'getFinancePendingApprovalReimbursements',
    'approveFinanceReimbursement',
    'rejectFinanceReimbursement',
    'transferFinanceReimbursement',
    'addSignFinanceReimbursement',
    'withdrawFinanceReimbursement',
    'exportFinanceReport'
  ]) {
    assert.match(api, new RegExp(`export function ${fn}\\(`), `api missing ${fn}`)
    assert.match(page, new RegExp(fn), `finance page should use ${fn}`)
  }

  assert.match(page, /enterpriseFinance/)
  assert.match(page, /budgetRows/)
  assert.match(page, /allocationRows/)
  assert.match(page, /reimbursementRows/)
  assert.match(page, /dedicatedLedgerRows/)
  assert.match(page, /periodRows/)
  assert.match(page, /reconciliationRows/)
  assert.match(page, /handlePostVoucher/)
  assert.match(page, /handleReverseVoucher/)
  assert.match(page, /handleRequestReverseVoucher/)
  assert.match(page, /handleApproveReverseRequest/)
  assert.match(page, /handleClosePeriod/)
  assert.match(page, /handleReopenPeriod/)
  assert.match(page, /handleRequestReopenPeriod/)
  assert.match(page, /handleApproveReopenRequest/)
  assert.match(page, /governanceApprovalRows/)
  assert.match(page, /handleCreateReconciliation/)
  assert.match(page, /handleExportFinanceReport/)
  assert.match(page, /subjectRows/)
  assert.match(page, /approvalActionRows/)
  assert.match(page, /approvalTemplateRows/)
  assert.match(page, /approvalNodeRows/)
  assert.match(page, /subjectTreeRows/)
  assert.match(page, /subjectParentOptions/)
  assert.match(page, /parentSubjectName/)
  assert.match(page, /上级科目不存在/)
  assert.match(page, /原凭证/)
  assert.match(page, /冲销原因/)
  assert.match(page, /高风险审批/)
  assert.match(page, /handleCreateDefaultApprovalTemplate/)
  assert.match(page, /handleEnableApprovalTemplate/)
  assert.match(page, /审批模板/)
  assert.match(page, /handleEditFinanceSubject/)
  assert.match(page, /handleDeleteFinanceSubject/)
  assert.match(page, /reimbursementScope/)
  assert.match(page, /pendingApprovalReimbursementRows/)
  assert.match(page, /visibleReimbursementRows/)
  assert.match(page, /待我审批/)
  assert.match(page, /currentApproverId/)
  assert.match(page, /handleApproveReimbursement/)
  assert.match(page, /handleTransferReimbursement/)
  assert.match(page, /handleAddSignReimbursement/)
  assert.match(page, /handleWithdrawReimbursement/)
  assert.match(page, /getUserPage/)
  assert.match(page, /approverCandidates/)
  assert.match(page, /loadFinanceApproverCandidates/)
  assert.match(page, /approverDialogVisible/)
  assert.match(page, /nextApproverId/)
  assert.doesNotMatch(page, /请输入审批人ID/)

  for (const view of [
    'Dashboard',
    'Budget',
    'Allocation',
    'Reimbursement',
    'Voucher',
    'Ledger',
    'Report',
    'Audit'
  ]) {
    const viewSource = await read(`newpro/rk/src/views/admin/finance/${view}.vue`)
    assert.match(viewSource, /FinanceView/, `${view}.vue should be a dedicated route wrapper`)
    assert.match(router, new RegExp(`views/admin/finance/${view}\\.vue`), `router should load ${view}.vue`)
  }

  assert.doesNotMatch(
    router,
    /path: 'finance\/(?:budget|allocation|reimbursement|voucher|ledger|report|audit)'[\s\S]{0,160}views\/admin\/club\/Finance\.vue/,
    'finance child routes should no longer all import the old large club Finance.vue directly'
  )
})
