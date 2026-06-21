package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterReviewSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureRegisterReviewSchema() {
        ensureJoinRequestColumn("form_payload_json",
                "ALTER TABLE join_requests ADD COLUMN form_payload_json LONGTEXT NULL COMMENT 'full submitted form payload json' AFTER source_auth_user_id");
        ensureRegisterReviewRequestTable();
    }

    private void ensureJoinRequestColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'join_requests' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("join_requests missing column {}, adding automatically", columnName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureRegisterReviewRequestTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'register_review_request'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("register_review_request table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE register_review_request (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "auth_user_id BIGINT DEFAULT NULL," +
                        "username VARCHAR(64) NOT NULL," +
                        "email VARCHAR(128) NOT NULL," +
                        "name VARCHAR(128) DEFAULT NULL," +
                        "referral_code VARCHAR(64) DEFAULT NULL," +
                        "review_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'," +
                        "review_comment VARCHAR(255) DEFAULT NULL," +
                        "reviewer_auth_user_id BIGINT DEFAULT NULL," +
                        "review_time DATETIME DEFAULT NULL," +
                        "form_payload_json LONGTEXT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_register_review_request_tenant (tenant_id)," +
                        "KEY idx_register_review_request_auth_user (auth_user_id)," +
                        "KEY idx_register_review_request_status (review_status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='register review request table'"
        );
    }
}
