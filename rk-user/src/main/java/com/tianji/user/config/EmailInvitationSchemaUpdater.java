package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailInvitationSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEmailInvitationTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'email_invitation'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        log.info("email_invitation table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE email_invitation (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "sender_user_id BIGINT DEFAULT NULL," +
                        "sender_role_id BIGINT DEFAULT NULL," +
                        "target_email VARCHAR(100) NOT NULL," +
                        "invitation_type VARCHAR(32) NOT NULL," +
                        "invite_token VARCHAR(64) NOT NULL," +
                        "status VARCHAR(32) NOT NULL DEFAULT 'SENT'," +
                        "accepted TINYINT(1) NOT NULL DEFAULT 0," +
                        "accepted_time DATETIME DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_email_invitation_token (invite_token)," +
                        "KEY idx_email_invitation_tenant (tenant_id)," +
                        "KEY idx_email_invitation_email (target_email)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮件邀请记录表'"
        );
    }
}
