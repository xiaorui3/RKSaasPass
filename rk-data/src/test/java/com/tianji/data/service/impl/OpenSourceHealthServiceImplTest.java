package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.model.vo.OpenSourceHealthVO;
import com.tianji.data.service.FrontendCacheWarmupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OpenSourceHealthServiceImplTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void checkShouldReturnTenantScopedBusinessHealthAndWarnForMissingDefaultRoles() {
        TenantContext.setTenantId(8L);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));
        when(warmupService.latestSummary()).thenReturn(FrontendCacheWarmupSummaryVO.builder()
                .redisAvailable(false)
                .degraded(true)
                .reason("redis unavailable")
                .checkedAt(LocalDateTime.now())
                .build());
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), org.mockito.ArgumentMatchers.<Object[]>any()))
                .thenAnswer(invocation -> {
                    String sql = invocation.getArgument(0, String.class);
                    Object[] args = invocation.getArguments();
                    Object lastArgument = args.length == 0 ? null : args[args.length - 1];
                    if (lastArgument instanceof Object[]) {
                        Object[] varargs = (Object[]) lastArgument;
                        lastArgument = varargs.length == 0 ? null : varargs[varargs.length - 1];
                    }
                    if (sql.contains("FROM `rk_auth`.`rk_menu`")) {
                        return 1L;
                    }
                    if (sql.contains("FROM `rk_auth`.`rk_role`")) {
                        String code = String.valueOf(lastArgument);
                        return "ADMIN".equals(code) || "USER".equals(code) ? 1L : 0L;
                    }
                    if (sql.contains("FROM `rk_search`.`rk_search_index`")) {
                        return 3L;
                    }
                    if (sql.contains("FROM `rk_user`.`rk_system_config`")) {
                        return 2L;
                    }
                    if (sql.contains("FROM `rk_user`.`rk_tenant`") || sql.contains("FROM `rk_content`.`rk_news`")) {
                        return 1L;
                    }
                    return 0L;
                });

        OpenSourceHealthServiceImpl service = new OpenSourceHealthServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        OpenSourceHealthVO result = service.check(99L);

        assertEquals(8L, result.getTenantId());
        assertEquals("8", result.getTenantScope());
        assertNotNull(result.getCheckedAt());
        assertEquals(7, result.getChecks().size());
        assertTrue(result.getSummary().getWarnCount() >= 2);
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "DEFAULT_ROLES".equals(item.getCode())
                        && "WARN".equals(item.getStatus())
                        && item.getSuggestion().contains("CLUB_MANAGER")
                        && item.getSuggestion().contains("TEACHER")), () -> result.getChecks().toString());
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "REDIS_CACHE".equals(item.getCode())
                        && !"PASS".equals(item.getStatus())
                        && item.getSuggestion().contains("预热")));
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "FILE_STORAGE".equals(item.getCode())
                        && item.getSuggestion().contains("文件")));
    }

    @Test
    void checkShouldDegradeMissingSourceTablesWithoutThrowing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), any(), any())).thenReturn(0);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        OpenSourceHealthServiceImpl service = new OpenSourceHealthServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        OpenSourceHealthVO result = service.check(null);

        assertNull(result.getTenantId());
        assertEquals("ALL", result.getTenantScope());
        assertEquals(7, result.getChecks().size());
        assertTrue(result.getSummary().getUnknownCount() >= 5);
        assertTrue(result.getChecks().stream().allMatch(item -> item.getDiagnostics() != null));
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "MENU_SEED".equals(item.getCode()) && "UNKNOWN".equals(item.getStatus())));
    }

    @Test
    void statusPriorityShouldExposeOverallFailWhenAnyCheckFails() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForList(contains("FROM `rk_auth`.`rk_menu`"), eq(String.class))).thenReturn(List.of());
        when(jdbcTemplate.queryForList(contains("FROM `rk_auth`.`rk_role`"), eq(String.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(List.of());
        when(jdbcTemplate.queryForObject(contains("COUNT(1)"), eq(Long.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(0L);

        OpenSourceHealthServiceImpl service = new OpenSourceHealthServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        OpenSourceHealthVO result = service.check(null);

        assertEquals("FAIL", result.getOverallStatus());
        assertTrue(result.getSummary().getFailCount() > 0);
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "MENU_SEED".equals(item.getCode())
                        && "FAIL".equals(item.getStatus())
                        && item.getDiagnostics().contains("缺失")));
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "DEFAULT_ROLES".equals(item.getCode())
                        && "FAIL".equals(item.getStatus())
                        && item.getSuggestion().contains("默认角色")));
    }

    @Test
    void checkShouldReturnUnknownItemWhenSingleCheckThrowsUnexpectedException() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), any(), any()))
                .thenThrow(new AssertionError("metadata driver crashed"));
        when(redisTemplate.hasKey("FRONTEND:CACHE:WARMUP:SUMMARY")).thenReturn(true);
        when(warmupService.latestSummary()).thenReturn(FrontendCacheWarmupSummaryVO.builder()
                .redisAvailable(true)
                .failureCount(0)
                .missCount(0)
                .keyCount(3)
                .checkedAt(LocalDateTime.now())
                .build());

        OpenSourceHealthServiceImpl service = new OpenSourceHealthServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        OpenSourceHealthVO result = service.check(null);

        assertEquals(7, result.getChecks().size());
        assertEquals("UNKNOWN", result.getOverallStatus());
        assertTrue(result.getChecks().stream().allMatch(item -> item.getDiagnostics() != null));
        assertTrue(result.getChecks().stream().anyMatch(item ->
                "MENU_SEED".equals(item.getCode())
                        && "UNKNOWN".equals(item.getStatus())
                        && item.getDiagnostics().contains("metadata driver crashed")));
    }
    @Test
    void checkShouldUseBoundedQueriesAndNotParseFullWarmupSummary() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        FrontendCacheWarmupService warmupService = mock(FrontendCacheWarmupService.class);
        when(jdbcTemplate.queryForObject(contains("information_schema.tables"), eq(Integer.class), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(contains("information_schema.columns"), eq(Integer.class), any(), any(), any())).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), org.mockito.ArgumentMatchers.<Object[]>any())).thenReturn(1L);
        when(redisTemplate.hasKey("FRONTEND:CACHE:WARMUP:SUMMARY")).thenReturn(true);

        OpenSourceHealthServiceImpl service = new OpenSourceHealthServiceImpl(jdbcTemplate, redisTemplate, warmupService);

        OpenSourceHealthVO result = service.check(1L);

        assertEquals(7, result.getChecks().size());
        verify(jdbcTemplate, never()).queryForList(contains("SELECT path"), eq(String.class));
        verify(jdbcTemplate, never()).queryForList(contains("SELECT code"), eq(String.class), org.mockito.ArgumentMatchers.<Object[]>any());
        verify(warmupService, never()).latestSummary();
    }
}
