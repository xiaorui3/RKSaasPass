package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClubMemberSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureClubMemberSchema() {
        try {
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.TABLES " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'club_members'",
                    Integer.class
            );
            if (tableCount == null || tableCount == 0) {
                return;
            }

            jdbcTemplate.execute("UPDATE club_members SET tenant_id = 1 WHERE tenant_id IS NULL");
            jdbcTemplate.execute("ALTER TABLE club_members MODIFY COLUMN tenant_id BIGINT NOT NULL DEFAULT 1");

            if (!indexExists("uk_tenant_student_id")) {
                jdbcTemplate.execute(
                        "ALTER TABLE club_members " +
                                "ADD UNIQUE KEY `uk_tenant_student_id` (`tenant_id`, `student_id`)"
                );
            }
            if (indexExists("uk_student_id")) {
                jdbcTemplate.execute("ALTER TABLE club_members DROP INDEX `uk_student_id`");
            }
        } catch (Exception e) {
            log.warn("club member schema updater skipped: {}", e.getMessage());
        }
    }

    private boolean indexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'club_members' AND INDEX_NAME = ?",
                Integer.class,
                indexName
        );
        return count != null && count > 0;
    }
}
