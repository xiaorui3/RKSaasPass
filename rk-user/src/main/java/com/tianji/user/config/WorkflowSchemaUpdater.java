package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureWorkflowSchema() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'workflow_action_log'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("workflow_action_log table missing, creating automatically");
        jdbcTemplate.execute(
                "CREATE TABLE workflow_action_log (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "business_type VARCHAR(64) NOT NULL," +
                        "target_type VARCHAR(64) NOT NULL," +
                        "target_id BIGINT NOT NULL," +
                        "review_token VARCHAR(128) DEFAULT NULL," +
                        "action VARCHAR(32) NOT NULL," +
                        "final_state VARCHAR(32) NOT NULL," +
                        "actor_auth_user_id BIGINT DEFAULT NULL," +
                        "comment VARCHAR(255) DEFAULT NULL," +
                        "source_channel VARCHAR(32) DEFAULT NULL," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "KEY idx_workflow_action_tenant (tenant_id)," +
                        "KEY idx_workflow_action_target (target_type, target_id)," +
                        "KEY idx_workflow_action_token (review_token)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='workflow action log'"
        );
    }
}
