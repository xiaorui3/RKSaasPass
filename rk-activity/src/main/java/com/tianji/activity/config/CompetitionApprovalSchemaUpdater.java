package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionApprovalSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureCompetitionApprovalColumns() {
        ensureColumn("manager_review_status",
                "ALTER TABLE competition_competitions ADD COLUMN manager_review_status TINYINT NOT NULL DEFAULT 1 COMMENT 'manager review status: 0 pending, 1 approved, 2 rejected'");
        ensureColumn("manager_review_comment",
                "ALTER TABLE competition_competitions ADD COLUMN manager_review_comment VARCHAR(500) NULL COMMENT 'manager review comment'");
        ensureColumn("manager_review_time",
                "ALTER TABLE competition_competitions ADD COLUMN manager_review_time DATETIME NULL COMMENT 'manager review time'");
        ensureColumn("manager_reviewer_id",
                "ALTER TABLE competition_competitions ADD COLUMN manager_reviewer_id BIGINT NULL COMMENT 'manager reviewer id'");
        ensureColumn("teacher_review_status",
                "ALTER TABLE competition_competitions ADD COLUMN teacher_review_status TINYINT NOT NULL DEFAULT 1 COMMENT '指导老师审核状态：0-待审核 1-已通过 2-已拒绝'");
        ensureColumn("teacher_review_comment",
                "ALTER TABLE competition_competitions ADD COLUMN teacher_review_comment VARCHAR(500) NULL COMMENT '指导老师审核意见'");
        ensureColumn("teacher_review_time",
                "ALTER TABLE competition_competitions ADD COLUMN teacher_review_time DATETIME NULL COMMENT '指导老师审核时间'");
        ensureColumn("teacher_reviewer_id",
                "ALTER TABLE competition_competitions ADD COLUMN teacher_reviewer_id BIGINT NULL COMMENT '指导老师审核人ID'");
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_competitions' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("competition_competitions missing column {}, adding automatically", columnName);
        jdbcTemplate.execute(ddl);
    }
}
