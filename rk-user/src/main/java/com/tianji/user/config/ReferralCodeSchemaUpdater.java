package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReferralCodeSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureReferralCodeTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                "referral_code"
        );
        if (count != null && count > 0) {
            ensureCodeColumnLength();
            return;
        }
        log.info("referral_code table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE referral_code (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL," +
                        "code VARCHAR(64) NOT NULL," +
                        "generator_id BIGINT NOT NULL," +
                        "max_uses INT DEFAULT 1," +
                        "used_count INT DEFAULT 0," +
                        "expires_at DATETIME DEFAULT NULL," +
                        "status TINYINT DEFAULT 1," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_referral_code (code)," +
                        "KEY idx_referral_tenant_code (tenant_id, code)," +
                        "KEY idx_referral_tenant_generator (tenant_id, generator_id)," +
                        "KEY idx_referral_status (status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='referral code table'"
        );
    }

    private void ensureCodeColumnLength() {
        Integer currentLength = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_code' AND COLUMN_NAME = 'code'",
                Integer.class
        );
        if (currentLength != null && currentLength >= 64) {
            return;
        }
        log.info("expanding referral_code.code column to VARCHAR(64)");
        jdbcTemplate.execute("ALTER TABLE referral_code MODIFY COLUMN code VARCHAR(64) NOT NULL");
    }
}
