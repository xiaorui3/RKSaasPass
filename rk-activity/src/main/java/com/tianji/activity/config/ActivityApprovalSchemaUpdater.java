package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityApprovalSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void updateSchema() {
        ensureColumn("rk_activity", "manager_review_status",
                "ALTER TABLE rk_activity ADD COLUMN manager_review_status TINYINT NOT NULL DEFAULT 1 COMMENT '负责人审核状态：0-待审核 1-已通过 2-已拒绝'");
        ensureColumn("rk_activity", "manager_review_comment",
                "ALTER TABLE rk_activity ADD COLUMN manager_review_comment VARCHAR(500) NULL COMMENT '负责人审核意见'");
        ensureColumn("rk_activity", "manager_review_time",
                "ALTER TABLE rk_activity ADD COLUMN manager_review_time DATETIME NULL COMMENT '负责人审核时间'");
        ensureColumn("rk_activity", "manager_reviewer_id",
                "ALTER TABLE rk_activity ADD COLUMN manager_reviewer_id BIGINT NULL COMMENT '负责人审核人ID'");
        ensureColumn("rk_activity", "teacher_review_status",
                "ALTER TABLE rk_activity ADD COLUMN teacher_review_status TINYINT NOT NULL DEFAULT 1 COMMENT '指导老师审核状态：0-待审核 1-已通过 2-已拒绝'");
        ensureColumn("rk_activity", "teacher_review_comment",
                "ALTER TABLE rk_activity ADD COLUMN teacher_review_comment VARCHAR(500) NULL COMMENT '指导老师审核意见'");
        ensureColumn("rk_activity", "teacher_review_time",
                "ALTER TABLE rk_activity ADD COLUMN teacher_review_time DATETIME NULL COMMENT '指导老师审核时间'");
        ensureColumn("rk_activity", "teacher_reviewer_id",
                "ALTER TABLE rk_activity ADD COLUMN teacher_reviewer_id BIGINT NULL COMMENT '指导老师审核人ID'");
    }

    private void ensureColumn(String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("表 {} 缺少字段 {}，开始自动补齐", tableName, columnName);
        jdbcTemplate.execute(ddl);
    }
}
