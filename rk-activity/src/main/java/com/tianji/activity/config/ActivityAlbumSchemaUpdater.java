package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityAlbumSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureActivityAlbumTable() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_activity_album_photo'",
                    Integer.class
            );
            if (count != null && count > 0) {
                ensureColumn("tenant_id", "ALTER TABLE rk_activity_album_photo ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id'");
                ensureColumn("file_name", "ALTER TABLE rk_activity_album_photo ADD COLUMN file_name VARCHAR(255) NULL COMMENT 'original file name'");
                ensureColumn("description", "ALTER TABLE rk_activity_album_photo ADD COLUMN description VARCHAR(500) NULL COMMENT 'description'");
                ensureColumn("sort_order", "ALTER TABLE rk_activity_album_photo ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order'");
                ensureColumn("uploader_id", "ALTER TABLE rk_activity_album_photo ADD COLUMN uploader_id BIGINT NULL COMMENT 'uploader auth user id'");
                ensureColumn("create_time", "ALTER TABLE rk_activity_album_photo ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time'");
                ensureColumn("update_time", "ALTER TABLE rk_activity_album_photo ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time'");
                ensureColumn("creator", "ALTER TABLE rk_activity_album_photo ADD COLUMN creator BIGINT NULL COMMENT 'creator auth user id'");
                ensureColumn("updater", "ALTER TABLE rk_activity_album_photo ADD COLUMN updater BIGINT NULL COMMENT 'updater auth user id'");
                ensureColumn("is_deleted", "ALTER TABLE rk_activity_album_photo ADD COLUMN is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete flag'");
                return;
            }
            jdbcTemplate.execute("CREATE TABLE rk_activity_album_photo ("
                    + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                    + "tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',"
                    + "activity_id BIGINT NOT NULL COMMENT '活动ID',"
                    + "photo_url VARCHAR(500) NOT NULL COMMENT '照片相对路径',"
                    + "file_name VARCHAR(255) NULL COMMENT '原始文件名',"
                    + "description VARCHAR(500) NULL COMMENT '说明',"
                    + "sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',"
                    + "uploader_id BIGINT NULL COMMENT '上传人auth用户ID',"
                    + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',"
                    + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',"
                    + "creator BIGINT NULL COMMENT '创建人',"
                    + "updater BIGINT NULL COMMENT '更新人',"
                    + "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',"
                    + "PRIMARY KEY (id),"
                    + "KEY idx_activity_album_activity (activity_id, is_deleted),"
                    + "KEY idx_activity_album_tenant (tenant_id, is_deleted)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动相册照片表'");
            log.info("created rk_activity_album_photo table");
        } catch (Exception e) {
            log.warn("rk_activity_album_photo auto-migration failed or skipped: {}", e.getMessage());
        }
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_activity_album_photo' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("rk_activity_album_photo column {} auto-migration failed or skipped: {}", columnName, e.getMessage());
        }
    }
}
