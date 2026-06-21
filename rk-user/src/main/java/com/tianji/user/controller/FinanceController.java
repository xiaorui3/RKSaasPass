package com.tianji.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.R;
import com.tianji.common.utils.TenantContext;
import com.tianji.common.utils.UserContext;
import com.tianji.user.domain.po.FinanceAccount;
import com.tianji.user.domain.po.FinanceRecord;
import com.tianji.user.mapper.FinanceAccountMapper;
import com.tianji.user.mapper.FinanceRecordMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "财务管理接口")
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceAccountMapper financeAccountMapper;
    private final FinanceRecordMapper financeRecordMapper;
    private final JdbcTemplate jdbcTemplate;

    @ApiOperation("获取财务账户")
    @GetMapping("/account")
    public R<FinanceAccount> getAccount() {
        return R.ok(findOrCreateAccount(resolveTenantId()));
    }

    @ApiOperation("获取财务驾驶舱")
    @GetMapping("/dashboard")
    public R<Map<String, Object>> getDashboard() {
        Long tenantId = resolveTenantId();
        FinanceAccount account = findOrCreateAccount(tenantId);
        List<FinanceRecord> records = financeRecordMapper.selectList(new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getTenantId, tenantId)
                .orderByDesc(FinanceRecord::getCreateTime)
                .last("LIMIT 200"));

        BigDecimal pendingAmount = sum(records, null, 0);
        BigDecimal approvedIncome = sum(records, 1, 1);
        BigDecimal approvedExpense = sum(records, 2, 1);
        BigDecimal totalBudget = safe(account.getTotalIncome()).compareTo(BigDecimal.ZERO) > 0
                ? safe(account.getTotalIncome())
                : approvedIncome;
        BigDecimal usageRate = totalBudget.compareTo(BigDecimal.ZERO) > 0
                ? safe(account.getTotalExpense()).multiply(BigDecimal.valueOf(100)).divide(totalBudget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("account", account);
        data.put("pendingAmount", pendingAmount);
        data.put("approvedIncome", approvedIncome);
        data.put("approvedExpense", approvedExpense);
        data.put("budgetUsageRate", usageRate);
        data.put("categoryBreakdown", buildCategoryBreakdown(records));
        data.put("monthlyTrend", buildMonthlyTrend(records));
        data.put("riskAlerts", buildRiskAlerts(account, pendingAmount, usageRate));
        data.put("recentRecords", records.size() > 10 ? records.subList(0, 10) : records);
        return R.ok(data);
    }

    @ApiOperation("查询财务记录")
    @GetMapping("/records")
    public R<Page<FinanceRecord>> listRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String keyword) {
        Long tenantId = resolveTenantId();
        Page<FinanceRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getTenantId, tenantId);
        if (type != null) {
            wrapper.eq(FinanceRecord::getType, type);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq(FinanceRecord::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(FinanceRecord::getStatus, status);
        }
        if (StringUtils.hasText(businessType)) {
            wrapper.eq(FinanceRecord::getBusinessType, businessType);
        }
        if (StringUtils.hasText(period)) {
            wrapper.eq(FinanceRecord::getPeriod, period);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FinanceRecord::getTitle, keyword)
                    .or().like(FinanceRecord::getDescription, keyword)
                    .or().like(FinanceRecord::getRecordNo, keyword));
        }
        wrapper.orderByDesc(FinanceRecord::getCreateTime);
        return R.ok(financeRecordMapper.selectPage(pageParam, wrapper));
    }

    @ApiOperation("创建财务记录")
    @PostMapping("/records")
    public R<String> createRecord(@RequestBody FinanceRecord record) {
        record.setTenantId(resolveTenantId());
        record.setStatus(0);
        record.setPostedStatus(0);
        record.setRecordNo(StringUtils.hasText(record.getRecordNo()) ? record.getRecordNo() : buildRecordNo(record));
        record.setBusinessType(StringUtils.hasText(record.getBusinessType()) ? record.getBusinessType() : defaultBusinessType(record));
        record.setPeriod(StringUtils.hasText(record.getPeriod()) ? record.getPeriod() : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        record.setOperatorId(UserContext.getUser());
        financeRecordMapper.insert(record);
        return R.ok("创建成功");
    }

    @ApiOperation("审核通过财务记录")
    @PutMapping("/records/{id}/approve")
    @Transactional
    public R<String> approveRecord(@PathVariable Long id) {
        FinanceRecord record = findRecord(id);
        if (record == null) {
            return R.error("记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() != 0) {
            return R.error("该记录已审核，不能重复入账");
        }
        if (record.getPostedStatus() != null && record.getPostedStatus() == 1) {
            return R.error("该记录已入账，不能重复入账");
        }
        record.setStatus(1);
        record.setPostedStatus(1);
        record.setReviewerId(UserContext.getUser());
        record.setReviewTime(LocalDateTime.now());
        financeRecordMapper.updateById(record);

        FinanceAccount account = findOrCreateAccount(resolveTenantId());
        BigDecimal amount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;
        if (record.getType() != null && record.getType() == 1) {
            account.setBalance(safe(account.getBalance()).add(amount));
            account.setTotalIncome(safe(account.getTotalIncome()).add(amount));
        } else if (record.getType() != null && record.getType() == 2) {
            account.setBalance(safe(account.getBalance()).subtract(amount));
            account.setTotalExpense(safe(account.getTotalExpense()).add(amount));
        }
        financeAccountMapper.updateById(account);
        return R.ok("审核通过");
    }

    @ApiOperation("审核拒绝财务记录")
    @PutMapping("/records/{id}/reject")
    public R<String> rejectRecord(@PathVariable Long id,
                                  @RequestParam(required = false) String reason) {
        FinanceRecord record = findRecord(id);
        if (record == null) {
            return R.error("记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() != 0) {
            return R.error("该记录已审核，不能重复驳回");
        }
        if (record.getPostedStatus() != null && record.getPostedStatus() == 1) {
            return R.error("该记录已入账，不能驳回");
        }
        record.setStatus(2);
        record.setRejectReason(reason);
        record.setReviewerId(UserContext.getUser());
        record.setReviewTime(LocalDateTime.now());
        financeRecordMapper.updateById(record);
        return R.ok("已拒绝");
    }

    @ApiOperation("生成财务报表")
    @GetMapping("/reports/summary")
    public R<Map<String, Object>> getReportSummary(@RequestParam(required = false) String period) {
        Long tenantId = resolveTenantId();
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getTenantId, tenantId);
        if (StringUtils.hasText(period)) {
            wrapper.eq(FinanceRecord::getPeriod, period);
        }
        List<FinanceRecord> records = financeRecordMapper.selectList(wrapper);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("period", StringUtils.hasText(period) ? period : "ALL");
        report.put("income", sum(records, 1, 1));
        report.put("expense", sum(records, 2, 1));
        report.put("pending", sum(records, null, 0));
        report.put("categoryBreakdown", buildCategoryBreakdown(records));
        report.put("monthlyTrend", buildMonthlyTrend(records));
        return R.ok(report);
    }

    @ApiOperation("获取企业级财务总览")
    @GetMapping("/enterprise/overview")
    public R<Map<String, Object>> getEnterpriseOverview() {
        Long tenantId = resolveTenantId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("budgets", queryTenantRows("finance_budget", tenantId, 20));
        data.put("allocations", queryTenantRows("finance_allocation", tenantId, 20));
        data.put("reimbursements", queryTenantRows("finance_reimbursement", tenantId, 20));
        data.put("vouchers", queryTenantRows("finance_voucher", tenantId, 20));
        data.put("ledger", queryTenantRows("finance_ledger", tenantId, 30));
        data.put("attachments", queryTenantRows("finance_attachment", tenantId, 20));
        data.put("auditLogs", queryTenantRows("finance_audit_log", tenantId, 30));
        data.put("periods", queryTenantRows("finance_period_close", tenantId, 20));
        data.put("reconciliations", queryTenantRows("finance_reconciliation", tenantId, 20));
        data.put("subjects", listOrSeedSubjects(tenantId));
        data.put("approvalActions", queryTenantRows("finance_approval_action", tenantId, 30));
        data.put("governanceApprovals", listFinanceGovernanceApprovalsData(tenantId, 50));
        data.put("approvalTemplates", listApprovalTemplatesWithNodes(tenantId));
        data.put("approvalTemplateNodes", queryTenantRows("finance_approval_template_node", tenantId, 100));
        List<Map<String, Object>> pendingApprovalReimbursements = queryPendingReimbursementApprovals(tenantId, UserContext.getUser(), 20);
        data.put("pendingApprovalReimbursements", pendingApprovalReimbursements);
        data.put("pendingApprovalCount", pendingApprovalReimbursements.size());
        data.put("pendingApprovalAmount", sumMoney(pendingApprovalReimbursements, "amount"));
        data.put("budgetTotal", sumColumn("finance_budget", "total_amount", tenantId));
        data.put("budgetUsed", sumColumn("finance_budget", "used_amount", tenantId));
        data.put("pendingReimbursement", sumColumnWhere("finance_reimbursement", "amount", tenantId, "status = 0"));
        data.put("allocationTotal", sumColumn("finance_allocation", "amount", tenantId));
        return R.ok(data);
    }

    @ApiOperation("查询财务高风险审批")
    @GetMapping("/governance-approvals")
    public R<List<Map<String, Object>>> listFinanceGovernanceApprovals() {
        return R.ok(listFinanceGovernanceApprovalsData(resolveTenantId(), 100));
    }

    @ApiOperation("查询预算")
    @GetMapping("/budgets")
    public R<List<Map<String, Object>>> listBudgets() {
        return R.ok(queryTenantRows("finance_budget", resolveTenantId(), 100));
    }

    @ApiOperation("创建预算")
    @PostMapping("/budgets")
    @Transactional
    public R<Map<String, Object>> createBudget(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        BigDecimal totalAmount = money(body.get("totalAmount"));
        BigDecimal usedAmount = money(body.get("usedAmount"));
        BigDecimal occupiedAmount = money(body.get("occupiedAmount"));
        BigDecimal availableAmount = totalAmount.subtract(usedAmount).subtract(occupiedAmount);
        Long id = insertWithKey(
                "INSERT INTO finance_budget (tenant_id,budget_no,budget_name,budget_type,period,total_amount,occupied_amount,used_amount,available_amount,status,owner_name,remark) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("YS"),
                text(body.get("budgetName"), "未命名预算"),
                text(body.get("budgetType"), "ANNUAL"),
                text(body.get("period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))),
                totalAmount,
                occupiedAmount,
                usedAmount,
                availableAmount.max(BigDecimal.ZERO),
                number(body.get("status"), 1),
                text(body.get("ownerName"), ""),
                text(body.get("remark"), "")
        );
        insertFinanceAuditLog("BUDGET", id, "CREATE", null, body, "创建预算", "LOW", "预算已创建");
        insertFinanceLedgerEntry("BUDGET", "YS-" + id, "BUDGET", id, BigDecimal.ZERO, totalAmount, text(body.get("period"), ""), "预算额度登记");
        return R.ok(Map.of("id", id));
    }

    @ApiOperation("查询拨款")
    @GetMapping("/allocations")
    public R<List<Map<String, Object>>> listAllocations() {
        return R.ok(queryTenantRows("finance_allocation", resolveTenantId(), 100));
    }

    @ApiOperation("创建拨款")
    @PostMapping("/allocations")
    @Transactional
    public R<Map<String, Object>> createAllocation(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        BigDecimal amount = money(body.get("amount"));
        Integer arrivalStatus = number(body.get("arrivalStatus"), 0);
        Long id = insertWithKey(
                "INSERT INTO finance_allocation (tenant_id,allocation_no,source_type,source_name,receiver_tenant_id,budget_id,amount,arrival_status,arrival_time,status,remark,operator_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("BK"),
                text(body.get("sourceType"), "SCHOOL_GRANT"),
                text(body.get("sourceName"), "学校经费"),
                longValue(body.get("receiverTenantId"), tenantId),
                longValue(body.get("budgetId"), null),
                amount,
                arrivalStatus,
                arrivalStatus == 1 ? LocalDateTime.now() : null,
                number(body.get("status"), arrivalStatus == 1 ? 1 : 0),
                text(body.get("remark"), ""),
                UserContext.getUser()
        );
        insertFinanceAuditLog("ALLOCATION", id, "CREATE", null, body, "创建拨款", amount.compareTo(BigDecimal.valueOf(10000)) >= 0 ? "HIGH" : "LOW", "拨款单已创建");
        insertFinanceLedgerEntry("ALLOCATION", "BK-" + id, "ALLOCATION", id, amount, BigDecimal.ZERO, text(body.get("period"), ""), "经费拨款登记");
        return R.ok(Map.of("id", id));
    }

    @ApiOperation("查询报销")
    @GetMapping("/reimbursements")
    public R<List<Map<String, Object>>> listReimbursements() {
        return R.ok(queryTenantRows("finance_reimbursement", resolveTenantId(), 100));
    }

    @ApiOperation("查询待我审批的报销")
    @GetMapping("/reimbursements/pending-approvals")
    public R<List<Map<String, Object>>> listPendingApprovalReimbursements() {
        return R.ok(queryPendingReimbursementApprovals(resolveTenantId(), UserContext.getUser(), 100));
    }

    @ApiOperation("报销审批通过")
    @PutMapping("/reimbursements/{id}/approve")
    @Transactional
    public R<Map<String, Object>> approveFinanceReimbursement(@PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> reimbursement = findTenantRow("finance_reimbursement", tenantId, id);
        if (reimbursement == null) {
            return R.error("报销单不存在");
        }
        Integer currentStatus = number(first(reimbursement, "status"), 0);
        if (currentStatus == 1) {
            return R.ok(approvalResult(id, 1, first(reimbursement, "current_approver_id", "currentApproverId"), "报销单已审批通过"));
        }
        BigDecimal amount = money(first(reimbursement, "amount"));
        Map<String, Object> advanced = advanceReimbursementApproval(tenantId, id, reimbursement, amount, body);
        if (advanced != null) {
            return R.ok(advanced);
        }
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 1, current_approver_id = NULL, current_step_no = 0, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                UserContext.getUser(),
                tenantId,
                id
        );
        insertFinanceLedgerEntry("REIMBURSEMENT_APPROVED", text(first(reimbursement, "reimbursement_no", "reimbursementNo"), "BX-" + id),
                "REIMBURSEMENT", id, BigDecimal.ZERO, amount, "", "报销审批通过");
        insertApprovalAction("REIMBURSEMENT", id, "APPROVE", first(reimbursement, "current_approver_id", "currentApproverId"), null,
                text(body == null ? null : body.get("opinion"), "审批通过"), 1);
        insertFinanceAuditLog("REIMBURSEMENT", id, "APPROVE", reimbursement, approvalResult(id, 1, null, "审批通过"),
                "报销审批通过", amount.compareTo(BigDecimal.valueOf(3000)) >= 0 ? "HIGH" : "LOW", "审批通过");
        return R.ok(approvalResult(id, 1, null, "审批通过"));
    }

    @ApiOperation("报销审批驳回")
    @PutMapping("/reimbursements/{id}/reject")
    @Transactional
    public R<Map<String, Object>> rejectFinanceReimbursement(@PathVariable Long id,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> reimbursement = findTenantRow("finance_reimbursement", tenantId, id);
        if (reimbursement == null) {
            return R.error("报销单不存在");
        }
        String reason = text(body == null ? null : first(body, "reason", "opinion", "remark"), "审批驳回");
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 2, reject_reason = ?, current_approver_id = NULL, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                reason,
                UserContext.getUser(),
                tenantId,
                id
        );
        insertApprovalAction("REIMBURSEMENT", id, "REJECT", first(reimbursement, "current_approver_id", "currentApproverId"), null, reason, 2);
        insertFinanceAuditLog("REIMBURSEMENT", id, "REJECT", reimbursement, approvalResult(id, 2, null, reason),
                "报销审批驳回", "MEDIUM", reason);
        return R.ok(approvalResult(id, 2, null, reason));
    }

    @ApiOperation("报销审批转交")
    @PutMapping("/reimbursements/{id}/transfer")
    @Transactional
    public R<Map<String, Object>> transferFinanceReimbursement(@PathVariable Long id,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> reimbursement = findTenantRow("finance_reimbursement", tenantId, id);
        if (reimbursement == null) {
            return R.error("报销单不存在");
        }
        Long nextApproverId = longValue(body == null ? null : body.get("nextApproverId"), null);
        if (nextApproverId == null) {
            return R.error("请选择转交审批人");
        }
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 0, current_approver_id = ?, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                nextApproverId,
                UserContext.getUser(),
                tenantId,
                id
        );
        String opinion = text(body == null ? null : body.get("opinion"), "审批转交");
        insertApprovalAction("REIMBURSEMENT", id, "TRANSFER", first(reimbursement, "current_approver_id", "currentApproverId"), nextApproverId, opinion, 0);
        insertFinanceAuditLog("REIMBURSEMENT", id, "TRANSFER", reimbursement, approvalResult(id, 0, nextApproverId, opinion),
                "报销审批转交", "LOW", opinion);
        return R.ok(approvalResult(id, 0, nextApproverId, opinion));
    }

    @ApiOperation("报销审批加签")
    @PutMapping("/reimbursements/{id}/add-sign")
    @Transactional
    public R<Map<String, Object>> addSignFinanceReimbursement(@PathVariable Long id,
                                                              @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> reimbursement = findTenantRow("finance_reimbursement", tenantId, id);
        if (reimbursement == null) {
            return R.error("报销单不存在");
        }
        Long nextApproverId = longValue(body == null ? null : body.get("nextApproverId"), null);
        if (nextApproverId == null) {
            return R.error("请选择加签审批人");
        }
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 0, current_approver_id = ?, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                nextApproverId,
                UserContext.getUser(),
                tenantId,
                id
        );
        String opinion = text(body == null ? null : body.get("opinion"), "审批加签");
        insertApprovalAction("REIMBURSEMENT", id, "ADD_SIGN", first(reimbursement, "current_approver_id", "currentApproverId"), nextApproverId, opinion, 0);
        insertFinanceAuditLog("REIMBURSEMENT", id, "ADD_SIGN", reimbursement, approvalResult(id, 0, nextApproverId, opinion),
                "报销审批加签", "LOW", opinion);
        return R.ok(approvalResult(id, 0, nextApproverId, opinion));
    }

    @ApiOperation("报销审批撤回")
    @PutMapping("/reimbursements/{id}/withdraw")
    @Transactional
    public R<Map<String, Object>> withdrawFinanceReimbursement(@PathVariable Long id,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> reimbursement = findTenantRow("finance_reimbursement", tenantId, id);
        if (reimbursement == null) {
            return R.error("报销单不存在");
        }
        String reason = text(body == null ? null : first(body, "reason", "opinion", "remark"), "申请人撤回");
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 3, current_approver_id = NULL, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                UserContext.getUser(),
                tenantId,
                id
        );
        insertApprovalAction("REIMBURSEMENT", id, "WITHDRAW", first(reimbursement, "current_approver_id", "currentApproverId"), null, reason, 3);
        insertFinanceAuditLog("REIMBURSEMENT", id, "WITHDRAW", reimbursement, approvalResult(id, 3, null, reason),
                "报销审批撤回", "MEDIUM", reason);
        return R.ok(approvalResult(id, 3, null, reason));
    }

    @ApiOperation("创建报销")
    @PostMapping("/reimbursements")
    @Transactional
    public R<Map<String, Object>> createReimbursement(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        BigDecimal amount = money(body.get("amount"));
        Long id = insertWithKey(
                "INSERT INTO finance_reimbursement (tenant_id,reimbursement_no,applicant_id,applicant_name,linked_activity_id,expense_subject,budget_id,amount,invoice_count,status,current_approver_id,approval_template_id,current_step_no,remark,operator_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("BX"),
                longValue(body.get("applicantId"), UserContext.getUser()),
                text(body.get("applicantName"), ""),
                longValue(body.get("linkedActivityId"), null),
                text(body.get("expenseSubject"), "活动经费"),
                longValue(body.get("budgetId"), null),
                amount,
                number(body.get("invoiceCount"), 0),
                number(body.get("status"), 0),
                longValue(body.get("currentApproverId"), null),
                null,
                0,
                text(body.get("remark"), ""),
                UserContext.getUser()
        );
        Map<String, Object> approvalState = applyApprovalTemplateToReimbursement(tenantId, id, amount, body);
        insertFinanceAuditLog("REIMBURSEMENT", id, "SUBMIT", null, body, "提交报销", amount.compareTo(BigDecimal.valueOf(3000)) >= 0 ? "HIGH" : "LOW", "报销单已提交");
        insertFinanceLedgerEntry("REIMBURSEMENT", "BX-" + id, "REIMBURSEMENT", id, BigDecimal.ZERO, amount, text(body.get("period"), ""), "报销占用登记");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.putAll(approvalState);
        return R.ok(result);
    }

    @ApiOperation("查询凭证")
    @GetMapping("/vouchers")
    public R<List<Map<String, Object>>> listVouchers() {
        return R.ok(queryTenantRows("finance_voucher", resolveTenantId(), 100));
    }

    @ApiOperation("创建凭证")
    @PostMapping("/vouchers")
    @Transactional
    public R<Map<String, Object>> createVoucher(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        BigDecimal amount = money(body.get("amount"));
        Long id = insertWithKey(
                "INSERT INTO finance_voucher (tenant_id,voucher_no,period,source_type,source_id,summary,amount,posting_status,operator_id) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("PZ"),
                text(body.get("period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))),
                text(body.get("sourceType"), "MANUAL"),
                longValue(body.get("sourceId"), null),
                text(body.get("summary"), "手工凭证"),
                amount,
                number(body.get("postingStatus"), 0),
                UserContext.getUser()
        );
        insertFinanceAuditLog("VOUCHER", id, "CREATE", null, body, "创建凭证", "LOW", "凭证已创建");
        insertFinanceLedgerEntry("VOUCHER", "PZ-" + id, "VOUCHER", id, amount, amount, text(body.get("period"), ""), "凭证分录登记");
        return R.ok(Map.of("id", id));
    }

    @ApiOperation("凭证过账")
    @PutMapping("/vouchers/{id}/post")
    @Transactional
    public R<Map<String, Object>> postFinanceVoucher(@PathVariable Long id) {
        Long tenantId = resolveTenantId();
        Map<String, Object> voucher = findTenantRow("finance_voucher", tenantId, id);
        if (voucher == null) {
            return R.error("凭证不存在");
        }
        String period = text(first(voucher, "period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        if (isPeriodClosed(tenantId, period)) {
            return R.error("该会计期间已结账，不能过账");
        }
        Integer postingStatus = number(first(voucher, "posting_status", "postingStatus"), 0);
        if (postingStatus == 1) {
            return R.ok(Map.of("id", id, "postingStatus", 1, "message", "凭证已过账"));
        }
        String voucherNo = text(first(voucher, "voucher_no", "voucherNo"), "PZ-" + id);
        BigDecimal amount = money(first(voucher, "amount"));
        String summary = text(first(voucher, "summary"), "凭证过账");
        jdbcTemplate.update(
                "UPDATE finance_voucher SET posting_status = 1, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                UserContext.getUser(),
                tenantId,
                id
        );
        insertFinanceLedgerEntry("POSTED_VOUCHER", voucherNo, "VOUCHER", id, amount, amount, period, "凭证过账：" + summary);
        insertFinanceAuditLog("VOUCHER", id, "POST", voucher, Map.of("postingStatus", 1), "凭证过账", "LOW", "过账成功");
        return R.ok(Map.of("id", id, "postingStatus", 1));
    }

    @ApiOperation("凭证冲销")
    @PutMapping("/vouchers/{id}/reverse")
    @Transactional
    public R<Map<String, Object>> reverseFinanceVoucher(@PathVariable Long id,
                                                        @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> voucher = findTenantRow("finance_voucher", tenantId, id);
        if (voucher == null) {
            return R.error("凭证不存在");
        }
        Long existingReverseId = longValue(first(voucher, "reverse_voucher_id", "reverseVoucherId"), null);
        if (existingReverseId != null && existingReverseId > 0) {
            return R.ok(Map.of("id", id, "reverseVoucherId", existingReverseId, "message", "凭证已冲销"));
        }
        String period = text(first(voucher, "period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        if (isPeriodClosed(tenantId, period)) {
            return R.error("该会计期间已结账，不能冲销");
        }
        Integer postingStatus = number(first(voucher, "posting_status", "postingStatus"), 0);
        if (postingStatus != 1) {
            return R.error("凭证未过账，不能冲销");
        }
        String voucherNo = text(first(voucher, "voucher_no", "voucherNo"), "PZ-" + id);
        BigDecimal amount = money(first(voucher, "amount")).negate();
        String reason = text(body == null ? null : body.get("remark"), "凭证冲销");
        Long reverseId = insertWithKey(
                "INSERT INTO finance_voucher (tenant_id,voucher_no,period,source_type,source_id,summary,amount,posting_status,reverse_voucher_id,original_voucher_id,reverse_reason,operator_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("CX"),
                period,
                "REVERSAL",
                id,
                "冲销 " + voucherNo + "：" + reason,
                amount,
                1,
                null,
                id,
                reason,
                UserContext.getUser()
        );
        jdbcTemplate.update(
                "UPDATE finance_voucher SET posting_status = 2, reverse_voucher_id = ?, reverse_reason = ?, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                reverseId,
                reason,
                UserContext.getUser(),
                tenantId,
                id
        );
        insertReverseVoucherEntries(tenantId, id, reverseId, reason);
        insertFinanceLedgerEntry("REVERSE_VOUCHER", "CX-" + reverseId, "VOUCHER_REVERSAL", reverseId, amount, amount, period, "冲销凭证：" + voucherNo);
        insertFinanceAuditLog("VOUCHER", id, "REVERSE", voucher, Map.of("reverseVoucherId", reverseId), reason, "MEDIUM", "冲销成功");
        return R.ok(Map.of("id", id, "reverseVoucherId", reverseId, "postingStatus", 2));
    }

    @ApiOperation("Request voucher reversal approval")
    @PutMapping("/vouchers/{id}/reverse/request")
    @Transactional
    public R<Map<String, Object>> requestReverseFinanceVoucher(@PathVariable Long id,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> voucher = findTenantRow("finance_voucher", tenantId, id);
        if (voucher == null) {
            return R.error("voucher not found");
        }
        Long existingReverseId = longValue(first(voucher, "reverse_voucher_id", "reverseVoucherId"), null);
        if (existingReverseId != null && existingReverseId > 0) {
            return R.error("voucher already reversed");
        }
        String period = text(first(voucher, "period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        if (isPeriodClosed(tenantId, period)) {
            return R.error("period closed, cannot reverse");
        }
        Integer postingStatus = number(first(voucher, "posting_status", "postingStatus"), 0);
        if (postingStatus != 1) {
            return R.error("鍑瘉鏈繃璐︼紝涓嶈兘鍐查攢");
        }
        Map<String, Object> pending = findPendingGovernanceAction(tenantId, null, "VOUCHER_REVERSE", id, "REVERSE_REQUEST");
        if (pending != null) {
            Long pendingId = longValue(first(pending, "id"), 0L);
            return R.ok(governanceApprovalResult(pendingId, id, "VOUCHER_REVERSE", "REVERSE_REQUEST", 0, "pending request exists"));
        }
        String reason = text(body == null ? null : first(body, "reason", "remark", "opinion"), "voucher reversal request");
        Long approverId = longValue(body == null ? null : first(body, "approverId", "toApproverId"), UserContext.getUser());
        Long actionId = insertApprovalActionWithKey("VOUCHER_REVERSE", id, "REVERSE_REQUEST", UserContext.getUser(), approverId, reason, 0);
        insertFinanceAuditLog("VOUCHER", id, "REVERSE_REQUEST", voucher, Map.of("actionId", actionId), reason, "HIGH", "pending approval");
        return R.ok(governanceApprovalResult(actionId, id, "VOUCHER_REVERSE", "REVERSE_REQUEST", 0, "request submitted"));
    }

    @ApiOperation("Approve voucher reversal")
    @PutMapping("/vouchers/reverse/{actionId}/approve")
    @Transactional
    public R<Map<String, Object>> approveReverseFinanceVoucher(@PathVariable Long actionId,
                                                               @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> action = findPendingGovernanceAction(tenantId, actionId, "VOUCHER_REVERSE", null, "REVERSE_REQUEST");
        if (action == null) {
            return R.error("pending reversal request not found");
        }
        Long voucherId = longValue(first(action, "business_id", "businessId"), null);
        if (voucherId == null) {
            return R.error("request business id missing");
        }
        Map<String, Object> executeBody = new LinkedHashMap<>();
        executeBody.put("remark", text(body == null ? null : first(body, "reason", "remark", "opinion"), text(first(action, "opinion"), "approval passed")));
        R<Map<String, Object>> executed = reverseFinanceVoucher(voucherId, executeBody);
        if (executed.getCode() == null || executed.getCode() != 200) {
            return executed;
        }
        String opinion = text(body == null ? null : first(body, "opinion", "remark"), "approval passed");
        markGovernanceActionApproved(tenantId, actionId, opinion);
        insertApprovalAction("VOUCHER_REVERSE", voucherId, "REVERSE_APPROVE", first(action, "from_approver_id", "fromApproverId"), UserContext.getUser(), opinion, 1);
        Map<String, Object> result = new LinkedHashMap<>(executed.getData());
        result.put("actionId", actionId);
        result.put("approvalStatus", 1);
        return R.ok(result);
    }

    @ApiOperation("查询总账")
    @GetMapping("/ledger")
    public R<List<Map<String, Object>>> listLedger() {
        return R.ok(queryTenantRows("finance_ledger", resolveTenantId(), 200));
    }

    @ApiOperation("查询附件")
    @GetMapping("/attachments")
    public R<List<Map<String, Object>>> listAttachments() {
        return R.ok(queryTenantRows("finance_attachment", resolveTenantId(), 100));
    }

    @ApiOperation("查询审计日志")
    @GetMapping("/audit-logs")
    public R<List<Map<String, Object>>> listAuditLogs() {
        return R.ok(queryTenantRows("finance_audit_log", resolveTenantId(), 200));
    }

    @ApiOperation("查询会计科目")
    @GetMapping("/subjects")
    public R<List<Map<String, Object>>> listSubjects() {
        return R.ok(listOrSeedSubjects(resolveTenantId()));
    }

    @ApiOperation("创建会计科目")
    @PostMapping("/subjects")
    @Transactional
    public R<Map<String, Object>> createFinanceSubject(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String subjectCode = text(body == null ? null : body.get("subjectCode"), "");
        String subjectName = text(body == null ? null : body.get("subjectName"), "");
        if (!StringUtils.hasText(subjectCode) || !StringUtils.hasText(subjectName)) {
            return R.error("请填写科目编码和科目名称");
        }
        String parentCode = text(body == null ? null : body.get("parentCode"), "");
        String parentError = validateSubjectParent(tenantId, null, subjectCode, parentCode);
        if (StringUtils.hasText(parentError)) {
            return R.error(parentError);
        }
        Long existingId = findSubjectIdByCode(tenantId, subjectCode);
        if (existingId != null) {
            jdbcTemplate.update(
                    "UPDATE finance_subject SET subject_name=?, subject_type=?, parent_code=?, direction=?, status=?, remark=?, operator_id=?, update_time=NOW(), is_deleted=0 WHERE tenant_id=? AND id=?",
                    subjectName,
                    text(body.get("subjectType"), "EXPENSE"),
                    parentCode,
                    text(body.get("direction"), "DEBIT"),
                    number(body.get("status"), 1),
                    text(body.get("remark"), ""),
                    UserContext.getUser(),
                    tenantId,
                    existingId
            );
            insertFinanceAuditLog("SUBJECT", existingId, "UPSERT", null, body, "更新会计科目", "LOW", "科目已更新");
            return R.ok(subjectResult(existingId, subjectCode, subjectName, number(body.get("status"), 1)));
        }
        Long id = insertWithKey(
                "INSERT INTO finance_subject (tenant_id,subject_code,subject_name,subject_type,parent_code,direction,status,remark,operator_id) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId,
                subjectCode,
                subjectName,
                text(body.get("subjectType"), "EXPENSE"),
                parentCode,
                text(body.get("direction"), "DEBIT"),
                number(body.get("status"), 1),
                text(body.get("remark"), ""),
                UserContext.getUser()
        );
        insertFinanceAuditLog("SUBJECT", id, "CREATE", null, body, "创建会计科目", "LOW", "科目已创建");
        return R.ok(subjectResult(id, subjectCode, subjectName, number(body.get("status"), 1)));
    }

    @ApiOperation("编辑会计科目")
    @PutMapping("/subjects/{id}")
    @Transactional
    public R<Map<String, Object>> updateFinanceSubject(@PathVariable Long id,
                                                       @RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> subject = findTenantRow("finance_subject", tenantId, id);
        if (subject == null) {
            return R.error("会计科目不存在");
        }
        String subjectCode = text(body == null ? null : body.get("subjectCode"), text(first(subject, "subject_code", "subjectCode"), ""));
        String subjectName = text(body == null ? null : body.get("subjectName"), text(first(subject, "subject_name", "subjectName"), ""));
        if (!StringUtils.hasText(subjectCode) || !StringUtils.hasText(subjectName)) {
            return R.error("请填写科目编码和科目名称");
        }
        Long existingId = findSubjectIdByCode(tenantId, subjectCode);
        if (existingId != null && !existingId.equals(id)) {
            return R.error("科目编码已存在");
        }
        String parentCode = text(body == null ? null : body.get("parentCode"), text(first(subject, "parent_code", "parentCode"), ""));
        String parentError = validateSubjectParent(tenantId, id, subjectCode, parentCode);
        if (StringUtils.hasText(parentError)) {
            return R.error(parentError);
        }
        Integer status = number(body == null ? null : body.get("status"), number(first(subject, "status"), 1));
        jdbcTemplate.update(
                "UPDATE finance_subject SET subject_code=?, subject_name=?, subject_type=?, parent_code=?, direction=?, status=?, remark=?, operator_id=?, update_time=NOW() WHERE tenant_id=? AND id=? AND is_deleted=0",
                subjectCode,
                subjectName,
                text(body == null ? null : body.get("subjectType"), text(first(subject, "subject_type", "subjectType"), "EXPENSE")),
                parentCode,
                text(body == null ? null : body.get("direction"), text(first(subject, "direction"), "DEBIT")),
                status,
                text(body == null ? null : body.get("remark"), text(first(subject, "remark"), "")),
                UserContext.getUser(),
                tenantId,
                id
        );
        Map<String, Object> result = subjectResult(id, subjectCode, subjectName, status);
        insertFinanceAuditLog("SUBJECT", id, "UPDATE", subject, result, "编辑会计科目", "LOW", "科目已更新");
        return R.ok(result);
    }

    @ApiOperation("删除会计科目")
    @DeleteMapping("/subjects/{id}")
    @Transactional
    public R<Map<String, Object>> deleteFinanceSubject(@PathVariable Long id) {
        Long tenantId = resolveTenantId();
        Map<String, Object> subject = findTenantRow("finance_subject", tenantId, id);
        if (subject == null) {
            return R.error("会计科目不存在");
        }
        jdbcTemplate.update(
                "UPDATE finance_subject SET is_deleted=1, status=0, operator_id=?, update_time=NOW() WHERE tenant_id=? AND id=? AND is_deleted=0",
                UserContext.getUser(),
                tenantId,
                id
        );
        Map<String, Object> result = subjectResult(id, text(first(subject, "subject_code", "subjectCode"), ""), text(first(subject, "subject_name", "subjectName"), ""), 0);
        insertFinanceAuditLog("SUBJECT", id, "DELETE", subject, result, "删除会计科目", "MEDIUM", "科目已删除");
        return R.ok(result);
    }

    @ApiOperation("切换会计科目状态")
    @PutMapping("/subjects/{id}/status")
    @Transactional
    public R<Map<String, Object>> updateFinanceSubjectStatus(@PathVariable Long id,
                                                             @RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> subject = findTenantRow("finance_subject", tenantId, id);
        if (subject == null) {
            return R.error("会计科目不存在");
        }
        Integer status = number(body == null ? null : body.get("status"), 1);
        jdbcTemplate.update(
                "UPDATE finance_subject SET status=?, remark=?, operator_id=?, update_time=NOW() WHERE tenant_id=? AND id=? AND is_deleted=0",
                status,
                text(body == null ? null : body.get("remark"), ""),
                UserContext.getUser(),
                tenantId,
                id
        );
        Map<String, Object> result = subjectResult(id, text(first(subject, "subject_code", "subjectCode"), ""), text(first(subject, "subject_name", "subjectName"), ""), status);
        insertFinanceAuditLog("SUBJECT", id, "STATUS", subject, result, "切换会计科目状态", "LOW", status == 1 ? "已启用" : "已停用");
        return R.ok(result);
    }

    @ApiOperation("查询财务审批模板")
    @GetMapping("/approval-templates")
    public R<List<Map<String, Object>>> listFinanceApprovalTemplates() {
        return R.ok(listApprovalTemplatesWithNodes(resolveTenantId()));
    }

    @ApiOperation("创建财务审批模板")
    @PostMapping("/approval-templates")
    @Transactional
    public R<Map<String, Object>> createFinanceApprovalTemplate(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String businessType = text(body == null ? null : body.get("businessType"), "REIMBURSEMENT");
        Integer status = number(body == null ? null : body.get("status"), 0);
        if (status == 1) {
            disableApprovalTemplates(tenantId, businessType);
        }
        Long id = insertWithKey(
                "INSERT INTO finance_approval_template (tenant_id,template_name,business_type,min_amount,max_amount,status,remark,operator_id) VALUES (?,?,?,?,?,?,?,?)",
                tenantId,
                text(body == null ? null : body.get("templateName"), "报销审批模板"),
                businessType,
                money(body == null ? null : body.get("minAmount")),
                nullableMoney(body == null ? null : body.get("maxAmount")),
                status,
                text(body == null ? null : body.get("remark"), ""),
                UserContext.getUser()
        );
        insertApprovalTemplateNodes(tenantId, id, body);
        Map<String, Object> result = findTenantRow("finance_approval_template", tenantId, id);
        insertFinanceAuditLog("APPROVAL_TEMPLATE", id, "CREATE", null, result, "创建审批模板", "LOW", "审批模板已创建");
        return R.ok(withTemplateNodes(tenantId, result));
    }

    @ApiOperation("创建默认财务审批模板")
    @PostMapping("/approval-templates/default")
    @Transactional
    public R<Map<String, Object>> createDefaultFinanceApprovalTemplate(@RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> existing = resolveActiveApprovalTemplate(tenantId, "REIMBURSEMENT", money(body == null ? null : body.get("amount")));
        if (existing != null) {
            enableApprovalTemplateInternal(tenantId, longValue(first(existing, "id"), 0L));
            return R.ok(withTemplateNodes(tenantId, existing));
        }
        Long firstApproverId = longValue(body == null ? null : first(body, "firstApproverId", "approverId", "defaultApproverId"), UserContext.getUser());
        Long secondApproverId = longValue(body == null ? null : first(body, "secondApproverId", "reviewerId"), firstApproverId);
        Long id = insertWithKey(
                "INSERT INTO finance_approval_template (tenant_id,template_name,business_type,min_amount,max_amount,status,remark,operator_id) VALUES (?,?,?,?,?,?,?,?)",
                tenantId,
                text(body == null ? null : body.get("templateName"), "默认报销审批模板"),
                "REIMBURSEMENT",
                BigDecimal.ZERO,
                null,
                1,
                "系统生成的报销多级审批模板",
                UserContext.getUser()
        );
        insertApprovalTemplateNode(tenantId, id, 1, "财务初审", firstApproverId, text(body == null ? null : body.get("firstApproverName"), "财务审批人"), "FINANCE");
        insertApprovalTemplateNode(tenantId, id, 2, "负责人复核", secondApproverId, text(body == null ? null : body.get("secondApproverName"), "负责人"), "MANAGER");
        Map<String, Object> result = findTenantRow("finance_approval_template", tenantId, id);
        insertFinanceAuditLog("APPROVAL_TEMPLATE", id, "DEFAULT", null, result, "生成默认审批模板", "LOW", "默认审批模板已启用");
        return R.ok(withTemplateNodes(tenantId, result));
    }

    @ApiOperation("启用财务审批模板")
    @PutMapping("/approval-templates/{id}/enable")
    @Transactional
    public R<Map<String, Object>> enableFinanceApprovalTemplate(@PathVariable Long id) {
        Long tenantId = resolveTenantId();
        Map<String, Object> template = findTenantRow("finance_approval_template", tenantId, id);
        if (template == null) {
            return R.error("审批模板不存在");
        }
        enableApprovalTemplateInternal(tenantId, id);
        Map<String, Object> result = findTenantRow("finance_approval_template", tenantId, id);
        insertFinanceAuditLog("APPROVAL_TEMPLATE", id, "ENABLE", template, result, "启用审批模板", "LOW", "审批模板已启用");
        return R.ok(withTemplateNodes(tenantId, result));
    }

    @ApiOperation("查询会计期间结账记录")
    @GetMapping("/periods")
    public R<List<Map<String, Object>>> listPeriods() {
        return R.ok(queryTenantRows("finance_period_close", resolveTenantId(), 100));
    }

    @ApiOperation("会计期间结账")
    @PostMapping("/periods/close")
    @Transactional
    public R<Map<String, Object>> closeFinancePeriod(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String period = text(body == null ? null : body.get("period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        boolean force = Boolean.TRUE.equals(body == null ? null : body.get("force"));
        Long pendingRecordCount = countWhere("finance_record", tenantId, "period = ? AND status = 0", period);
        Long pendingVoucherCount = countWhere("finance_voucher", tenantId, "period = ? AND posting_status = 0", period);
        long pendingCount = safeLong(pendingRecordCount) + safeLong(pendingVoucherCount);
        if (pendingCount > 0 && !force) {
            return R.error("该期间仍有未审核单据或未过账凭证，请处理后再结账");
        }
        BigDecimal income = sumFinanceRecords(tenantId, period, 1, 1);
        BigDecimal expense = sumFinanceRecords(tenantId, period, 2, 1);
        Long voucherCount = countWhere("finance_voucher", tenantId, "period = ?", period);
        Long ledgerCount = countWhere("finance_ledger", tenantId, "period = ?", period);
        Long existingId = findPeriodCloseId(tenantId, period);
        if (existingId == null) {
            existingId = insertWithKey(
                    "INSERT INTO finance_period_close (tenant_id,period,close_type,status,income_amount,expense_amount,voucher_count,ledger_count,pending_count,closed_by,closed_time,remark) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                    tenantId,
                    period,
                    force ? "FORCE" : "NORMAL",
                    1,
                    income,
                    expense,
                    voucherCount,
                    ledgerCount,
                    pendingCount,
                    UserContext.getUser(),
                    LocalDateTime.now(),
                    text(body == null ? null : body.get("remark"), "")
            );
        } else {
            jdbcTemplate.update(
                    "UPDATE finance_period_close SET close_type=?, status=1, income_amount=?, expense_amount=?, voucher_count=?, ledger_count=?, pending_count=?, closed_by=?, closed_time=?, remark=?, update_time=NOW(), is_deleted=0 WHERE tenant_id=? AND id=?",
                    force ? "FORCE" : "NORMAL",
                    income,
                    expense,
                    voucherCount,
                    ledgerCount,
                    pendingCount,
                    UserContext.getUser(),
                    LocalDateTime.now(),
                    text(body == null ? null : body.get("remark"), ""),
                    tenantId,
                    existingId
            );
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", existingId);
        result.put("period", period);
        result.put("incomeAmount", income);
        result.put("expenseAmount", expense);
        result.put("pendingCount", pendingCount);
        result.put("force", force);
        insertFinanceAuditLog("PERIOD_CLOSE", existingId, "CLOSE", null, result, "期间结账", force ? "HIGH" : "LOW", "结账成功");
        return R.ok(result);
    }

    @ApiOperation("会计期间反结账")
    @PostMapping("/periods/reopen")
    @Transactional
    public R<Map<String, Object>> reopenFinancePeriod(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String period = text(body == null ? null : body.get("period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        String reason = text(body == null ? null : body.get("reason"), text(body == null ? null : body.get("remark"), "后台反结账"));
        Long existingId = findPeriodCloseId(tenantId, period);
        if (existingId == null) {
            return R.error("该会计期间未结账，无需反结账");
        }
        Map<String, Object> before = findTenantRow("finance_period_close", tenantId, existingId);
        jdbcTemplate.update(
                "UPDATE finance_period_close SET status=0, close_type='REOPEN', remark=?, update_time=NOW(), is_deleted=0 WHERE tenant_id=? AND id=?",
                reason,
                tenantId,
                existingId
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", existingId);
        result.put("period", period);
        result.put("status", 0);
        result.put("reason", reason);
        insertFinanceAuditLog("PERIOD_CLOSE", existingId, "REOPEN", before, result, "期间反结账", "HIGH", reason);
        return R.ok(result);
    }

    @ApiOperation("Request period reopen approval")
    @PostMapping("/periods/reopen/request")
    @Transactional
    public R<Map<String, Object>> requestReopenFinancePeriod(@RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String period = text(body == null ? null : first(body, "period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        Long existingId = findPeriodCloseId(tenantId, period);
        if (existingId == null) {
            return R.error("period close record not found");
        }
        Map<String, Object> pending = findPendingGovernanceAction(tenantId, null, "PERIOD_REOPEN", existingId, "REOPEN_REQUEST");
        if (pending != null) {
            Long pendingId = longValue(first(pending, "id"), 0L);
            return R.ok(governanceApprovalResult(pendingId, existingId, "PERIOD_REOPEN", "REOPEN_REQUEST", 0, "pending request exists"));
        }
        String reason = text(body == null ? null : first(body, "reason", "remark", "opinion"), "period reopen request");
        Long approverId = longValue(body == null ? null : first(body, "approverId", "toApproverId"), UserContext.getUser());
        Long actionId = insertApprovalActionWithKey("PERIOD_REOPEN", existingId, "REOPEN_REQUEST", UserContext.getUser(), approverId, period + "|" + reason, 0);
        insertFinanceAuditLog("PERIOD_CLOSE", existingId, "REOPEN_REQUEST", findTenantRow("finance_period_close", tenantId, existingId), Map.of("actionId", actionId, "period", period), reason, "HIGH", "pending approval");
        return R.ok(governanceApprovalResult(actionId, existingId, "PERIOD_REOPEN", "REOPEN_REQUEST", 0, "request submitted"));
    }

    @ApiOperation("Approve period reopen")
    @PostMapping("/periods/reopen/{actionId}/approve")
    @Transactional
    public R<Map<String, Object>> approveReopenFinancePeriod(@PathVariable Long actionId,
                                                             @RequestBody(required = false) Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        Map<String, Object> action = findPendingGovernanceAction(tenantId, actionId, "PERIOD_REOPEN", null, "REOPEN_REQUEST");
        if (action == null) {
            return R.error("pending reopen request not found");
        }
        Long periodCloseId = longValue(first(action, "business_id", "businessId"), null);
        Map<String, Object> periodClose = findTenantRow("finance_period_close", tenantId, periodCloseId);
        if (periodClose == null) {
            return R.error("period close record not found");
        }
        String opinion = text(body == null ? null : first(body, "opinion", "remark", "reason"), "approval passed");
        Map<String, Object> executeBody = new LinkedHashMap<>();
        executeBody.put("period", text(first(periodClose, "period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))));
        executeBody.put("reason", opinion);
        R<Map<String, Object>> executed = reopenFinancePeriod(executeBody);
        if (executed.getCode() == null || executed.getCode() != 200) {
            return executed;
        }
        markGovernanceActionApproved(tenantId, actionId, opinion);
        insertApprovalAction("PERIOD_REOPEN", periodCloseId, "REOPEN_APPROVE", first(action, "from_approver_id", "fromApproverId"), UserContext.getUser(), opinion, 1);
        Map<String, Object> result = new LinkedHashMap<>(executed.getData());
        result.put("actionId", actionId);
        result.put("approvalStatus", 1);
        return R.ok(result);
    }

    @ApiOperation("查询对账记录")
    @GetMapping("/reconciliations")
    public R<List<Map<String, Object>>> listReconciliations() {
        return R.ok(queryTenantRows("finance_reconciliation", resolveTenantId(), 100));
    }

    @ApiOperation("创建对账记录")
    @PostMapping("/reconciliations")
    @Transactional
    public R<Map<String, Object>> createFinanceReconciliation(@RequestBody Map<String, Object> body) {
        Long tenantId = resolveTenantId();
        String period = text(body == null ? null : body.get("period"), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")));
        FinanceAccount account = findOrCreateAccount(tenantId);
        BigDecimal accountBalance = body != null && body.containsKey("accountBalance")
                ? money(body.get("accountBalance"))
                : safe(account.getBalance());
        BigDecimal ledgerBalance = sumLedgerBalance(tenantId, period);
        BigDecimal difference = accountBalance.subtract(ledgerBalance);
        Integer status = difference.compareTo(BigDecimal.ZERO) == 0 ? 1 : 0;
        Long id = insertWithKey(
                "INSERT INTO finance_reconciliation (tenant_id,reconciliation_no,period,account_balance,ledger_balance,difference_amount,status,handler_id,remark) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId,
                docNo("DZ"),
                period,
                accountBalance,
                ledgerBalance,
                difference,
                status,
                UserContext.getUser(),
                text(body == null ? null : body.get("remark"), "")
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("period", period);
        result.put("accountBalance", accountBalance);
        result.put("ledgerBalance", ledgerBalance);
        result.put("differenceAmount", difference);
        result.put("status", status);
        insertFinanceAuditLog("RECONCILIATION", id, "CREATE", null, result, "资金对账", difference.compareTo(BigDecimal.ZERO) == 0 ? "LOW" : "HIGH", "对账记录已生成");
        return R.ok(result);
    }

    @ApiOperation("导出财务报表")
    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportFinanceReport(@RequestParam(required = false) String period) {
        Long tenantId = resolveTenantId();
        List<FinanceRecord> records = queryFinanceRecordsForReport(tenantId, period);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("period,type,record_no,title,category,amount,status,business_type,create_time\n");
        for (FinanceRecord record : records) {
            csv.append(csv(record.getPeriod())).append(',')
                    .append(csv(record.getType() != null && record.getType() == 1 ? "income" : "expense")).append(',')
                    .append(csv(record.getRecordNo())).append(',')
                    .append(csv(record.getTitle())).append(',')
                    .append(csv(record.getCategory())).append(',')
                    .append(csv(record.getAmount())).append(',')
                    .append(csv(record.getStatus())).append(',')
                    .append(csv(record.getBusinessType())).append(',')
                    .append(csv(record.getCreateTime()))
                    .append('\n');
        }
        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        String name = "finance-report-" + safeFileName(StringUtils.hasText(period) ? period : "all") + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private FinanceAccount findOrCreateAccount(Long tenantId) {
        FinanceAccount account = financeAccountMapper.selectOne(new LambdaQueryWrapper<FinanceAccount>()
                .eq(FinanceAccount::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (account != null) {
            return account;
        }
        account = new FinanceAccount();
        account.setTenantId(tenantId);
        account.setBalance(BigDecimal.ZERO);
        account.setTotalIncome(BigDecimal.ZERO);
        account.setTotalExpense(BigDecimal.ZERO);
        financeAccountMapper.insert(account);
        return account;
    }

    private FinanceRecord findRecord(Long id) {
        return financeRecordMapper.selectOne(new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getId, id)
                .eq(FinanceRecord::getTenantId, resolveTenantId())
                .last("LIMIT 1"));
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sum(List<FinanceRecord> records, Integer type, Integer status) {
        return records.stream()
                .filter(record -> type == null || (record.getType() != null && record.getType().equals(type)))
                .filter(record -> status == null || (record.getStatus() != null && record.getStatus().equals(status)))
                .map(record -> safe(record.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Map<String, Object>> buildCategoryBreakdown(List<FinanceRecord> records) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (FinanceRecord record : records) {
            String category = StringUtils.hasText(record.getCategory()) ? record.getCategory() : "other";
            Map<String, Object> item = grouped.computeIfAbsent(category, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("category", key);
                value.put("income", BigDecimal.ZERO);
                value.put("expense", BigDecimal.ZERO);
                value.put("pending", BigDecimal.ZERO);
                return value;
            });
            BigDecimal amount = safe(record.getAmount());
            if (record.getStatus() != null && record.getStatus() == 0) {
                item.put("pending", ((BigDecimal) item.get("pending")).add(amount));
            }
            if (record.getType() != null && record.getType() == 1) {
                item.put("income", ((BigDecimal) item.get("income")).add(amount));
            }
            if (record.getType() != null && record.getType() == 2) {
                item.put("expense", ((BigDecimal) item.get("expense")).add(amount));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private List<Map<String, Object>> buildMonthlyTrend(List<FinanceRecord> records) {
        Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (FinanceRecord record : records) {
            String period = StringUtils.hasText(record.getPeriod()) ? record.getPeriod() :
                    (record.getCreateTime() == null ? "未归期" : record.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM")));
            Map<String, Object> item = grouped.computeIfAbsent(period, key -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("period", key);
                value.put("income", BigDecimal.ZERO);
                value.put("expense", BigDecimal.ZERO);
                return value;
            });
            BigDecimal amount = safe(record.getAmount());
            if (record.getType() != null && record.getType() == 1 && record.getStatus() != null && record.getStatus() == 1) {
                item.put("income", ((BigDecimal) item.get("income")).add(amount));
            }
            if (record.getType() != null && record.getType() == 2 && record.getStatus() != null && record.getStatus() == 1) {
                item.put("expense", ((BigDecimal) item.get("expense")).add(amount));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    private List<Map<String, Object>> buildRiskAlerts(FinanceAccount account, BigDecimal pendingAmount, BigDecimal usageRate) {
        List<Map<String, Object>> alerts = new ArrayList<>();
        if (safe(account.getBalance()).compareTo(BigDecimal.ZERO) < 0) {
            alerts.add(Map.of("type", "danger", "title", "账户余额为负", "message", "请立即核对支出审批和资金入账。"));
        }
        if (usageRate.compareTo(BigDecimal.valueOf(80)) >= 0) {
            alerts.add(Map.of("type", "warning", "title", "预算使用率偏高", "message", "当前预算使用率 " + usageRate + "%。"));
        }
        if (pendingAmount.compareTo(BigDecimal.ZERO) > 0) {
            alerts.add(Map.of("type", "info", "title", "存在待审单据", "message", "待审核金额 " + pendingAmount + " 元。"));
        }
        return alerts;
    }

    private String buildRecordNo(FinanceRecord record) {
        String prefix = record.getType() != null && record.getType() == 1 ? "SR" : "ZC";
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String defaultBusinessType(FinanceRecord record) {
        if (record.getType() != null && record.getType() == 1) {
            return "INCOME";
        }
        if ("reimbursement".equals(record.getCategory())) {
            return "REIMBURSEMENT";
        }
        return "EXPENSE";
    }

    private List<Map<String, Object>> queryTenantRows(String tableName, Long tenantId, int limit) {
        String safeTable = requireFinanceTable(tableName);
        return jdbcTemplate.queryForList("SELECT * FROM " + safeTable + " WHERE tenant_id = ? AND is_deleted = 0 ORDER BY create_time DESC LIMIT ?",
                tenantId,
                Math.max(1, Math.min(limit, 500)));
    }

    private List<Map<String, Object>> queryPendingReimbursementApprovals(Long tenantId, Long currentApproverId, int limit) {
        if (currentApproverId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT * FROM finance_reimbursement WHERE tenant_id = ? AND is_deleted = 0 AND status = 0 AND current_approver_id = ? ORDER BY update_time DESC, create_time DESC LIMIT ?",
                tenantId,
                currentApproverId,
                Math.max(1, Math.min(limit, 500))
        );
    }

    private List<FinanceRecord> queryFinanceRecordsForReport(Long tenantId, String period) {
        LambdaQueryWrapper<FinanceRecord> wrapper = new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getTenantId, tenantId)
                .orderByDesc(FinanceRecord::getCreateTime);
        if (StringUtils.hasText(period)) {
            wrapper.eq(FinanceRecord::getPeriod, period);
        }
        return financeRecordMapper.selectList(wrapper);
    }

    private BigDecimal sumColumn(String tableName, String columnName, Long tenantId) {
        return sumColumnWhere(tableName, columnName, tenantId, "1=1");
    }

    private BigDecimal sumColumnWhere(String tableName, String columnName, Long tenantId, String whereClause) {
        String safeTable = requireFinanceTable(tableName);
        String safeColumn = requireFinanceColumn(columnName);
        BigDecimal value = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(" + safeColumn + "), 0) FROM " + safeTable +
                        " WHERE tenant_id = ? AND is_deleted = 0 AND " + whereClause,
                BigDecimal.class,
                tenantId);
        return safe(value);
    }

    private BigDecimal sumMoney(List<Map<String, Object>> rows, String columnName) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        String safeColumn = requireFinanceColumn(columnName);
        return rows.stream()
                .map(row -> money(row.get(safeColumn)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Map<String, Object> findTenantRow(String tableName, Long tenantId, Long id) {
        String safeTable = requireFinanceTable(tableName);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + safeTable + " WHERE tenant_id = ? AND id = ? AND is_deleted = 0 LIMIT 1",
                tenantId,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> listApprovalTemplatesWithNodes(Long tenantId) {
        List<Map<String, Object>> templates = queryTenantRows("finance_approval_template", tenantId, 100);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> template : templates) {
            result.add(withTemplateNodes(tenantId, template));
        }
        return result;
    }

    private Map<String, Object> withTemplateNodes(Long tenantId, Map<String, Object> template) {
        if (template == null) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>(template);
        Long templateId = longValue(first(template, "id"), null);
        result.put("nodes", templateId == null ? List.of() : queryApprovalTemplateNodes(tenantId, templateId));
        return result;
    }

    private Map<String, Object> resolveActiveApprovalTemplate(Long tenantId, String businessType, BigDecimal amount) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM finance_approval_template WHERE tenant_id = ? AND business_type = ? AND status = 1 AND is_deleted = 0 " +
                        "AND min_amount <= ? AND (max_amount IS NULL OR max_amount = 0 OR max_amount >= ?) ORDER BY min_amount DESC, update_time DESC LIMIT 1",
                tenantId,
                businessType,
                safe(amount),
                safe(amount)
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> queryApprovalTemplateNodes(Long tenantId, Long templateId) {
        if (templateId == null) {
            return List.of();
        }
        return jdbcTemplate.queryForList(
                "SELECT * FROM finance_approval_template_node WHERE tenant_id = ? AND template_id = ? AND is_deleted = 0 ORDER BY step_no ASC, sort_no ASC, id ASC",
                tenantId,
                templateId
        );
    }

    private Map<String, Object> applyApprovalTemplateToReimbursement(Long tenantId, Long reimbursementId, BigDecimal amount, Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> template = resolveActiveApprovalTemplate(tenantId, "REIMBURSEMENT", amount);
        if (template == null) {
            result.put("approvalTemplateId", null);
            result.put("currentStepNo", 0);
            result.put("currentApproverId", longValue(body == null ? null : body.get("currentApproverId"), null));
            return result;
        }
        Long templateId = longValue(first(template, "id"), null);
        List<Map<String, Object>> nodes = queryApprovalTemplateNodes(tenantId, templateId);
        Map<String, Object> firstNode = nodes.isEmpty() ? null : nodes.get(0);
        Long firstApproverId = longValue(first(firstNode, "approver_id", "approverId"), longValue(body == null ? null : body.get("currentApproverId"), null));
        Integer firstStepNo = number(first(firstNode, "step_no", "stepNo"), 1);
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET approval_template_id = ?, current_step_no = ?, current_approver_id = ?, status = 0, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                templateId,
                firstStepNo,
                firstApproverId,
                UserContext.getUser(),
                tenantId,
                reimbursementId
        );
        insertApprovalAction("REIMBURSEMENT", reimbursementId, "TEMPLATE_APPLY", null, firstApproverId, "套用审批模板：" + text(first(template, "template_name", "templateName"), ""), 0);
        result.put("approvalTemplateId", templateId);
        result.put("currentStepNo", firstStepNo);
        result.put("currentApproverId", firstApproverId);
        return result;
    }

    private Map<String, Object> advanceReimbursementApproval(Long tenantId, Long reimbursementId, Map<String, Object> reimbursement,
                                                             BigDecimal amount, Map<String, Object> body) {
        Long templateId = longValue(first(reimbursement, "approval_template_id", "approvalTemplateId"), null);
        Integer currentStepNo = number(first(reimbursement, "current_step_no", "currentStepNo"), 0);
        if (templateId == null || currentStepNo <= 0) {
            return null;
        }
        List<Map<String, Object>> nodes = queryApprovalTemplateNodes(tenantId, templateId);
        Map<String, Object> nextNode = null;
        for (Map<String, Object> node : nodes) {
            Integer stepNo = number(first(node, "step_no", "stepNo"), 0);
            if (stepNo > currentStepNo) {
                nextNode = node;
                break;
            }
        }
        String opinion = text(body == null ? null : body.get("opinion"), "审批通过");
        if (nextNode == null) {
            return null;
        }
        Integer nextStepNo = number(first(nextNode, "step_no", "stepNo"), currentStepNo + 1);
        Long nextApproverId = longValue(first(nextNode, "approver_id", "approverId"), null);
        jdbcTemplate.update(
                "UPDATE finance_reimbursement SET status = 0, current_approver_id = ?, current_step_no = ?, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                nextApproverId,
                nextStepNo,
                UserContext.getUser(),
                tenantId,
                reimbursementId
        );
        Map<String, Object> result = approvalResult(reimbursementId, 0, nextApproverId, "审批通过，已流转到下一节点");
        result.put("approvalTemplateId", templateId);
        result.put("currentStepNo", nextStepNo);
        insertApprovalAction("REIMBURSEMENT", reimbursementId, "APPROVE_STEP", first(reimbursement, "current_approver_id", "currentApproverId"), nextApproverId, opinion, 0);
        insertFinanceAuditLog("REIMBURSEMENT", reimbursementId, "APPROVE_STEP", reimbursement, result,
                "报销多级审批流转", amount.compareTo(BigDecimal.valueOf(3000)) >= 0 ? "HIGH" : "LOW", "已流转到第 " + nextStepNo + " 节点");
        return result;
    }

    @SuppressWarnings("unchecked")
    private void insertApprovalTemplateNodes(Long tenantId, Long templateId, Map<String, Object> body) {
        Object rawNodes = body == null ? null : body.get("nodes");
        if (rawNodes instanceof List<?>) {
            int index = 1;
            for (Object rawNode : (List<?>) rawNodes) {
                if (rawNode instanceof Map<?, ?>) {
                    Map<String, Object> node = (Map<String, Object>) rawNode;
                    insertApprovalTemplateNode(
                            tenantId,
                            templateId,
                            number(first(node, "stepNo", "step_no"), index),
                            text(first(node, "nodeName", "node_name"), "审批节点" + index),
                            longValue(first(node, "approverId", "approver_id"), null),
                            text(first(node, "approverName", "approver_name"), ""),
                            text(first(node, "approverRole", "approver_role"), "")
                    );
                    index++;
                }
            }
            if (index > 1) {
                return;
            }
        }
        insertApprovalTemplateNode(
                tenantId,
                templateId,
                1,
                "财务审批",
                longValue(body == null ? null : first(body, "approverId", "currentApproverId", "defaultApproverId"), UserContext.getUser()),
                text(body == null ? null : body.get("approverName"), "财务审批人"),
                text(body == null ? null : body.get("approverRole"), "FINANCE")
        );
    }

    private void insertApprovalTemplateNode(Long tenantId, Long templateId, Integer stepNo, String nodeName,
                                            Long approverId, String approverName, String approverRole) {
        jdbcTemplate.update(
                "INSERT INTO finance_approval_template_node (tenant_id,template_id,step_no,node_name,approver_id,approver_name,approver_role,approve_type,required_flag,sort_no,remark,operator_id) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                tenantId,
                templateId,
                stepNo,
                nodeName,
                approverId,
                approverName,
                approverRole,
                "ANY",
                1,
                stepNo,
                "",
                UserContext.getUser()
        );
    }

    private void disableApprovalTemplates(Long tenantId, String businessType) {
        jdbcTemplate.update(
                "UPDATE finance_approval_template SET status = 0, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND business_type = ? AND is_deleted = 0",
                UserContext.getUser(),
                tenantId,
                businessType
        );
    }

    private void enableApprovalTemplateInternal(Long tenantId, Long id) {
        Map<String, Object> template = findTenantRow("finance_approval_template", tenantId, id);
        String businessType = text(first(template, "business_type", "businessType"), "REIMBURSEMENT");
        disableApprovalTemplates(tenantId, businessType);
        jdbcTemplate.update(
                "UPDATE finance_approval_template SET status = 1, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                UserContext.getUser(),
                tenantId,
                id
        );
    }

    private Object first(Map<String, Object> row, String... keys) {
        if (row == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return null;
    }

    private boolean isPeriodClosed(Long tenantId, String period) {
        Long count = countWhere("finance_period_close", tenantId, "period = ? AND status = 1", period);
        return safeLong(count) > 0;
    }

    private Long findPeriodCloseId(Long tenantId, String period) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM finance_period_close WHERE tenant_id = ? AND period = ? AND is_deleted = 0 LIMIT 1",
                tenantId,
                period
        );
        return rows.isEmpty() ? null : longValue(rows.get(0).get("id"), null);
    }

    private Long countWhere(String tableName, Long tenantId, String whereClause, Object... args) {
        String safeTable = requireFinanceTable(tableName);
        List<Object> params = new ArrayList<>();
        params.add(tenantId);
        if (args != null) {
            for (Object arg : args) {
                params.add(arg);
            }
        }
        Long value = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + safeTable + " WHERE tenant_id = ? AND is_deleted = 0 AND " + whereClause,
                Long.class,
                params.toArray()
        );
        return value == null ? 0L : value;
    }

    private BigDecimal sumFinanceRecords(Long tenantId, String period, Integer type, Integer status) {
        BigDecimal value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM finance_record WHERE tenant_id = ? AND is_deleted = 0 AND period = ? AND type = ? AND status = ?",
                BigDecimal.class,
                tenantId,
                period,
                type,
                status
        );
        return safe(value);
    }

    private BigDecimal sumLedgerBalance(Long tenantId, String period) {
        BigDecimal value = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(debit_amount - credit_amount), 0) FROM finance_ledger WHERE tenant_id = ? AND is_deleted = 0 AND period = ?",
                BigDecimal.class,
                tenantId,
                period
        );
        return safe(value);
    }

    private List<Map<String, Object>> listOrSeedSubjects(Long tenantId) {
        Long count = countWhere("finance_subject", tenantId, "1=1");
        if (safeLong(count) == 0) {
            seedDefaultSubjects(tenantId);
        }
        return queryTenantRows("finance_subject", tenantId, 200);
    }

    private void seedDefaultSubjects(Long tenantId) {
        Object[][] defaults = new Object[][]{
                {"1001", "库存现金", "ASSET", "", "DEBIT"},
                {"1002", "银行存款", "ASSET", "", "DEBIT"},
                {"4001", "会费收入", "INCOME", "", "CREDIT"},
                {"4002", "财政拨款收入", "INCOME", "", "CREDIT"},
                {"5001", "活动执行费用", "EXPENSE", "", "DEBIT"},
                {"5002", "物资采购费用", "EXPENSE", "", "DEBIT"},
                {"6601", "管理费用", "EXPENSE", "", "DEBIT"}
        };
        for (Object[] item : defaults) {
            if (findSubjectIdByCode(tenantId, String.valueOf(item[0])) != null) {
                continue;
            }
            insertWithKey(
                    "INSERT INTO finance_subject (tenant_id,subject_code,subject_name,subject_type,parent_code,direction,status,remark,operator_id) VALUES (?,?,?,?,?,?,?,?,?)",
                    tenantId,
                    item[0],
                    item[1],
                    item[2],
                    item[3],
                    item[4],
                    1,
                    "系统默认科目",
                    UserContext.getUser()
            );
        }
    }

    private Long findSubjectIdByCode(Long tenantId, String subjectCode) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM finance_subject WHERE tenant_id = ? AND subject_code = ? AND is_deleted = 0 LIMIT 1",
                tenantId,
                subjectCode
        );
        return rows.isEmpty() ? null : longValue(rows.get(0).get("id"), null);
    }

    private Map<String, Object> findSubjectByCode(Long tenantId, String subjectCode) {
        if (!StringUtils.hasText(subjectCode)) {
            return null;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM finance_subject WHERE tenant_id = ? AND subject_code = ? AND is_deleted = 0 LIMIT 1",
                tenantId,
                subjectCode
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private String validateSubjectParent(Long tenantId, Long subjectId, String subjectCode, String parentCode) {
        if (!StringUtils.hasText(parentCode)) {
            return "";
        }
        if (parentCode.equals(subjectCode)) {
            return "上级科目不能指向自己";
        }
        Map<String, Object> parent = findSubjectByCode(tenantId, parentCode);
        if (parent == null) {
            return "上级科目不存在";
        }
        String cursorParentCode = text(first(parent, "parent_code", "parentCode"), "");
        int guard = 0;
        while (StringUtils.hasText(cursorParentCode) && guard < 20) {
            if (cursorParentCode.equals(subjectCode)) {
                return "上级科目不能形成循环";
            }
            Map<String, Object> cursor = findSubjectByCode(tenantId, cursorParentCode);
            if (cursor == null) {
                break;
            }
            Long cursorId = longValue(first(cursor, "id"), null);
            if (subjectId != null && subjectId.equals(cursorId)) {
                return "上级科目不能形成循环";
            }
            cursorParentCode = text(first(cursor, "parent_code", "parentCode"), "");
            guard++;
        }
        return "";
    }

    private Map<String, Object> subjectResult(Long id, String subjectCode, String subjectName, Integer status) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("subjectCode", subjectCode);
        result.put("subjectName", subjectName);
        result.put("status", status);
        return result;
    }

    private Map<String, Object> approvalResult(Long id, Integer status, Object currentApproverId, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("status", status);
        result.put("currentApproverId", currentApproverId);
        result.put("message", message);
        return result;
    }

    private List<Map<String, Object>> listFinanceGovernanceApprovalsData(Long tenantId, int limit) {
        return jdbcTemplate.queryForList(
                "SELECT * FROM finance_approval_action WHERE tenant_id = ? AND is_deleted = 0 AND " +
                        "business_type IN ('PERIOD_REOPEN','VOUCHER_REVERSE') ORDER BY update_time DESC, create_time DESC LIMIT ?",
                tenantId,
                Math.max(1, Math.min(limit, 500))
        );
    }

    private Map<String, Object> governanceApprovalResult(Long actionId, Long businessId, String businessType,
                                                         String action, Integer statusAfter, String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("actionId", actionId);
        result.put("id", actionId);
        result.put("businessId", businessId);
        result.put("businessType", businessType);
        result.put("action", action);
        result.put("statusAfter", statusAfter);
        result.put("message", message);
        return result;
    }

    private Map<String, Object> findPendingGovernanceAction(Long tenantId, Long actionId, String businessType,
                                                            Long businessId, String requestAction) {
        StringBuilder sql = new StringBuilder("SELECT * FROM finance_approval_action WHERE tenant_id = ? AND is_deleted = 0 AND status_after = 0");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        if (actionId != null) {
            sql.append(" AND id = ?");
            args.add(actionId);
        }
        if (StringUtils.hasText(businessType)) {
            sql.append(" AND business_type = ?");
            args.add(businessType);
        }
        if (businessId != null) {
            sql.append(" AND business_id = ?");
            args.add(businessId);
        }
        if (StringUtils.hasText(requestAction)) {
            sql.append(" AND action = ?");
            args.add(requestAction);
        }
        sql.append(" ORDER BY update_time DESC, create_time DESC LIMIT 1");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), args.toArray());
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Long insertApprovalActionWithKey(String businessType, Long businessId, String action, Object fromApproverId,
                                             Object toApproverId, String opinion, Integer statusAfter) {
        return insertWithKey(
                "INSERT INTO finance_approval_action (tenant_id,business_type,business_id,action,from_approver_id,to_approver_id,opinion,status_after,operator_id) VALUES (?,?,?,?,?,?,?,?,?)",
                resolveTenantId(),
                businessType,
                businessId,
                action,
                fromApproverId,
                toApproverId,
                opinion,
                statusAfter,
                UserContext.getUser()
        );
    }

    private void markGovernanceActionApproved(Long tenantId, Long actionId, String opinion) {
        jdbcTemplate.update(
                "UPDATE finance_approval_action SET status_after = 1, opinion = ?, operator_id = ?, update_time = NOW() WHERE tenant_id = ? AND id = ? AND is_deleted = 0",
                opinion,
                UserContext.getUser(),
                tenantId,
                actionId
        );
    }

    private void insertApprovalAction(String businessType, Long businessId, String action, Object fromApproverId,
                                      Object toApproverId, String opinion, Integer statusAfter) {
        jdbcTemplate.update(
                "INSERT INTO finance_approval_action (tenant_id,business_type,business_id,action,from_approver_id,to_approver_id,opinion,status_after,operator_id) VALUES (?,?,?,?,?,?,?,?,?)",
                resolveTenantId(),
                businessType,
                businessId,
                action,
                fromApproverId,
                toApproverId,
                opinion,
                statusAfter,
                UserContext.getUser()
        );
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private Long insertWithKey(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps;
        }, keyHolder);
        Number key = null;
        if (!keyHolder.getKeyList().isEmpty()) {
            Map<String, Object> generatedKeys = keyHolder.getKeyList().get(0);
            Object raw = generatedKeys.get("GENERATED_KEY");
            if (!(raw instanceof Number)) {
                raw = generatedKeys.get("ID");
            }
            if (!(raw instanceof Number)) {
                raw = generatedKeys.get("id");
            }
            if (!(raw instanceof Number) && generatedKeys.size() == 1) {
                raw = generatedKeys.values().iterator().next();
            }
            if (!(raw instanceof Number)) {
                raw = generatedKeys.values().stream()
                        .filter(Number.class::isInstance)
                        .findFirst()
                        .orElse(null);
            }
            if (raw instanceof Number) {
                key = (Number) raw;
            }
        }
        if (key instanceof BigInteger) {
            return ((BigInteger) key).longValue();
        }
        return key == null ? 0L : key.longValue();
    }

    private void insertFinanceAuditLog(String businessType, Long businessId, String action, Object beforeValue,
                                       Object afterValue, String opinion, String riskLevel, String result) {
        jdbcTemplate.update(
                "INSERT INTO finance_audit_log (tenant_id,business_type,business_id,action,before_value,after_value,operator_id,opinion,risk_level,result) VALUES (?,?,?,?,?,?,?,?,?,?)",
                resolveTenantId(),
                businessType,
                businessId,
                action,
                beforeValue == null ? null : String.valueOf(beforeValue),
                afterValue == null ? null : String.valueOf(afterValue),
                UserContext.getUser(),
                opinion,
                riskLevel,
                result
        );
    }

    private void insertReverseVoucherEntries(Long tenantId, Long originalVoucherId, Long reverseVoucherId, String reason) {
        List<Map<String, Object>> entries = jdbcTemplate.queryForList(
                "SELECT * FROM finance_voucher_entry WHERE tenant_id = ? AND voucher_id = ? AND is_deleted = 0 ORDER BY id ASC",
                tenantId,
                originalVoucherId
        );
        if (entries.isEmpty()) {
            Map<String, Object> original = findTenantRow("finance_voucher", tenantId, originalVoucherId);
            jdbcTemplate.update(
                    "INSERT INTO finance_voucher_entry (tenant_id,voucher_id,direction,subject_code,subject_name,amount,summary) VALUES (?,?,?,?,?,?,?)",
                    tenantId,
                    reverseVoucherId,
                    "REVERSE",
                    "",
                    "红字冲销",
                    money(first(original, "amount")).abs().negate(),
                    "冲销原凭证 #" + originalVoucherId + "：" + reason
            );
            return;
        }
        for (Map<String, Object> entry : entries) {
            String direction = text(first(entry, "direction"), "");
            String reverseDirection = "DEBIT".equalsIgnoreCase(direction) ? "CREDIT" : "CREDIT".equalsIgnoreCase(direction) ? "DEBIT" : "REVERSE";
            jdbcTemplate.update(
                    "INSERT INTO finance_voucher_entry (tenant_id,voucher_id,direction,subject_code,subject_name,amount,summary) VALUES (?,?,?,?,?,?,?)",
                    tenantId,
                    reverseVoucherId,
                    reverseDirection,
                    text(first(entry, "subject_code", "subjectCode"), ""),
                    text(first(entry, "subject_name", "subjectName"), ""),
                    money(first(entry, "amount")).abs().negate(),
                    "冲销原分录 #" + first(entry, "id") + "：" + reason
            );
        }
    }

    private void insertFinanceLedgerEntry(String ledgerType, String documentNo, String businessType, Long businessId,
                                          BigDecimal debitAmount, BigDecimal creditAmount, String period, String summary) {
        jdbcTemplate.update(
                "INSERT INTO finance_ledger (tenant_id,ledger_type,document_no,business_type,business_id,debit_amount,credit_amount,balance_amount,period,summary,operator_id) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                resolveTenantId(),
                ledgerType,
                documentNo,
                businessType,
                businessId,
                safe(debitAmount),
                safe(creditAmount),
                safe(debitAmount).subtract(safe(creditAmount)),
                StringUtils.hasText(period) ? period : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                summary,
                UserContext.getUser()
        );
    }

    private String requireFinanceTable(String tableName) {
        if (!tableName.matches("finance_(budget|budget_item|budget_ledger|allocation|reimbursement|reimbursement_item|voucher|voucher_entry|ledger|attachment|audit_log|period_close|reconciliation|subject|approval_action|approval_template|approval_template_node|record)")) {
            throw new IllegalArgumentException("invalid finance table");
        }
        return tableName;
    }

    private String requireFinanceColumn(String columnName) {
        if (!columnName.matches("[a-z_]+")) {
            throw new IllegalArgumentException("invalid finance column");
        }
        return columnName;
    }

    private BigDecimal money(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal nullableMoney(Object value) {
        if (value == null || !StringUtils.hasText(String.valueOf(value))) {
            return null;
        }
        return money(value);
    }

    private Integer number(Object value, Integer fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Long longValue(Object value, Long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private String text(Object value, String fallback) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String csv(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        String escaped = text.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String safeFileName(String value) {
        return String.valueOf(value).replaceAll("[^0-9A-Za-z_-]", "_");
    }

    private String docNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
