package com.tianji.user.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.DemoDataCenterVO;
import com.tianji.user.mapper.SystemConfigMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.Invocation;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoDataServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void generateShouldPersistRepeatableSafeManifestWhenSourceTablesAreUnavailable() {
        TenantContext.setTenantId(1L);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(mapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(0);

        DemoDataServiceImpl service = new DemoDataServiceImpl(mapper, jdbcTemplate, new ObjectMapper());

        DemoDataCenterVO result = service.generate();

        assertEquals("GENERATED", result.getStatus());
        assertEquals("DEMO_OPEN_SOURCE", result.getSafeTag());
        assertEquals("open-source-demo", result.getDemoTenantCode());
        assertTrue(result.getRepeatable());
        assertTrue(result.getCleanupSupported());
        assertTrue(result.getCounts().get("demoNews") >= 1);
        assertTrue(result.getCounts().get("demoActivities") >= 1);
        assertTrue(result.getCounts().get("demoCompetitions") >= 1);
        assertFalse(result.getSampleItems().isEmpty());

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).insert(captor.capture());
        SystemConfig saved = captor.getValue();
        assertEquals(1L, saved.getTenantId());
        assertEquals("open.source.demo.data", saved.getConfigKey());
        assertTrue(saved.getConfigValue().contains("DEMO_OPEN_SOURCE"));
        assertTrue(saved.getConfigValue().contains("demoBatchId"));
        assertTrue(saved.getConfigValue().contains("open-source-demo"));
    }

    @Test
    void cleanupShouldOnlyUseDemoMarkerAndKeepManifestAsCleaned() {
        TenantContext.setTenantId(7L);
        SystemConfig existing = new SystemConfig()
                .setId(100L)
                .setTenantId(7L)
                .setConfigKey("open.source.demo.data")
                .setConfigValue("{\"status\":\"GENERATED\",\"safeTag\":\"DEMO_OPEN_SOURCE\",\"demoTenantCode\":\"open-source-demo\",\"counts\":{\"demoUsers\":2},\"sampleItems\":[]}");
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(mapper.selectOne(ArgumentMatchers.any())).thenReturn(existing);
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(0);

        DemoDataServiceImpl service = new DemoDataServiceImpl(mapper, jdbcTemplate, new ObjectMapper());

        DemoDataCenterVO result = service.cleanup();

        assertEquals("CLEANED", result.getStatus());
        assertEquals("DEMO_OPEN_SOURCE", result.getSafeTag());
        verify(jdbcTemplate, never()).update(ArgumentMatchers.contains("DELETE FROM"), ArgumentMatchers.<Object[]>any());
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).updateById(captor.capture());
        assertTrue(captor.getValue().getConfigValue().contains("\"status\":\"CLEANED\""));
    }

    @Test
    void resetShouldCleanupThenGenerateNewManifest() {
        TenantContext.setTenantId(1L);
        SystemConfig existing = new SystemConfig()
                .setId(101L)
                .setTenantId(1L)
                .setConfigKey("open.source.demo.data")
                .setConfigValue("{\"status\":\"GENERATED\",\"safeTag\":\"DEMO_OPEN_SOURCE\",\"demoTenantCode\":\"open-source-demo\",\"counts\":{\"demoUsers\":2},\"sampleItems\":[]}");
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(mapper.selectOne(ArgumentMatchers.any())).thenReturn(existing);
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.eq(Integer.class), any(Object[].class))).thenReturn(0);

        DemoDataServiceImpl service = new DemoDataServiceImpl(mapper, jdbcTemplate, new ObjectMapper());

        DemoDataCenterVO result = service.reset();

        assertEquals("GENERATED", result.getStatus());
        assertEquals(List.of("tenant", "users", "club", "news", "activity", "competition"), result.getModules());
        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(mapper).updateById(captor.capture());
        assertTrue(captor.getValue().getConfigValue().contains("\"status\":\"GENERATED\""));
    }

    @Test
    void generateShouldKeepCompetitionInsertPlaceholderCountAlignedWithArguments() {
        TenantContext.setTenantId(1L);
        SystemConfigMapper mapper = mock(SystemConfigMapper.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(mapper.selectOne(ArgumentMatchers.any())).thenReturn(null);
        when(jdbcTemplate.queryForObject(anyString(), ArgumentMatchers.eq(Integer.class), ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    Object[] args = trailingArguments(invocation, 2);
                    if (args.length == 2
                            && "rk_activity".equals(args[0])
                            && "competition_competitions".equals(args[1])) {
                        return 1;
                    }
                    return 0;
                });
        when(jdbcTemplate.update(anyString(), ArgumentMatchers.<Object[]>any())).thenReturn(0);
        DemoDataServiceImpl service = new DemoDataServiceImpl(mapper, jdbcTemplate, new ObjectMapper());

        service.generate();

        for (Invocation invocation : mockingDetails(jdbcTemplate).getInvocations()) {
            Object[] arguments = invocation.getArguments();
            if (arguments.length == 0 || !(arguments[0] instanceof String)) {
                continue;
            }
            String sql = (String) arguments[0];
            if (sql.startsWith("INSERT INTO rk_activity.competition_competitions")) {
                assertEquals(countPlaceholders(sql), trailingArguments(invocation, 1).length);
                return;
            }
        }
        throw new AssertionError("competition demo insert SQL was not executed");
    }

    private int countPlaceholders(String sql) {
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            if (sql.charAt(i) == '?') {
                count++;
            }
        }
        return count;
    }

    private Object[] trailingArguments(InvocationOnMock invocation, int startIndex) {
        Object[] arguments = invocation.getArguments();
        if (arguments.length <= startIndex) {
            return new Object[0];
        }
        if (arguments.length == startIndex + 1 && arguments[startIndex] instanceof Object[]) {
            return (Object[]) arguments[startIndex];
        }
        return Arrays.copyOfRange(arguments, startIndex, arguments.length);
    }
}
