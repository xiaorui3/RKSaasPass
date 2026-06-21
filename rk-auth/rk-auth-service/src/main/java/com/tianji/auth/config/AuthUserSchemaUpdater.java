package com.tianji.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthUserSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAuthUserSchema() {
        ensureColumn("identity_key", "ALTER TABLE user ADD COLUMN identity_key VARCHAR(64) NULL COMMENT 'linked identity key' AFTER password");
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("auth.user missing column {}, auto creating", columnName);
        jdbcTemplate.execute(ddl);
    }
}
