package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureCommentTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'comments'",
                    Integer.class
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute("CREATE TABLE comments ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "tenant_id BIGINT NULL COMMENT '租户ID',"
                    + "target_type VARCHAR(32) NOT NULL COMMENT '评论目标类型：news/activity',"
                    + "target_id BIGINT NOT NULL COMMENT '评论目标ID',"
                    + "user_id BIGINT NULL COMMENT '评论用户ID',"
                    + "user_name VARCHAR(80) NULL COMMENT '评论用户名称',"
                    + "user_avatar VARCHAR(500) NULL COMMENT '评论用户头像',"
                    + "content VARCHAR(1000) NOT NULL COMMENT '评论内容',"
                    + "featured TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否精选',"
                    + "status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0隐藏 1可见 2删除',"
                    + "creator BIGINT NULL COMMENT '创建人',"
                    + "updater BIGINT NULL COMMENT '更新人',"
                    + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                    + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                    + "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',"
                    + "INDEX idx_comments_target (tenant_id, target_type, target_id, status, is_deleted, create_time),"
                    + "INDEX idx_comments_user (tenant_id, user_id, create_time)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用评论表'");
            log.info("comments table created automatically");
        } catch (Exception e) {
            log.warn("comments table auto-migration failed or skipped: {}", e.getMessage());
        }
    }
}
