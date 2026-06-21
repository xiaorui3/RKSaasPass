package com.tianji.user.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailReferralAuditSchemaUpdaterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    void ensureEmailReferralAuditSchema_shouldAddColumnsAndCreateTablesWhenMissing() {
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'email_invitation' AND COLUMN_NAME = ?"), eq(Integer.class), anyString()))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'email_invitation_event'"), eq(Integer.class)))
                .thenReturn(0);
        when(jdbcTemplate.queryForObject(contains("TABLE_NAME = 'referral_conversion_record'"), eq(Integer.class)))
                .thenReturn(0);

        EmailReferralAuditSchemaUpdater updater = new EmailReferralAuditSchemaUpdater(jdbcTemplate);

        updater.ensureEmailReferralAuditSchema();

        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE email_invitation ADD COLUMN referral_code"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("ALTER TABLE email_invitation ADD COLUMN referral_code_id"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE email_invitation_event"));
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE referral_conversion_record"));
    }
}
