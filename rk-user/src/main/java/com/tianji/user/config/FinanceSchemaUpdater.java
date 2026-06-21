package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinanceSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureFinanceSchema() {
        try {
            ensureFinanceRecordTable();
            ensureFinanceAccountTable();
            ensureEnterpriseFinanceTables();
        } catch (Exception e) {
            log.warn("finance schema updater skipped: {}", e.getMessage());
        }
    }

    private void ensureEnterpriseFinanceTables() {
        ensureTable("finance_budget",
                "CREATE TABLE finance_budget (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "budget_no VARCHAR(64) DEFAULT NULL," +
                        "budget_name VARCHAR(128) NOT NULL," +
                        "budget_type VARCHAR(64) DEFAULT NULL," +
                        "period VARCHAR(20) DEFAULT NULL," +
                        "total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "occupied_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "used_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "available_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "status INT NOT NULL DEFAULT 1," +
                        "owner_name VARCHAR(80) DEFAULT NULL," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_budget_tenant_period (tenant_id, period)," +
                        "KEY idx_finance_budget_no (tenant_id, budget_no)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance budget'");
        ensureTable("finance_budget_item",
                "CREATE TABLE finance_budget_item (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "budget_id BIGINT DEFAULT NULL," +
                        "item_name VARCHAR(128) NOT NULL," +
                        "subject_code VARCHAR(64) DEFAULT NULL," +
                        "allocated_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "occupied_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "used_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "status INT NOT NULL DEFAULT 1," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_budget_item_budget (tenant_id, budget_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance budget item'");
        ensureTable("finance_budget_ledger",
                "CREATE TABLE finance_budget_ledger (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "budget_id BIGINT DEFAULT NULL," +
                        "business_type VARCHAR(64) DEFAULT NULL," +
                        "business_id BIGINT DEFAULT NULL," +
                        "change_type VARCHAR(64) DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "before_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "after_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_budget_ledger_budget (tenant_id, budget_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance budget ledger'");
        ensureTable("finance_allocation",
                "CREATE TABLE finance_allocation (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "allocation_no VARCHAR(64) DEFAULT NULL," +
                        "source_type VARCHAR(64) DEFAULT NULL," +
                        "source_name VARCHAR(128) DEFAULT NULL," +
                        "receiver_tenant_id BIGINT DEFAULT NULL," +
                        "budget_id BIGINT DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "arrival_status INT NOT NULL DEFAULT 0," +
                        "arrival_time DATETIME DEFAULT NULL," +
                        "status INT NOT NULL DEFAULT 0," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_allocation_tenant (tenant_id, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance allocation'");
        ensureTable("finance_reimbursement",
                "CREATE TABLE finance_reimbursement (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "reimbursement_no VARCHAR(64) DEFAULT NULL," +
                        "applicant_id BIGINT DEFAULT NULL," +
                        "applicant_name VARCHAR(80) DEFAULT NULL," +
                        "linked_activity_id BIGINT DEFAULT NULL," +
                        "expense_subject VARCHAR(128) DEFAULT NULL," +
                        "budget_id BIGINT DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "invoice_count INT NOT NULL DEFAULT 0," +
                        "status INT NOT NULL DEFAULT 0," +
                        "current_approver_id BIGINT DEFAULT NULL," +
                        "approval_template_id BIGINT DEFAULT NULL," +
                        "current_step_no INT NOT NULL DEFAULT 0," +
                        "reject_reason VARCHAR(255) DEFAULT NULL," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_reimbursement_tenant (tenant_id, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance reimbursement'");
        ensureTable("finance_reimbursement_item",
                "CREATE TABLE finance_reimbursement_item (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "reimbursement_id BIGINT DEFAULT NULL," +
                        "item_name VARCHAR(128) DEFAULT NULL," +
                        "expense_subject VARCHAR(128) DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "invoice_no VARCHAR(80) DEFAULT NULL," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_reimbursement_item (tenant_id, reimbursement_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance reimbursement item'");
        ensureTable("finance_voucher",
                "CREATE TABLE finance_voucher (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "voucher_no VARCHAR(64) DEFAULT NULL," +
                        "period VARCHAR(20) DEFAULT NULL," +
                        "source_type VARCHAR(64) DEFAULT NULL," +
                        "source_id BIGINT DEFAULT NULL," +
                        "summary VARCHAR(255) DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "posting_status INT NOT NULL DEFAULT 0," +
                        "reverse_voucher_id BIGINT DEFAULT NULL," +
                        "original_voucher_id BIGINT DEFAULT NULL," +
                        "reverse_reason VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_voucher_tenant_period (tenant_id, period)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance voucher'");
        ensureTable("finance_voucher_entry",
                "CREATE TABLE finance_voucher_entry (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "voucher_id BIGINT DEFAULT NULL," +
                        "direction VARCHAR(16) DEFAULT NULL," +
                        "subject_code VARCHAR(64) DEFAULT NULL," +
                        "subject_name VARCHAR(128) DEFAULT NULL," +
                        "amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "summary VARCHAR(255) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_voucher_entry (tenant_id, voucher_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance voucher entry'");
        ensureTable("finance_ledger",
                "CREATE TABLE finance_ledger (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "ledger_type VARCHAR(64) DEFAULT NULL," +
                        "document_no VARCHAR(64) DEFAULT NULL," +
                        "business_type VARCHAR(64) DEFAULT NULL," +
                        "business_id BIGINT DEFAULT NULL," +
                        "debit_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "credit_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "balance_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "period VARCHAR(20) DEFAULT NULL," +
                        "summary VARCHAR(255) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_ledger_tenant_period (tenant_id, period)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance ledger'");
        ensureTable("finance_attachment",
                "CREATE TABLE finance_attachment (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "business_type VARCHAR(64) DEFAULT NULL," +
                        "business_id BIGINT DEFAULT NULL," +
                        "file_name VARCHAR(255) DEFAULT NULL," +
                        "file_url VARCHAR(500) DEFAULT NULL," +
                        "file_type VARCHAR(64) DEFAULT NULL," +
                        "invoice_no VARCHAR(80) DEFAULT NULL," +
                        "invoice_type VARCHAR(64) DEFAULT NULL," +
                        "verify_status INT NOT NULL DEFAULT 0," +
                        "archive_status INT NOT NULL DEFAULT 0," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_attachment_business (tenant_id, business_type, business_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance attachment'");
        ensureTable("finance_audit_log",
                "CREATE TABLE finance_audit_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "business_type VARCHAR(64) DEFAULT NULL," +
                        "business_id BIGINT DEFAULT NULL," +
                        "action VARCHAR(64) DEFAULT NULL," +
                        "before_value TEXT DEFAULT NULL," +
                        "after_value TEXT DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "operator_name VARCHAR(80) DEFAULT NULL," +
                        "opinion VARCHAR(500) DEFAULT NULL," +
                        "risk_level VARCHAR(32) DEFAULT NULL," +
                        "result VARCHAR(255) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_audit_business (tenant_id, business_type, business_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance audit log'");
        ensureTable("finance_period_close",
                "CREATE TABLE finance_period_close (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "period VARCHAR(20) NOT NULL," +
                        "close_type VARCHAR(64) DEFAULT NULL," +
                        "status INT NOT NULL DEFAULT 1," +
                        "income_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "expense_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "voucher_count INT NOT NULL DEFAULT 0," +
                        "ledger_count INT NOT NULL DEFAULT 0," +
                        "pending_count INT NOT NULL DEFAULT 0," +
                        "closed_by BIGINT DEFAULT NULL," +
                        "closed_time DATETIME DEFAULT NULL," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_finance_period_close (tenant_id, period)," +
                        "KEY idx_finance_period_status (tenant_id, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance period close'");
        ensureTable("finance_reconciliation",
                "CREATE TABLE finance_reconciliation (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "reconciliation_no VARCHAR(64) DEFAULT NULL," +
                        "period VARCHAR(20) NOT NULL," +
                        "account_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "ledger_balance DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "difference_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "status INT NOT NULL DEFAULT 0," +
                        "handler_id BIGINT DEFAULT NULL," +
                        "handler_name VARCHAR(80) DEFAULT NULL," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_reconciliation_period (tenant_id, period)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance reconciliation'");
        ensureTable("finance_subject",
                "CREATE TABLE finance_subject (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "subject_code VARCHAR(64) NOT NULL," +
                        "subject_name VARCHAR(128) NOT NULL," +
                        "subject_type VARCHAR(64) DEFAULT NULL," +
                        "parent_code VARCHAR(64) DEFAULT NULL," +
                        "direction VARCHAR(16) DEFAULT NULL," +
                        "status INT NOT NULL DEFAULT 1," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_finance_subject_code (tenant_id, subject_code)," +
                        "KEY idx_finance_subject_type (tenant_id, subject_type, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance subject'");
        ensureTable("finance_approval_action",
                "CREATE TABLE finance_approval_action (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "business_type VARCHAR(64) DEFAULT NULL," +
                        "business_id BIGINT DEFAULT NULL," +
                        "action VARCHAR(64) DEFAULT NULL," +
                        "from_approver_id BIGINT DEFAULT NULL," +
                        "to_approver_id BIGINT DEFAULT NULL," +
                        "opinion VARCHAR(500) DEFAULT NULL," +
                        "status_after INT DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_approval_business (tenant_id, business_type, business_id)," +
                        "KEY idx_finance_approval_action (tenant_id, action)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance approval action'");
        ensureTable("finance_approval_template",
                "CREATE TABLE finance_approval_template (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "template_name VARCHAR(128) NOT NULL," +
                        "business_type VARCHAR(64) NOT NULL DEFAULT 'REIMBURSEMENT'," +
                        "min_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00," +
                        "max_amount DECIMAL(14,2) DEFAULT NULL," +
                        "status INT NOT NULL DEFAULT 0," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_approval_template_tenant (tenant_id, business_type, status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance approval template'");
        ensureTable("finance_approval_template_node",
                "CREATE TABLE finance_approval_template_node (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "template_id BIGINT NOT NULL," +
                        "step_no INT NOT NULL DEFAULT 1," +
                        "node_name VARCHAR(128) DEFAULT NULL," +
                        "approver_id BIGINT DEFAULT NULL," +
                        "approver_name VARCHAR(80) DEFAULT NULL," +
                        "approver_role VARCHAR(80) DEFAULT NULL," +
                        "approve_type VARCHAR(32) NOT NULL DEFAULT 'ANY'," +
                        "required_flag TINYINT NOT NULL DEFAULT 1," +
                        "amount_limit DECIMAL(14,2) DEFAULT NULL," +
                        "sort_no INT NOT NULL DEFAULT 0," +
                        "remark VARCHAR(500) DEFAULT NULL," +
                        "operator_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_finance_approval_node_template (tenant_id, template_id, step_no)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance approval template node'");
        ensureColumn("finance_voucher", "original_voucher_id", "ALTER TABLE finance_voucher ADD COLUMN original_voucher_id BIGINT DEFAULT NULL AFTER reverse_voucher_id");
        ensureColumn("finance_voucher", "reverse_reason", "ALTER TABLE finance_voucher ADD COLUMN reverse_reason VARCHAR(500) DEFAULT NULL AFTER original_voucher_id");
        ensureColumn("finance_reimbursement", "approval_template_id", "ALTER TABLE finance_reimbursement ADD COLUMN approval_template_id BIGINT DEFAULT NULL AFTER current_approver_id");
        ensureColumn("finance_reimbursement", "current_step_no", "ALTER TABLE finance_reimbursement ADD COLUMN current_step_no INT NOT NULL DEFAULT 0 AFTER approval_template_id");
    }

    private void ensureFinanceRecordTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_record'",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(
                    "CREATE TABLE finance_record (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT," +
                            "tenant_id BIGINT NOT NULL DEFAULT 1," +
                            "type INT NOT NULL," +
                            "amount DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                            "category VARCHAR(64) DEFAULT NULL," +
                            "record_no VARCHAR(64) DEFAULT NULL," +
                            "business_type VARCHAR(64) DEFAULT NULL," +
                            "business_id BIGINT DEFAULT NULL," +
                            "budget_item VARCHAR(128) DEFAULT NULL," +
                            "period VARCHAR(20) DEFAULT NULL," +
                            "title VARCHAR(120) DEFAULT NULL," +
                            "description VARCHAR(500) DEFAULT NULL," +
                            "proof_image_url VARCHAR(500) DEFAULT NULL," +
                            "operator_id BIGINT DEFAULT NULL," +
                            "reviewer_id BIGINT DEFAULT NULL," +
                            "reject_reason VARCHAR(255) DEFAULT NULL," +
                            "posted_status INT NOT NULL DEFAULT 0," +
                            "voucher_id BIGINT DEFAULT NULL," +
                            "status INT NOT NULL DEFAULT 0," +
                            "review_time DATETIME DEFAULT NULL," +
                            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "is_deleted TINYINT NOT NULL DEFAULT 0," +
                            "PRIMARY KEY (id)," +
                            "KEY idx_finance_record_tenant_status (tenant_id, status)," +
                            "KEY idx_finance_record_period (tenant_id, period)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance record'"
            );
            return;
        }
        ensureColumn("finance_record", "id", "ALTER TABLE finance_record ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
        ensureColumn("finance_record", "tenant_id", "ALTER TABLE finance_record ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id");
        ensureColumn("finance_record", "type", "ALTER TABLE finance_record ADD COLUMN type INT NOT NULL DEFAULT 1 AFTER tenant_id");
        ensureColumn("finance_record", "amount", "ALTER TABLE finance_record ADD COLUMN amount DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER type");
        ensureColumn("finance_record", "category", "ALTER TABLE finance_record ADD COLUMN category VARCHAR(64) DEFAULT NULL AFTER amount");
        ensureColumn("finance_record", "record_no", "ALTER TABLE finance_record ADD COLUMN record_no VARCHAR(64) DEFAULT NULL AFTER category");
        ensureColumn("finance_record", "business_type", "ALTER TABLE finance_record ADD COLUMN business_type VARCHAR(64) DEFAULT NULL AFTER record_no");
        ensureColumn("finance_record", "business_id", "ALTER TABLE finance_record ADD COLUMN business_id BIGINT DEFAULT NULL AFTER business_type");
        ensureColumn("finance_record", "budget_item", "ALTER TABLE finance_record ADD COLUMN budget_item VARCHAR(128) DEFAULT NULL AFTER business_id");
        ensureColumn("finance_record", "period", "ALTER TABLE finance_record ADD COLUMN period VARCHAR(20) DEFAULT NULL AFTER budget_item");
        ensureColumn("finance_record", "title", "ALTER TABLE finance_record ADD COLUMN title VARCHAR(120) DEFAULT NULL AFTER period");
        ensureColumn("finance_record", "description", "ALTER TABLE finance_record ADD COLUMN description VARCHAR(500) DEFAULT NULL AFTER title");
        ensureColumn("finance_record", "proof_image_url", "ALTER TABLE finance_record ADD COLUMN proof_image_url VARCHAR(500) DEFAULT NULL AFTER description");
        ensureColumn("finance_record", "operator_id", "ALTER TABLE finance_record ADD COLUMN operator_id BIGINT DEFAULT NULL AFTER proof_image_url");
        ensureColumn("finance_record", "reviewer_id", "ALTER TABLE finance_record ADD COLUMN reviewer_id BIGINT DEFAULT NULL AFTER operator_id");
        ensureColumn("finance_record", "reject_reason", "ALTER TABLE finance_record ADD COLUMN reject_reason VARCHAR(255) DEFAULT NULL AFTER reviewer_id");
        ensureColumn("finance_record", "posted_status", "ALTER TABLE finance_record ADD COLUMN posted_status INT NOT NULL DEFAULT 0 AFTER reject_reason");
        ensureColumn("finance_record", "voucher_id", "ALTER TABLE finance_record ADD COLUMN voucher_id BIGINT DEFAULT NULL AFTER posted_status");
        ensureColumn("finance_record", "status", "ALTER TABLE finance_record ADD COLUMN status INT NOT NULL DEFAULT 0 AFTER voucher_id");
        ensureColumn("finance_record", "review_time", "ALTER TABLE finance_record ADD COLUMN review_time DATETIME DEFAULT NULL AFTER status");
        ensureColumn("finance_record", "create_time", "ALTER TABLE finance_record ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP AFTER review_time");
        ensureColumn("finance_record", "update_time", "ALTER TABLE finance_record ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time");
        ensureColumn("finance_record", "is_deleted", "ALTER TABLE finance_record ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 AFTER update_time");
    }

    private void ensureFinanceAccountTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'finance_account'",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(
                    "CREATE TABLE finance_account (" +
                            "id BIGINT NOT NULL AUTO_INCREMENT," +
                            "tenant_id BIGINT NOT NULL DEFAULT 1," +
                            "balance DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                            "total_income DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                            "total_expense DECIMAL(12,2) NOT NULL DEFAULT 0.00," +
                            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "is_deleted TINYINT NOT NULL DEFAULT 0," +
                            "PRIMARY KEY (id)," +
                            "UNIQUE KEY uk_finance_account_tenant (tenant_id)" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='finance account'"
            );
            return;
        }
        ensureColumn("finance_account", "id", "ALTER TABLE finance_account ADD COLUMN id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
        ensureColumn("finance_account", "tenant_id", "ALTER TABLE finance_account ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id");
        ensureColumn("finance_account", "balance", "ALTER TABLE finance_account ADD COLUMN balance DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER tenant_id");
        ensureColumn("finance_account", "total_income", "ALTER TABLE finance_account ADD COLUMN total_income DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER balance");
        ensureColumn("finance_account", "total_expense", "ALTER TABLE finance_account ADD COLUMN total_expense DECIMAL(12,2) NOT NULL DEFAULT 0.00 AFTER total_income");
        ensureColumn("finance_account", "create_time", "ALTER TABLE finance_account ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP AFTER total_expense");
        ensureColumn("finance_account", "update_time", "ALTER TABLE finance_account ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time");
        ensureColumn("finance_account", "is_deleted", "ALTER TABLE finance_account ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 AFTER update_time");
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = '" + tableName + "' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            log.info("{}.{} missing, applying ddl", tableName, columnName);
            jdbcTemplate.execute(ddl);
        }
    }

    private void ensureTable(String tableName, String createSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (count == null || count == 0) {
            log.info("{} missing, creating enterprise finance table", tableName);
            jdbcTemplate.execute(createSql);
        }
    }
}
