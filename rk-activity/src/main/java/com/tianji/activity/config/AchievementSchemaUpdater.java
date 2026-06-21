package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class AchievementSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAchievementSchema() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_member_achievement'",
                Integer.class
        );
        if (count != null && count > 0) {
            ensureReviewColumns();
            return;
        }
        log.info("rk_member_achievement table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE rk_member_achievement (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "user_id BIGINT NOT NULL," +
                        "achievement_type TINYINT NOT NULL," +
                        "achievement_title VARCHAR(200) NOT NULL," +
                        "achievement_level TINYINT DEFAULT NULL," +
                        "description TEXT," +
                        "proof_images JSON DEFAULT NULL," +
                        "award_date DATE DEFAULT NULL," +
                        "award_organization VARCHAR(200) DEFAULT NULL," +
                        "status TINYINT NOT NULL DEFAULT 1," +
                        "audit_time DATETIME DEFAULT NULL," +
                        "audit_user_id BIGINT DEFAULT NULL," +
                        "audit_remark VARCHAR(500) DEFAULT NULL," +
                        "manager_review_status TINYINT NOT NULL DEFAULT 0," +
                        "manager_review_comment VARCHAR(500) DEFAULT NULL," +
                        "manager_review_time DATETIME DEFAULT NULL," +
                        "manager_reviewer_id BIGINT DEFAULT NULL," +
                        "teacher_review_status TINYINT NOT NULL DEFAULT 0," +
                        "teacher_review_comment VARCHAR(500) DEFAULT NULL," +
                        "teacher_review_time DATETIME DEFAULT NULL," +
                        "teacher_reviewer_id BIGINT DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "creator BIGINT DEFAULT NULL," +
                        "updater BIGINT DEFAULT NULL," +
                        "is_deleted TINYINT NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_achievement_tenant (tenant_id)," +
                        "KEY idx_achievement_user (user_id)," +
                        "KEY idx_achievement_type (achievement_type)," +
                        "KEY idx_achievement_level (achievement_level)," +
                        "KEY idx_achievement_status (status)," +
                        "KEY idx_achievement_award_date (award_date)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='member achievement'"
        );
    }

    private void ensureReviewColumns() {
        ensureColumn("manager_review_status",
                "ALTER TABLE rk_member_achievement ADD COLUMN manager_review_status TINYINT NOT NULL DEFAULT 0 COMMENT 'manager review status: 0 pending, 1 approved, 2 rejected'");
        ensureColumn("manager_review_comment",
                "ALTER TABLE rk_member_achievement ADD COLUMN manager_review_comment VARCHAR(500) NULL COMMENT 'manager review comment'");
        ensureColumn("manager_review_time",
                "ALTER TABLE rk_member_achievement ADD COLUMN manager_review_time DATETIME NULL COMMENT 'manager review time'");
        ensureColumn("manager_reviewer_id",
                "ALTER TABLE rk_member_achievement ADD COLUMN manager_reviewer_id BIGINT NULL COMMENT 'manager reviewer id'");
        ensureColumn("teacher_review_status",
                "ALTER TABLE rk_member_achievement ADD COLUMN teacher_review_status TINYINT NOT NULL DEFAULT 0 COMMENT 'teacher review status: 0 pending, 1 approved, 2 rejected'");
        ensureColumn("teacher_review_comment",
                "ALTER TABLE rk_member_achievement ADD COLUMN teacher_review_comment VARCHAR(500) NULL COMMENT 'teacher review comment'");
        ensureColumn("teacher_review_time",
                "ALTER TABLE rk_member_achievement ADD COLUMN teacher_review_time DATETIME NULL COMMENT 'teacher review time'");
        ensureColumn("teacher_reviewer_id",
                "ALTER TABLE rk_member_achievement ADD COLUMN teacher_reviewer_id BIGINT NULL COMMENT 'teacher reviewer id'");
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_member_achievement' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("rk_member_achievement missing column {}, adding automatically", columnName);
        jdbcTemplate.execute(ddl);
    }
}
