package com.tianji.common.autoconfigure.mybatis;

import com.tianji.common.utils.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RKTenantLineHandlerTest {

    @AfterEach
    void tearDown() {
        TenantContext.removeTenantId();
        TenantContext.setSuperAdmin(false);
    }

    @Test
    void ignoreTable_shouldSkipLoginRecordBecauseTableHasNoTenantColumn() {
        RKTenantLineHandler handler = new RKTenantLineHandler();

        assertTrue(handler.ignoreTable("login_record"));
    }

    @Test
    void ignoreTable_shouldStillApplyTenantIsolationToTenantScopedBusinessTables() {
        RKTenantLineHandler handler = new RKTenantLineHandler();

        assertFalse(handler.ignoreTable("role"));
    }
}
