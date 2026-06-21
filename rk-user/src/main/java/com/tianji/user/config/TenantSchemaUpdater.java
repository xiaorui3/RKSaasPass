package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaUpdater implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        ensureTenantSchema();
    }

    void ensureTenantSchema() {
        try {
            Integer hasTenantTable = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_tenant'",
                    Integer.class
            );
            if (hasTenantTable == null || hasTenantTable == 0) {
                return;
            }

            Integer hasDisplayOrder = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_tenant' AND COLUMN_NAME = ?",
                    Integer.class,
                    "display_order"
            );
            if (hasDisplayOrder != null && hasDisplayOrder == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE `rk_tenant` " +
                                "ADD COLUMN `display_order` INT NOT NULL DEFAULT 0 COMMENT 'tenant display order' AFTER `logo_url`"
                );
                jdbcTemplate.execute("UPDATE `rk_tenant` SET `display_order` = COALESCE(`display_order`, 0)");
                log.info("added rk_tenant.display_order column");
            }
        } catch (Exception e) {
            log.warn("tenant schema updater skipped: {}", e.getMessage());
        }
    }
}
