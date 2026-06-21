package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorksApprovalSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureWorksApprovalColumns() {
        ensureColumn("manager_review_status",
                "ALTER TABLE works ADD COLUMN manager_review_status TINYINT NOT NULL DEFAULT 1 COMMENT 'manager review status: 0 pending, 1 approved, 2 rejected'");
        ensureColumn("manager_review_comment",
                "ALTER TABLE works ADD COLUMN manager_review_comment VARCHAR(500) NULL COMMENT 'manager review comment'");
        ensureColumn("manager_review_time",
                "ALTER TABLE works ADD COLUMN manager_review_time DATETIME NULL COMMENT 'manager review time'");
        ensureColumn("manager_reviewer_id",
                "ALTER TABLE works ADD COLUMN manager_reviewer_id BIGINT NULL COMMENT 'manager reviewer id'");
        ensureColumn("teacher_review_status",
                "ALTER TABLE works ADD COLUMN teacher_review_status TINYINT NOT NULL DEFAULT 1 COMMENT 'teacher review status: 0 pending, 1 approved, 2 rejected'");
        ensureColumn("teacher_review_comment",
                "ALTER TABLE works ADD COLUMN teacher_review_comment VARCHAR(500) NULL COMMENT 'teacher review comment'");
        ensureColumn("teacher_review_time",
                "ALTER TABLE works ADD COLUMN teacher_review_time DATETIME NULL COMMENT 'teacher review time'");
        ensureColumn("teacher_reviewer_id",
                "ALTER TABLE works ADD COLUMN teacher_reviewer_id BIGINT NULL COMMENT 'teacher reviewer id'");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'works' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
            log.info("works missing column {}, added automatically", columnName);
        } catch (Exception e) {
            log.warn("works column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
