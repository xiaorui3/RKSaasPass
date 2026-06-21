package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlumniGraduationSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAlumniGraduationSchema() {
        ensureTable("alumni_profile_token",
                "CREATE TABLE alumni_profile_token (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "alumni_id BIGINT NOT NULL," +
                        "member_id BIGINT DEFAULT NULL," +
                        "email VARCHAR(100) NOT NULL," +
                        "token VARCHAR(64) NOT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'SENT'," +
                        "expires_at DATETIME DEFAULT NULL," +
                        "submitted_time DATETIME DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_alumni_profile_token (token)," +
                        "KEY idx_alumni_profile_alumni (alumni_id)," +
                        "KEY idx_alumni_profile_member (member_id)," +
                        "KEY idx_alumni_profile_tenant (tenant_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='alumni profile one-time token table'");

        ensureTable("alumni_graduation_log",
                "CREATE TABLE alumni_graduation_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "member_id BIGINT NOT NULL," +
                        "alumni_id BIGINT DEFAULT NULL," +
                        "student_id VARCHAR(50) DEFAULT NULL," +
                        "process_year INT NOT NULL," +
                        "action_type VARCHAR(32) NOT NULL," +
                        "old_grade VARCHAR(50) DEFAULT NULL," +
                        "new_grade VARCHAR(50) DEFAULT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'DONE'," +
                        "remark VARCHAR(255) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_alumni_graduation_member_year_action (tenant_id, member_id, process_year, action_type)," +
                        "KEY idx_alumni_graduation_tenant_year (tenant_id, process_year)," +
                        "KEY idx_alumni_graduation_student (student_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='alumni annual graduation idempotency log'");
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
