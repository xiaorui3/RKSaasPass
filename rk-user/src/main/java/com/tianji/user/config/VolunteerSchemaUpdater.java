package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class VolunteerSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureVolunteerSchema() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'volunteer_record'",
                    Integer.class
            );
            if (count == null || count == 0) {
                return;
            }
            ensureColumn("creator", "ALTER TABLE volunteer_record ADD COLUMN creator BIGINT DEFAULT NULL AFTER update_time");
            ensureColumn("updater", "ALTER TABLE volunteer_record ADD COLUMN updater BIGINT DEFAULT NULL AFTER creator");
        } catch (Exception e) {
            log.warn("volunteer schema updater skipped: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() " +
                        "AND TABLE_NAME = 'volunteer_record' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            log.info("volunteer_record.{} missing, applying ddl", columnName);
            jdbcTemplate.execute(ddl);
        }
    }
}
