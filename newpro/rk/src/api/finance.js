import request from '@/utils/request'

export function getFinanceAccount() {
  return request({ url: '/api/finance/account', method: 'get' })
}

export function getFinanceDashboard() {
  return request({ url: '/api/finance/dashboard', method: 'get' })
}

export function getFinanceRecords(params) {
  return request({ url: '/api/finance/records', method: 'get', params })
}

export function createFinanceRecord(data) {
  return request({ url: '/api/finance/records', method: 'post', data })
}

export function approveFinanceRecord(id) {
  return request({ url: `/api/finance/records/${id}/approve`, method: 'put' })
}

export function rejectFinanceRecord(id, reason) {
  return request({ url: `/api/finance/records/${id}/reject`, method: 'put', params: { reason } })
}

export function getFinanceReportSummary(params = {}) {
  return request({ url: '/api/finance/reports/summary', method: 'get', params })
}

export function getFinanceEnterpriseOverview() {
  return request({ url: '/api/finance/enterprise/overview', method: 'get' })
}

export function getFinanceBudgets() {
  return request({ url: '/api/finance/budgets', method: 'get' })
}

export function createFinanceBudget(data) {
  return request({ url: '/api/finance/budgets', method: 'post', data })
}

export function getFinanceAllocations() {
  return request({ url: '/api/finance/allocations', method: 'get' })
}

export function createFinanceAllocation(data) {
  return request({ url: '/api/finance/allocations', method: 'post', data })
}

export function getFinanceReimbursements() {
  return request({ url: '/api/finance/reimbursements', method: 'get' })
}

export function getFinancePendingApprovalReimbursements() {
  return request({ url: '/api/finance/reimbursements/pending-approvals', method: 'get' })
}

export function createFinanceReimbursement(data) {
  return request({ url: '/api/finance/reimbursements', method: 'post', data })
}

export function getFinanceVouchers() {
  return request({ url: '/api/finance/vouchers', method: 'get' })
}

export function createFinanceVoucher(data) {
  return request({ url: '/api/finance/vouchers', method: 'post', data })
}

export function postFinanceVoucher(id) {
  return request({ url: `/api/finance/vouchers/${id}/post`, method: 'put' })
}

export function reverseFinanceVoucher(id, data = {}) {
  return request({ url: `/api/finance/vouchers/${id}/reverse`, method: 'put', data })
}

export function requestReverseFinanceVoucher(id, data = {}) {
  return request({ url: `/api/finance/vouchers/${id}/reverse/request`, method: 'put', data })
}

export function approveReverseFinanceVoucher(actionId, data = {}) {
  return request({ url: `/api/finance/vouchers/reverse/${actionId}/approve`, method: 'put', data })
}

export function getFinanceLedger() {
  return request({ url: '/api/finance/ledger', method: 'get' })
}

export function getFinanceAuditLogs() {
  return request({ url: '/api/finance/audit-logs', method: 'get' })
}

export function getFinancePeriods() {
  return request({ url: '/api/finance/periods', method: 'get' })
}

export function closeFinancePeriod(data) {
  return request({ url: '/api/finance/periods/close', method: 'post', data })
}

export function reopenFinancePeriod(data) {
  return request({ url: '/api/finance/periods/reopen', method: 'post', data })
}

export function requestReopenFinancePeriod(data = {}) {
  return request({ url: '/api/finance/periods/reopen/request', method: 'post', data })
}

export function approveReopenFinancePeriod(actionId, data = {}) {
  return request({ url: `/api/finance/periods/reopen/${actionId}/approve`, method: 'post', data })
}

export function getFinanceGovernanceApprovals() {
  return request({ url: '/api/finance/governance-approvals', method: 'get' })
}

export function getFinanceReconciliations() {
  return request({ url: '/api/finance/reconciliations', method: 'get' })
}

export function createFinanceReconciliation(data) {
  return request({ url: '/api/finance/reconciliations', method: 'post', data })
}

export function getFinanceSubjects() {
  return request({ url: '/api/finance/subjects', method: 'get' })
}

export function createFinanceSubject(data) {
  return request({ url: '/api/finance/subjects', method: 'post', data })
}

export function updateFinanceSubject(id, data) {
  return request({ url: `/api/finance/subjects/${id}`, method: 'put', data })
}

export function deleteFinanceSubject(id) {
  return request({ url: `/api/finance/subjects/${id}`, method: 'delete' })
}

export function updateFinanceSubjectStatus(id, data) {
  return request({ url: `/api/finance/subjects/${id}/status`, method: 'put', data })
}

export function getFinanceApprovalTemplates() {
  return request({ url: '/api/finance/approval-templates', method: 'get' })
}

export function createFinanceApprovalTemplate(data) {
  return request({ url: '/api/finance/approval-templates', method: 'post', data })
}

export function enableFinanceApprovalTemplate(id) {
  return request({ url: `/api/finance/approval-templates/${id}/enable`, method: 'put' })
}

export function createDefaultFinanceApprovalTemplate(data = {}) {
  return request({ url: '/api/finance/approval-templates/default', method: 'post', data })
}

export function approveFinanceReimbursement(id, data = {}) {
  return request({ url: `/api/finance/reimbursements/${id}/approve`, method: 'put', data })
}

export function rejectFinanceReimbursement(id, data = {}) {
  return request({ url: `/api/finance/reimbursements/${id}/reject`, method: 'put', data })
}

export function transferFinanceReimbursement(id, data = {}) {
  return request({ url: `/api/finance/reimbursements/${id}/transfer`, method: 'put', data })
}

export function addSignFinanceReimbursement(id, data = {}) {
  return request({ url: `/api/finance/reimbursements/${id}/add-sign`, method: 'put', data })
}

export function withdrawFinanceReimbursement(id, data = {}) {
  return request({ url: `/api/finance/reimbursements/${id}/withdraw`, method: 'put', data })
}

export function exportFinanceReport(params = {}) {
  return request({ url: '/api/finance/reports/export', method: 'get', params, responseType: 'blob' })
}
