package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureCreditSchema() {
        ensureCreditTypeTable();
        ensureUserCreditRecordTable();
        ensureUserCreditSummaryTable();
        seedCreditTypes();
    }

    private void ensureCreditTypeTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'credit_type'",
                Integer.class
        );
        if (count != null && count > 0) {
            ensureColumn("credit_type", "creator",
                    "ALTER TABLE credit_type ADD COLUMN creator BIGINT DEFAULT NULL AFTER update_time");
            ensureColumn("credit_type", "updater",
                    "ALTER TABLE credit_type ADD COLUMN updater BIGINT DEFAULT NULL AFTER creator");
            return;
        }
        log.info("credit_type table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE credit_type (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "name VARCHAR(50) NOT NULL," +
                        "code VARCHAR(30) NOT NULL," +
                        "description VARCHAR(255) DEFAULT ''," +
                        "max_credit DECIMAL(8,1) DEFAULT 0.0," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_code_tenant (code, tenant_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='credit type'"
        );
    }

    private void ensureUserCreditRecordTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_credit_record'",
                Integer.class
        );
        if (count != null && count > 0) {
            ensureColumn("user_credit_record", "creator",
                    "ALTER TABLE user_credit_record ADD COLUMN creator BIGINT DEFAULT NULL AFTER update_time");
            ensureColumn("user_credit_record", "updater",
                    "ALTER TABLE user_credit_record ADD COLUMN updater BIGINT DEFAULT NULL AFTER creator");
            return;
        }
        log.info("user_credit_record table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE user_credit_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "user_id BIGINT NOT NULL," +
                        "source_type VARCHAR(64) DEFAULT NULL," +
                        "source_id BIGINT DEFAULT NULL," +
                        "credit_type_code VARCHAR(30) DEFAULT NULL," +
                        "credit_hours DECIMAL(8,1) DEFAULT 0.0," +
                        "credit_score DECIMAL(8,1) DEFAULT 0.0," +
                        "description VARCHAR(255) DEFAULT NULL," +
                        "status INT NOT NULL DEFAULT 0," +
                        "verifier_id BIGINT DEFAULT NULL," +
                        "verify_time DATETIME DEFAULT NULL," +
                        "reject_reason VARCHAR(255) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_user_credit_record_user (user_id)," +
                        "KEY idx_user_credit_record_tenant (tenant_id)," +
                        "KEY idx_user_credit_record_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user credit record'"
        );
    }

    private void ensureUserCreditSummaryTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_credit_summary'",
                Integer.class
        );
        if (count != null && count > 0) {
            ensureColumn("user_credit_summary", "create_time",
                    "ALTER TABLE user_credit_summary ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP AFTER last_updated");
            ensureColumn("user_credit_summary", "update_time",
                    "ALTER TABLE user_credit_summary ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time");
            ensureColumn("user_credit_summary", "creator",
                    "ALTER TABLE user_credit_summary ADD COLUMN creator BIGINT DEFAULT NULL AFTER update_time");
            ensureColumn("user_credit_summary", "updater",
                    "ALTER TABLE user_credit_summary ADD COLUMN updater BIGINT DEFAULT NULL AFTER creator");
            return;
        }
        log.info("user_credit_summary table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE user_credit_summary (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "user_id BIGINT NOT NULL," +
                        "total_credits DECIMAL(8,1) DEFAULT 0.0," +
                        "total_hours DECIMAL(8,1) DEFAULT 0.0," +
                        "volunteer_hours DECIMAL(8,1) DEFAULT 0.0," +
                        "activity_count INT DEFAULT 0," +
                        "competition_awards INT DEFAULT 0," +
                        "last_updated DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_user_credit_summary_user_tenant (user_id, tenant_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='user credit summary'"
        );
    }

    private void seedCreditTypes() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM credit_type WHERE tenant_id = 1 AND is_deleted = 0",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("credit_type empty, seeding defaults");
        jdbcTemplate.execute(
                "INSERT INTO credit_type (name, code, description, max_credit, tenant_id, is_deleted) VALUES " +
                        "('Activity Credit', 'activity', 'activity participation credit', 10.0, 1, 0)," +
                        "('Competition Credit', 'competition', 'competition credit', 20.0, 1, 0)," +
                        "('Volunteer Service', 'volunteer', 'volunteer service credit', 20.0, 1, 0)," +
                        "('Practice Credit', 'practice', 'practice credit', 15.0, 1, 0)"
        );
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
}
