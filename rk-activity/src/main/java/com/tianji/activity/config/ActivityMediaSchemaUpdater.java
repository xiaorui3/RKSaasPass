package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityMediaSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureActivityMediaColumns() {
        try {
            ensureColumn("attachment_url",
                    "ALTER TABLE rk_activity ADD COLUMN attachment_url VARCHAR(500) NULL COMMENT '活动附件URL'");
        } catch (Exception e) {
            log.warn("rk_activity 媒体字段自动迁移跳过: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_activity' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            log.info("rk_activity missing column {}, adding automatically", columnName);
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("rk_activity column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
