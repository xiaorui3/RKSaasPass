package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureNotificationTables() {
        ensureTable("rk_notification",
                "CREATE TABLE rk_notification (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "title VARCHAR(255) NOT NULL," +
                        "content TEXT," +
                        "type VARCHAR(64)," +
                        "priority INT DEFAULT 0," +
                        "sender_id BIGINT," +
                        "sender_name VARCHAR(128)," +
                        "target_type INT DEFAULT 0," +
                        "target_ids VARCHAR(1000)," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "creator BIGINT," +
                        "updater BIGINT," +
                        "is_deleted TINYINT DEFAULT 0," +
                        "KEY idx_rk_notification_tenant (tenant_id)," +
                        "KEY idx_rk_notification_type (type)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='notification table'");

        ensureTable("rk_notification_read",
                "CREATE TABLE rk_notification_read (" +
                        "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "notification_id BIGINT NOT NULL," +
                        "user_id BIGINT NOT NULL," +
                        "read_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "KEY idx_rk_notification_read_notification (notification_id)," +
                        "KEY idx_rk_notification_read_user (user_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='notification read table'");

        ensureColumn("rk_notification", "metadata",
                "ALTER TABLE rk_notification ADD COLUMN metadata TEXT NULL AFTER target_ids");
    }

    private void ensureTable(String tableName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                tableName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("Table {} missing, creating automatically", tableName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("{}.{} missing, adding automatically", tableName, columnName);
        jdbcTemplate.execute(ddl);
    }
}
