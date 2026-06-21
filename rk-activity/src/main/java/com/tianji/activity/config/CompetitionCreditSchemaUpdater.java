package com.tianji.activity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionCreditSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureCompetitionCreditColumns() {
        ensureColumn("participation_points",
                "ALTER TABLE competition_competitions ADD COLUMN participation_points INT NULL COMMENT '参与学分'");
        ensureColumn("first_prize_points",
                "ALTER TABLE competition_competitions ADD COLUMN first_prize_points INT NULL COMMENT '一等奖学分'");
        ensureColumn("second_prize_points",
                "ALTER TABLE competition_competitions ADD COLUMN second_prize_points INT NULL COMMENT '二等奖学分'");
        ensureColumn("third_prize_points",
                "ALTER TABLE competition_competitions ADD COLUMN third_prize_points INT NULL COMMENT '三等奖学分'");
        ensureColumn("excellent_prize_points",
                "ALTER TABLE competition_competitions ADD COLUMN excellent_prize_points INT NULL COMMENT '优秀奖学分'");
    }

    private void ensureColumn(String columnName, String ddl) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'competition_competitions' AND COLUMN_NAME = ?",
                    Integer.class,
                    columnName
            );
            if (count != null && count > 0) {
                return;
            }
            log.info("competition_competitions missing column {}, adding automatically", columnName);
            jdbcTemplate.execute(ddl);
        } catch (Exception e) {
            log.warn("ensureCompetitionCreditColumns failed for column {}", columnName, e);
        }
    }
}
