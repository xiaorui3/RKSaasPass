package com.tianji.content.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrossTenantNewsSchemaUpdater {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void updateSchema() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'news' AND COLUMN_NAME = 'is_cross_tenant'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }
        log.info("news 表缺少字段 is_cross_tenant，开始自动补齐");
        jdbcTemplate.execute("ALTER TABLE news ADD COLUMN is_cross_tenant TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许跨租户公开展示'");
    }
}
