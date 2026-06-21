package com.tianji.message.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class WikiDocumentSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureWikiDocumentTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_wiki_document'",
                    Integer.class
            );
            if (count != null && count > 0) {
                ensureColumn("status", "ALTER TABLE rk_wiki_document ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT 'status: 1 enabled, 0 disabled'");
                ensureColumn("creator", "ALTER TABLE rk_wiki_document ADD COLUMN creator BIGINT NULL COMMENT 'creator auth user id'");
                ensureColumn("updater", "ALTER TABLE rk_wiki_document ADD COLUMN updater BIGINT NULL COMMENT 'updater auth user id'");
                ensureColumn("is_deleted", "ALTER TABLE rk_wiki_document ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete flag'");
                return;
            }
            jdbcTemplate.execute("CREATE TABLE rk_wiki_document ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',"
                    + "tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',"
                    + "title VARCHAR(200) NOT NULL COMMENT 'document title',"
                    + "category VARCHAR(100) NOT NULL DEFAULT 'default' COMMENT 'document category',"
                    + "content MEDIUMTEXT NULL COMMENT 'markdown content',"
                    + "status TINYINT NOT NULL DEFAULT 1 COMMENT 'status: 1 enabled, 0 disabled',"
                    + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',"
                    + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',"
                    + "creator BIGINT NULL COMMENT 'creator auth user id',"
                    + "updater BIGINT NULL COMMENT 'updater auth user id',"
                    + "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete flag',"
                    + "PRIMARY KEY (id),"
                    + "KEY idx_wiki_tenant_category (tenant_id, category, is_deleted),"
                    + "KEY idx_wiki_update_time (update_time)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='wiki documents'");
            log.info("created rk_wiki_document table");
        } catch (Exception e) {
            log.warn("rk_wiki_document auto-migration failed or skipped: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_wiki_document' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("rk_wiki_document column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
