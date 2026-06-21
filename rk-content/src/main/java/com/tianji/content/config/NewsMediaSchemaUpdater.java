package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsMediaSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureNewsMediaColumns() {
        try {
            ensureColumn("video_url",
                    "ALTER TABLE news ADD COLUMN video_url VARCHAR(500) NULL COMMENT '新闻视频URL'");
            ensureColumn("attachment_url",
                    "ALTER TABLE news ADD COLUMN attachment_url VARCHAR(500) NULL COMMENT '新闻附件URL'");
        } catch (Exception e) {
            log.warn("news 媒体字段自动迁移跳过: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            log.info("news missing column {}, adding automatically", columnName);
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("news column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
