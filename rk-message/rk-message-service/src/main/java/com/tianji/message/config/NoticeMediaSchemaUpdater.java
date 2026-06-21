package com.tianji.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeMediaSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureNoticeMediaColumns() {
        try {
            ensureColumn("notice_level",
                    "ALTER TABLE rk_notice ADD COLUMN notice_level VARCHAR(32) NULL COMMENT 'notice priority level'");
            ensureColumn("target_type",
                    "ALTER TABLE rk_notice ADD COLUMN target_type VARCHAR(32) NULL COMMENT 'target type'");
            ensureColumn("target_ids",
                    "ALTER TABLE rk_notice ADD COLUMN target_ids VARCHAR(1000) NULL COMMENT 'target id list'");
            ensureColumn("publish_time",
                    "ALTER TABLE rk_notice ADD COLUMN publish_time DATETIME NULL COMMENT 'publish time'");
            ensureColumn("is_top",
                    "ALTER TABLE rk_notice ADD COLUMN is_top TINYINT NOT NULL DEFAULT 0 COMMENT 'top flag'");
            ensureColumn("view_count",
                    "ALTER TABLE rk_notice ADD COLUMN view_count INT NOT NULL DEFAULT 0 COMMENT 'view count'");
            ensureColumn("is_published",
                    "ALTER TABLE rk_notice ADD COLUMN is_published TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'published flag'");
            ensureColumn("cover_image",
                    "ALTER TABLE rk_notice ADD COLUMN cover_image VARCHAR(500) NULL COMMENT '公告封面图片URL'");
            ensureColumn("attachment_url",
                    "ALTER TABLE rk_notice ADD COLUMN attachment_url VARCHAR(500) NULL COMMENT '公告附件URL'");
            ensureColumn("manager_review_status",
                    "ALTER TABLE rk_notice ADD COLUMN manager_review_status TINYINT NOT NULL DEFAULT 1 COMMENT 'manager review status: 0 pending, 1 approved, 2 rejected'");
            ensureColumn("manager_review_comment",
                    "ALTER TABLE rk_notice ADD COLUMN manager_review_comment VARCHAR(500) NULL COMMENT 'manager review comment'");
            ensureColumn("manager_review_time",
                    "ALTER TABLE rk_notice ADD COLUMN manager_review_time DATETIME NULL COMMENT 'manager review time'");
            ensureColumn("manager_reviewer_id",
                    "ALTER TABLE rk_notice ADD COLUMN manager_reviewer_id BIGINT NULL COMMENT 'manager reviewer id'");
            ensureColumn("teacher_review_status",
                    "ALTER TABLE rk_notice ADD COLUMN teacher_review_status TINYINT NOT NULL DEFAULT 1 COMMENT 'teacher review status: 0 pending, 1 approved, 2 rejected'");
            ensureColumn("teacher_review_comment",
                    "ALTER TABLE rk_notice ADD COLUMN teacher_review_comment VARCHAR(500) NULL COMMENT 'teacher review comment'");
            ensureColumn("teacher_review_time",
                    "ALTER TABLE rk_notice ADD COLUMN teacher_review_time DATETIME NULL COMMENT 'teacher review time'");
            ensureColumn("teacher_reviewer_id",
                    "ALTER TABLE rk_notice ADD COLUMN teacher_reviewer_id BIGINT NULL COMMENT 'teacher reviewer id'");
        } catch (Exception e) {
            log.warn("rk_notice 媒体字段自动迁移跳过: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_notice' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            log.info("rk_notice missing column {}, adding automatically", columnName);
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("rk_notice column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
