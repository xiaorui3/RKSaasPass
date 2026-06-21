package com.tianji.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlumniSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAlumniSchema() {
        ensureColumn("show_table",
                "ALTER TABLE club_alumni ADD COLUMN show_table TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'whether to show in alumni roster' AFTER advice");
        ensureColumn("is_active",
                "ALTER TABLE club_alumni ADD COLUMN is_active TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'whether alumni record is active' AFTER show_table");
        ensureColumn("is_core_member",
                "ALTER TABLE club_alumni ADD COLUMN is_core_member TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'whether alumni is a core member' AFTER is_active");
        ensureColumn("member_status",
                "ALTER TABLE club_alumni ADD COLUMN member_status VARCHAR(32) DEFAULT NULL COMMENT 'derived alumni status' AFTER is_core_member");
        ensureColumn("graduation_status",
                "ALTER TABLE club_alumni ADD COLUMN graduation_status VARCHAR(32) DEFAULT NULL COMMENT 'normalized graduation status' AFTER member_status");
        backfillDerivedColumns();
    }

    private void ensureColumn(String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'club_alumni' AND COLUMN_NAME = ?",
                Integer.class,
                columnName
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("club_alumni missing column {}, adding automatically", columnName);
        jdbcTemplate.execute(ddl);
    }

    private void backfillDerivedColumns() {
        int currentYear = LocalDate.now().getYear();
        String resolvedStatusSql = "CASE " +
                "WHEN actual_graduation_date IS NOT NULL THEN '已毕业' " +
                "WHEN expected_graduation_year IS NOT NULL AND expected_graduation_year <= " + currentYear + " THEN '已毕业' " +
                "WHEN enrollment_year IS NOT NULL AND (" + currentYear + " - enrollment_year) >= 4 THEN '已毕业' " +
                "WHEN generation_year IS NOT NULL AND (" + currentYear + " - generation_year) >= 4 THEN '已毕业' " +
                "ELSE '在校' END";
        jdbcTemplate.execute(
                "UPDATE club_alumni SET " +
                        "show_table = COALESCE(show_table, 1), " +
                        "is_active = COALESCE(is_active, CASE WHEN is_deleted = 1 THEN 0 ELSE 1 END), " +
                        "is_core_member = COALESCE(is_core_member, 0), " +
                        "member_status = " + resolvedStatusSql + ", " +
                        "graduation_status = " + resolvedStatusSql
        );
    }
}
