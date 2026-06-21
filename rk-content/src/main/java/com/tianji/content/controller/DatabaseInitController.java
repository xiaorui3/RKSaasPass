package com.tianji.content.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 临时数据库初始化Controller
 * 用于执行新闻审核功能的数据库迁移
 * TODO: 执行完成后删除此类
 */
@Slf4j
@RestController
@RequestMapping("/admin/database")
@RequiredArgsConstructor
public class DatabaseInitController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行新闻审核字段迁移
     * POST /admin/database/init-news-approval
     */
    @PostMapping("/init-news-approval")
    public Map<String, Object> initNewsApprovalFields() {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            // 检查字段是否已存在
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'rk_content' " +
                    "AND TABLE_NAME = 'news' " +
                    "AND COLUMN_NAME = 'approval_status'";

            Integer count = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);

            if (count != null && count > 0) {
                result.put("success", false);
                result.put("message", "字段已存在，无需重复执行");
                result.put("existing", true);
                return result;
            }

            // 添加审核字段
            log.info("开始执行新闻审核字段迁移...");

            // 1. 添加approval_status字段
            String addApprovalStatus = "ALTER TABLE `news` " +
                    "ADD COLUMN `approval_status` tinyint DEFAULT 0 " +
                    "COMMENT '审核状态：0-草稿 1-待审核 2-已通过 3-已拒绝'";
            jdbcTemplate.execute(addApprovalStatus);
            log.info("✅ 添加字段: approval_status");

            // 2. 添加approver字段
            String addApprover = "ALTER TABLE `news` " +
                    "ADD COLUMN `approver` varchar(50) " +
                    "COMMENT '审核人'";
            jdbcTemplate.execute(addApprover);
            log.info("✅ 添加字段: approver");

            // 3. 添加approval_time字段
            String addApprovalTime = "ALTER TABLE `news` " +
                    "ADD COLUMN `approval_time` datetime " +
                    "COMMENT '审核时间'";
            jdbcTemplate.execute(addApprovalTime);
            log.info("✅ 添加字段: approval_time");

            // 4. 添加reject_reason字段
            String addRejectReason = "ALTER TABLE `news` " +
                    "ADD COLUMN `reject_reason` varchar(500) " +
                    "COMMENT '拒绝原因'";
            jdbcTemplate.execute(addRejectReason);
            log.info("✅ 添加字段: reject_reason");

            // 5. 创建索引
            try {
                String createIndex1 = "CREATE INDEX idx_approval_status ON news(approval_status)";
                jdbcTemplate.execute(createIndex1);
                log.info("✅ 创建索引: idx_approval_status");
            } catch (Exception e) {
                log.warn("索引idx_approval_status可能已存在: {}", e.getMessage());
            }

            try {
                String createIndex2 = "CREATE INDEX idx_tenant_approval ON news(tenant_id, approval_status)";
                jdbcTemplate.execute(createIndex2);
                log.info("✅ 创建索引: idx_tenant_approval");
            } catch (Exception e) {
                log.warn("索引idx_tenant_approval可能已存在: {}", e.getMessage());
            }

            // 6. 更新现有数据
            String updatePublished = "UPDATE news SET approval_status = 2 WHERE is_published = 1";
            int publishedCount = jdbcTemplate.update(updatePublished);
            log.info("✅ 更新已发布新闻为'已通过'状态: {} 条", publishedCount);

            String updateDraft = "UPDATE news SET approval_status = 0 WHERE is_published = 0 AND approval_status IS NULL";
            int draftCount = jdbcTemplate.update(updateDraft);
            log.info("✅ 更新草稿新闻: {} 条", draftCount);

            // 7. 验证迁移结果
            String verifySql = "SELECT COUNT(*) as total_news, " +
                    "SUM(CASE WHEN approval_status = 0 THEN 1 ELSE 0 END) as draft_count, " +
                    "SUM(CASE WHEN approval_status = 1 THEN 1 ELSE 0 END) as pending_count, " +
                    "SUM(CASE WHEN approval_status = 2 THEN 1 ELSE 0 END) as approved_count, " +
                    "SUM(CASE WHEN approval_status = 3 THEN 1 ELSE 0 END) as rejected_count " +
                    "FROM news";

            Map<String, Object> stats = jdbcTemplate.queryForMap(verifySql);

            result.put("success", true);
            result.put("message", "数据库迁移成功");
            result.put("stats", stats);
            log.info("📊 迁移统计: {}", stats);

        } catch (Exception e) {
            log.error("数据库迁移失败", e);
            result.put("success", false);
            result.put("message", "迁移失败: " + e.getMessage());
            result.put("error", e.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * 检查迁移状态
     * GET /admin/database/check-news-approval
     */
    @PostMapping("/check-news-approval")
    public Map<String, Object> checkMigrationStatus() {
        Map<String, Object> result = new java.util.HashMap<>();

        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = 'rk_content' " +
                    "AND TABLE_NAME = 'news' " +
                    "AND COLUMN_NAME IN ('approval_status', 'approver', 'approval_time', 'reject_reason')";

            Integer columnCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

            boolean migrated = columnCount != null && columnCount >= 4;

            result.put("migrated", migrated);
            result.put("columnCount", columnCount);

            if (migrated) {
                // 获取统计数据
                String statsSql = "SELECT COUNT(*) as total, " +
                        "SUM(CASE WHEN approval_status = 1 THEN 1 ELSE 0 END) as pending " +
                        "FROM news";
                Map<String, Object> stats = jdbcTemplate.queryForMap(statsSql);
                result.put("stats", stats);
            }

        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }
}
