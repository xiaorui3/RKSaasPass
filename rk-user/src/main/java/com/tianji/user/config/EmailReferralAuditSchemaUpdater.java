package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailReferralAuditSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEmailReferralAuditSchema() {
        ensureInvitationColumn("referral_code", "ALTER TABLE email_invitation ADD COLUMN referral_code VARCHAR(64) NULL COMMENT 'binding referral code' AFTER invite_token");
        ensureInvitationColumn("referral_code_id", "ALTER TABLE email_invitation ADD COLUMN referral_code_id BIGINT NULL COMMENT 'binding referral code id' AFTER referral_code");
        ensureInvitationColumn("open_time", "ALTER TABLE email_invitation ADD COLUMN open_time DATETIME NULL COMMENT 'invitation open time' AFTER referral_code_id");
        ensureInvitationColumn("register_submit_time", "ALTER TABLE email_invitation ADD COLUMN register_submit_time DATETIME NULL COMMENT 'register submit time' AFTER open_time");
        ensureInvitationColumn("register_success_time", "ALTER TABLE email_invitation ADD COLUMN register_success_time DATETIME NULL COMMENT 'register success time' AFTER register_submit_time");
        ensureInvitationColumn("join_submit_time", "ALTER TABLE email_invitation ADD COLUMN join_submit_time DATETIME NULL COMMENT 'join submit time' AFTER register_success_time");
        ensureInvitationColumn("join_approved_time", "ALTER TABLE email_invitation ADD COLUMN join_approved_time DATETIME NULL COMMENT 'join approved time' AFTER join_submit_time");
        ensureInvitationColumn("conversion_status", "ALTER TABLE email_invitation ADD COLUMN conversion_status VARCHAR(32) NULL COMMENT 'conversion status' AFTER join_approved_time");
        ensureInvitationColumn("conversion_user_id", "ALTER TABLE email_invitation ADD COLUMN conversion_user_id BIGINT NULL COMMENT 'converted auth user id' AFTER conversion_status");
        ensureInvitationColumn("conversion_join_request_id", "ALTER TABLE email_invitation ADD COLUMN conversion_join_request_id BIGINT NULL COMMENT 'converted join request id' AFTER conversion_user_id");
        ensureInvitationEventTable();
        ensureReferralConversionRecordTable();
    }

    private void ensureInvitationColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_invitation' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("email_invitation missing column {}, adding automatically", columnName);
        jdbcTemplate.execute(ddl);
    }

    private void ensureInvitationEventTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_invitation_event'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("email_invitation_event table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE email_invitation_event (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "invitation_id BIGINT NOT NULL," +
                        "invite_token VARCHAR(64) NOT NULL," +
                        "referral_code VARCHAR(64) DEFAULT NULL," +
                        "event_type VARCHAR(32) NOT NULL," +
                        "event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                        "actor_email VARCHAR(100) DEFAULT NULL," +
                        "actor_user_id BIGINT DEFAULT NULL," +
                        "join_request_id BIGINT DEFAULT NULL," +
                        "remark VARCHAR(255) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_email_invitation_event_invitation (invitation_id)," +
                        "KEY idx_email_invitation_event_token (invite_token)," +
                        "KEY idx_email_invitation_event_type (event_type)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='email invitation event table'"
        );
    }

    private void ensureReferralConversionRecordTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'referral_conversion_record'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("referral_conversion_record table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE referral_conversion_record (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "referral_code_id BIGINT DEFAULT NULL," +
                        "referral_code VARCHAR(64) NOT NULL," +
                        "invitation_id BIGINT DEFAULT NULL," +
                        "invite_token VARCHAR(64) DEFAULT NULL," +
                        "target_email VARCHAR(100) DEFAULT NULL," +
                        "conversion_type VARCHAR(32) NOT NULL," +
                        "conversion_status VARCHAR(32) NOT NULL," +
                        "auth_user_id BIGINT DEFAULT NULL," +
                        "join_request_id BIGINT DEFAULT NULL," +
                        "converted_time DATETIME DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_referral_conversion_code (referral_code)," +
                        "KEY idx_referral_conversion_token (invite_token)," +
                        "KEY idx_referral_conversion_status (conversion_status)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='referral conversion record table'"
        );
    }
}
