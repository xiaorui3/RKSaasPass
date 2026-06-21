package com.tianji.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailTemplateSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEmailTemplateSchema() {
        ensureColumn("email_subject", "ALTER TABLE rk_message_template ADD COLUMN email_subject VARCHAR(255) NULL COMMENT 'email subject' AFTER template_name");
        ensureColumn("html_mode", "ALTER TABLE rk_message_template ADD COLUMN html_mode TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'html mode' AFTER template_content");
        ensureColumn("media_payload", "ALTER TABLE rk_message_template ADD COLUMN media_payload LONGTEXT NULL COMMENT 'media payload json' AFTER html_mode");
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_message_template' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("rk_message_template missing column {}, auto creating", columnName);
        jdbcTemplate.execute(ddl);
    }
}
