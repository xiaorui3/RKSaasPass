package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureLogTables() {
        ensureTable("sys_oper_log",
                "CREATE TABLE sys_oper_log (" +
                        "oper_id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "title VARCHAR(255)," +
                        "business_type INT DEFAULT 0," +
                        "method VARCHAR(255)," +
                        "request_method VARCHAR(16)," +
                        "operator_type INT DEFAULT 0," +
                        "oper_name VARCHAR(128)," +
                        "dept_name VARCHAR(128)," +
                        "oper_url VARCHAR(500)," +
                        "oper_ip VARCHAR(64)," +
                        "oper_location VARCHAR(255)," +
                        "oper_param TEXT," +
                        "json_result TEXT," +
                        "status INT DEFAULT 0," +
                        "error_msg TEXT," +
                        "oper_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "cost_time BIGINT DEFAULT 0" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='operation log table'");

        ensureTable("sys_logininfor",
                "CREATE TABLE sys_logininfor (" +
                        "info_id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                        "login_name VARCHAR(128)," +
                        "ipaddr VARCHAR(64)," +
                        "login_location VARCHAR(255)," +
                        "browser VARCHAR(255)," +
                        "os VARCHAR(255)," +
                        "status VARCHAR(32)," +
                        "msg VARCHAR(500)," +
                        "login_time DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='login log table'");

        ensureVarcharColumnLength("sys_oper_log", "title", 255);
        ensureVarcharColumnLength("sys_oper_log", "method", 255);
        ensureVarcharColumnLength("sys_oper_log", "request_method", 16);
        ensureVarcharColumnLength("sys_oper_log", "oper_name", 128);
        ensureVarcharColumnLength("sys_oper_log", "oper_url", 500);
        ensureVarcharColumnLength("sys_oper_log", "oper_ip", 64);
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

    private void ensureVarcharColumnLength(String tableName, String columnName, int expectedLength) {
        Integer currentLength = jdbcTemplate.query(
                "SELECT character_maximum_length FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                rs -> rs.next() ? rs.getInt(1) : null,
                tableName,
                columnName
        );
        if (currentLength == null || currentLength >= expectedLength) {
            return;
        }
        log.info("Altering {}.{} length from {} to {}", tableName, columnName, currentLength, expectedLength);
        jdbcTemplate.execute(String.format(
                "ALTER TABLE %s MODIFY COLUMN %s VARCHAR(%d)",
                tableName,
                columnName,
                expectedLength
        ));
    }
}
