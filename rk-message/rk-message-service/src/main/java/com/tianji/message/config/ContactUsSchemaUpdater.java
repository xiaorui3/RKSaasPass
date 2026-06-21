package com.tianji.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactUsSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureContactUsColumns() {
        ensureColumn("target_email", "ALTER TABLE contact_us_messages ADD COLUMN target_email VARCHAR(255) NULL COMMENT 'requested recipient email' AFTER email");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'contact_us_messages' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("contact_us_messages column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
