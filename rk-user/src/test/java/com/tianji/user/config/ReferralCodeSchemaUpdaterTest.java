package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralCodeSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureReferralCodeTable_shouldCreateTableWhenMissing() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = ?"), eq(Integer.class), anyString()))
                .thenReturn(0);

        ReferralCodeSchemaUpdater updater = new ReferralCodeSchemaUpdater(jdbcTemplate);

        updater.ensureReferralCodeTable();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE referral_code"));
    }
}
