package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdmissionSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void updateJoinRequestSchema() {
        ensureColumn("grade", "ALTER TABLE join_requests ADD COLUMN grade VARCHAR(20) NULL COMMENT 'grade' AFTER major");
        ensureColumn("username", "ALTER TABLE join_requests ADD COLUMN username VARCHAR(50) NULL COMMENT 'login username' AFTER email");
        ensureColumn("invite_token", "ALTER TABLE join_requests ADD COLUMN invite_token VARCHAR(64) NULL COMMENT 'invite token' AFTER email");
        ensureColumn("referral_code", "ALTER TABLE join_requests ADD COLUMN referral_code VARCHAR(64) NULL COMMENT 'referral code' AFTER invite_token");
        ensureColumn("password_hash", "ALTER TABLE join_requests ADD COLUMN password_hash VARCHAR(100) NULL COMMENT 'password hash' AFTER username");
        ensureColumn("auth_user_id", "ALTER TABLE join_requests ADD COLUMN auth_user_id BIGINT NULL COMMENT 'approved auth user id' AFTER password_hash");
        ensureColumn("source_auth_user_id", "ALTER TABLE join_requests ADD COLUMN source_auth_user_id BIGINT NULL COMMENT 'source auth user id' AFTER auth_user_id");
    }

    private void ensureColumn(String columnName, String ddl) {
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
}
