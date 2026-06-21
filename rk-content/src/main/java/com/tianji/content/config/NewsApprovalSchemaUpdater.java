package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 新闻审核表结构自动更新器
 * 在应用启动时自动添加缺失的审核字段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsApprovalSchemaUpdater implements ApplicationListener<ContextRefreshedEvent> {

    private final DataSource dataSource;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            ensureColumn(jdbcTemplate, "manager_review_status",
                    "ALTER TABLE `news` ADD COLUMN `manager_review_status` tinyint DEFAULT 1 COMMENT 'manager review status: 0 pending, 1 approved, 2 rejected'");
            ensureColumn(jdbcTemplate, "manager_review_comment",
                    "ALTER TABLE `news` ADD COLUMN `manager_review_comment` varchar(500) NULL COMMENT 'manager review comment'");
            ensureColumn(jdbcTemplate, "manager_review_time",
                    "ALTER TABLE `news` ADD COLUMN `manager_review_time` datetime NULL COMMENT 'manager review time'");
            ensureColumn(jdbcTemplate, "manager_reviewer_id",
                    "ALTER TABLE `news` ADD COLUMN `manager_reviewer_id` bigint NULL COMMENT 'manager reviewer id'");
            ensureColumn(jdbcTemplate, "teacher_review_status",
                    "ALTER TABLE `news` ADD COLUMN `teacher_review_status` tinyint DEFAULT 1 COMMENT 'teacher review status: 0 pending, 1 approved, 2 rejected'");
            ensureColumn(jdbcTemplate, "teacher_review_comment",
                    "ALTER TABLE `news` ADD COLUMN `teacher_review_comment` varchar(500) NULL COMMENT 'teacher review comment'");
            ensureColumn(jdbcTemplate, "teacher_review_time",
                    "ALTER TABLE `news` ADD COLUMN `teacher_review_time` datetime NULL COMMENT 'teacher review time'");
            ensureColumn(jdbcTemplate, "teacher_reviewer_id",
                    "ALTER TABLE `news` ADD COLUMN `teacher_reviewer_id` bigint NULL COMMENT 'teacher reviewer id'");

            // 检查字段是否存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'rk_content' " +
                    "AND TABLE_NAME = 'news' " +
                    "AND COLUMN_NAME = 'approval_status'";

            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (count == null || count == 0) {
                log.info("📊 检测到新闻审核字段缺失，开始自动迁移...");

                // 添加字段
                try {
                    jdbcTemplate.execute("ALTER TABLE `news` ADD COLUMN `approval_status` tinyint DEFAULT 0 COMMENT '审核状态：0-草稿 1-待审核 2-已通过 3-已拒绝'");
                    log.info("✅ 添加字段: approval_status");
                } catch (Exception e) {
                    log.warn("字段approval_status添加失败或已存在: {}", e.getMessage());
                }

                try {
                    jdbcTemplate.execute("ALTER TABLE `news` ADD COLUMN `approver` varchar(50) COMMENT '审核人'");
                    log.info("✅ 添加字段: approver");
                } catch (Exception e) {
                    log.warn("字段approver添加失败或已存在: {}", e.getMessage());
                }

                try {
                    jdbcTemplate.execute("ALTER TABLE `news` ADD COLUMN `approval_time` datetime COMMENT '审核时间'");
                    log.info("✅ 添加字段: approval_time");
                } catch (Exception e) {
                    log.warn("字段approval_time添加失败或已存在: {}", e.getMessage());
                }

                try {
                    jdbcTemplate.execute("ALTER TABLE `news` ADD COLUMN `reject_reason` varchar(500) COMMENT '拒绝原因'");
                    log.info("✅ 添加字段: reject_reason");
                } catch (Exception e) {
                    log.warn("字段reject_reason添加失败或已存在: {}", e.getMessage());
                }

                // 创建索引
                try {
                    jdbcTemplate.execute("CREATE INDEX idx_approval_status ON news(approval_status)");
                    log.info("✅ 创建索引: idx_approval_status");
                } catch (Exception e) {
                    log.warn("索引idx_approval_status创建失败或已存在: {}", e.getMessage());
                }

                try {
                    jdbcTemplate.execute("CREATE INDEX idx_tenant_approval ON news(tenant_id, approval_status)");
                    log.info("✅ 创建索引: idx_tenant_approval");
                } catch (Exception e) {
                    log.warn("索引idx_tenant_approval创建失败或已存在: {}", e.getMessage());
                }

                // 更新现有数据
                try {
                    int publishedCount = jdbcTemplate.update("UPDATE news SET approval_status = 2 WHERE is_published = 1");
                    log.info("✅ 更新已发布新闻为'已通过'状态: {} 条", publishedCount);
                } catch (Exception e) {
                    log.warn("更新已发布新闻失败: {}", e.getMessage());
                }

                try {
                    int draftCount = jdbcTemplate.update("UPDATE news SET approval_status = 0 WHERE is_published = 0 AND (approval_status IS NULL OR approval_status = 0)");
                    log.info("✅ 更新草稿新闻: {} 条", draftCount);
                } catch (Exception e) {
                    log.warn("更新草稿新闻失败: {}", e.getMessage());
                }

                log.info("🎉 新闻审核表结构迁移完成！");
            } else {
                log.info("✅ 新闻审核字段已存在，无需迁移");
            }
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }

    private void ensureColumn(JdbcTemplate jdbcTemplate, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.execute(ddl);
            log.info("news missing column {}, added automatically", columnName);
        } catch (Exception e) {
            log.warn("add news column {} failed or already exists: {}", columnName, e.getMessage());
        }
    }
}
