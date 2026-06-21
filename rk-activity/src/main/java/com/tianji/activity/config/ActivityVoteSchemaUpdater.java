package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityVoteSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureActivityVoteTables() {
        try {
            ensureVoteTable();
            ensureOptionTable();
        } catch (Exception e) {
            log.warn("activity vote auto-migration failed or skipped: {}", e.getMessage());
        }
    }

    private void ensureVoteTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_activity_vote'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("CREATE TABLE rk_activity_vote ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',"
                + "tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',"
                + "activity_id BIGINT NOT NULL COMMENT 'activity id',"
                + "title VARCHAR(200) NOT NULL COMMENT 'vote title',"
                + "description VARCHAR(1000) NULL COMMENT 'vote description',"
                + "status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 2 closed',"
                + "target_user_ids MEDIUMTEXT NULL COMMENT 'target auth user ids json',"
                + "notify_sent TINYINT NOT NULL DEFAULT 0 COMMENT 'notify sent flag',"
                + "closed_time DATETIME NULL COMMENT 'closed time',"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',"
                + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',"
                + "creator BIGINT NULL COMMENT 'creator auth user id',"
                + "updater BIGINT NULL COMMENT 'updater auth user id',"
                + "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete flag',"
                + "PRIMARY KEY (id),"
                + "KEY idx_activity_vote_activity (activity_id, is_deleted),"
                + "KEY idx_activity_vote_tenant (tenant_id, is_deleted)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity votes'");
        log.info("created rk_activity_vote table");
    }

    private void ensureOptionTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'rk_activity_vote_option'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("CREATE TABLE rk_activity_vote_option ("
                + "id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',"
                + "tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT 'tenant id',"
                + "vote_id BIGINT NOT NULL COMMENT 'vote id',"
                + "option_label VARCHAR(200) NOT NULL COMMENT 'option label',"
                + "vote_count INT NOT NULL DEFAULT 0 COMMENT 'vote count',"
                + "sort_order INT NOT NULL DEFAULT 0 COMMENT 'sort order',"
                + "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',"
                + "update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'update time',"
                + "creator BIGINT NULL COMMENT 'creator auth user id',"
                + "updater BIGINT NULL COMMENT 'updater auth user id',"
                + "is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'logic delete flag',"
                + "PRIMARY KEY (id),"
                + "KEY idx_activity_vote_option_vote (vote_id, is_deleted)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='activity vote options'");
        log.info("created rk_activity_vote_option table");
    }
}
