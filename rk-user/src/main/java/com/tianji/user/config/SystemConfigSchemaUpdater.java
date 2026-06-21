package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemConfigSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSystemConfigTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'system_config'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        log.info("system_config 表不存在，开始自动建表");
        jdbcTemplate.execute(
                "CREATE TABLE system_config (" +
                        "id BIGINT NOT NULL AUTO_INCREMENT," +
                        "config_key VARCHAR(100) NOT NULL," +
                        "config_value TEXT," +
                        "description VARCHAR(255) DEFAULT NULL," +
                        "tenant_id BIGINT NOT NULL DEFAULT 1," +
                        "is_enabled TINYINT(1) NOT NULL DEFAULT 1," +
                        "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                        "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                        "PRIMARY KEY (id)," +
                        "UNIQUE KEY uk_system_config_tenant_key (tenant_id, config_key)," +
                        "KEY idx_system_config_tenant (tenant_id)" +
                        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表'"
        );
    }
}
