package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailSendAuditSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEmailSendAuditSchema() {
        ensureTable("email_send_task",
                "CREATE TABLE email_send_task (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "sender_user_id BIGINT DEFAULT NULL," +
                        "sender_role_id BIGINT DEFAULT NULL," +
                        "send_type VARCHAR(32) NOT NULL," +
                        "recipient_mode VARCHAR(32) NOT NULL," +
                        "subject VARCHAR(255) NOT NULL," +
                        "content LONGTEXT," +
                        "html TINYINT(1) NOT NULL DEFAULT 0," +
                        "status VARCHAR(32) NOT NULL," +
                        "queued_count INT NOT NULL DEFAULT 0," +
                        "success_count INT NOT NULL DEFAULT 0," +
                        "fail_count INT NOT NULL DEFAULT 0," +
                        "finished_time DATETIME DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_email_send_task_tenant (tenant_id)," +
                        "KEY idx_email_send_task_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='email send task table'");

        ensureTable("email_send_recipient",
                "CREATE TABLE email_send_recipient (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "task_id BIGINT NOT NULL," +
                        "recipient_email VARCHAR(100) NOT NULL," +
                        "recipient_name VARCHAR(100) DEFAULT NULL," +
                        "recipient_user_id BIGINT DEFAULT NULL," +
                        "source_type VARCHAR(32) DEFAULT NULL," +
                        "source_ref_id BIGINT DEFAULT NULL," +
                        "send_status VARCHAR(32) NOT NULL DEFAULT 'QUEUED'," +
                        "error_message VARCHAR(255) DEFAULT NULL," +
                        "provider_message_id VARCHAR(128) DEFAULT NULL," +
                        "queued_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "sent_time DATETIME DEFAULT NULL," +
                        "last_attempt_time DATETIME DEFAULT NULL," +
                        "retry_count INT NOT NULL DEFAULT 0," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_email_send_recipient_task (task_id)," +
                        "KEY idx_email_send_recipient_status (send_status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='email send recipient table'");
    }

    private void ensureTable(String tableName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                tableName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("{} table missing, creating automatically", tableName);
        jdbcTemplate.execute(ddl);
    }
}
