package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.SaasVisualScreenVO;
import com.tianji.data.service.SaasVisualScreenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.tianji.data.constants.RedisConstants.KEY_SAAS_VISUAL_SCREEN;

@Slf4j
@Service
public class SaasVisualScreenServiceImpl implements SaasVisualScreenService {
    private static final int CACHE_TTL_SECONDS = 60;
    private static final List<String> TIME_COLUMNS = Arrays.asList("create_time", "created_at", "created_time");
    private static final String FRONTEND_CACHE_KEY_PATTERN = "rk:frontend-cache:*";
    private static final String NULL_MARKER = "__RK_NULL__";

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public SaasVisualScreenServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SaasVisualScreenVO getOverview(Long requestedTenantId) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String cacheKey = KEY_SAAS_VISUAL_SCREEN + tenantScope;

        SaasVisualScreenVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        SaasVisualScreenVO computed = compute(scopedTenantId, tenantScope);
        computed.getCache()
                .setHit(false)
                .setMode("computed")
                .setKey(cacheKey)
                .setTtlSeconds(CACHE_TTL_SECONDS)
                .setCachedAt(LocalDateTime.now());
        writeCache(cacheKey, computed);
        return computed;
    }

    private Long resolveTenantId(Long requestedTenantId) {
        if (TenantContext.isSuperAdmin()) {
            return requestedTenantId != null && requestedTenantId > 0 ? requestedTenantId : null;
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && currentTenantId > 0) {
            return currentTenantId;
        }
        return requestedTenantId != null && requestedTenantId > 0 ? requestedTenantId : 1L;
    }

    private SaasVisualScreenVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isEmpty()) {
                return null;
            }
            return JsonUtils.toBean(json, SaasVisualScreenVO.class);
        } catch (Exception e) {
            log.warn("read SaaS visual screen cache failed: {}", e.getMessage());
            SaasVisualScreenVO fallback = null;
            return fallback;
        }
    }

    private void writeCache(String cacheKey, SaasVisualScreenVO data) {
        try {
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            data.getCache().setMode("fallback");
            log.warn("write SaaS visual screen cache failed: {}", e.getMessage());
        }
    }

    private SaasVisualScreenVO compute(Long tenantId, String tenantScope) {
        LocalDateTime now = LocalDateTime.now();
        SaasVisualScreenVO vo = new SaasVisualScreenVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setTenantName(resolveTenantName(tenantId))
                .setUpdatedAt(now);
        vo.getServiceHealth().setStatus("UP").setCheckedAt(now);

        vo.getTenantOverview()
                .setTenantCount(countAny(vo, "tenantCount", "tenant", null, null))
                .setActiveTenantCount(countAny(vo, "activeTenantCount", "tenant", "status = 1", null))
                .setCurrentTenantUsers(countAny(vo, "currentTenantUsers", "user", null, tenantId));

        vo.getPeople()
                .setMemberCount(countAny(vo, "memberCount", "member", null, tenantId))
                .setRoleCount(countAny(vo, "roleCount", "role", null, tenantId))
                .setOnlineCount(countAny(vo, "onlineCount", "login_log", todayCondition("login_time"), tenantId))
                .setTodayLoginCount(countAny(vo, "todayLoginCount", "login_log", todayCondition("login_time"), tenantId));

        vo.getContent()
                .setNewsCount(countAny(vo, "newsCount", "news", null, tenantId))
                .setNoticeCount(countAny(vo, "noticeCount", "notice", null, tenantId))
                .setTodayPublishedCount(
                        countAny(vo, "todayNewsPublishedCount", "news", todayAvailableCondition("news"), tenantId)
                                + countAny(vo, "todayNoticePublishedCount", "notice", todayAvailableCondition("notice"), tenantId));

        vo.getActivity()
                .setActivityCount(countAny(vo, "activityCount", "activity", null, tenantId))
                .setCompetitionCount(countAny(vo, "competitionCount", "competition", null, tenantId))
                .setRegistrationCount(countAny(vo, "registrationCount", "activity_registration", null, tenantId)
                        + countAny(vo, "competitionRegistrationCount", "competition_participant", null, tenantId))
                .setTodayRegistrationCount(countAny(vo, "todayActivityRegistrationCount", "activity_registration", todayAvailableCondition("activity_registration"), tenantId)
                        + countAny(vo, "todayCompetitionRegistrationCount", "competition_participant", todayAvailableCondition("competition_participant"), tenantId));

        vo.getApprovals()
                .setJoinPendingCount(countAny(vo, "joinPendingCount", "join_request", "status IN (0,1,'PENDING','pending')", tenantId))
                .setContentPendingCount(countAny(vo, "contentPendingCount", "news", "approval_status IN (0,1,'PENDING','pending')", tenantId))
                .setFinancePendingCount(countAny(vo, "financePendingCount", "finance_reimbursement", "status IN (0,'PENDING','pending')", tenantId));
        vo.getApprovals().setTotalPendingCount(vo.getApprovals().getJoinPendingCount()
                + vo.getApprovals().getContentPendingCount()
                + vo.getApprovals().getFinancePendingCount());

        vo.getInteractions()
                .setCommentCount(countAny(vo, "commentCount", "comments", null, tenantId))
                .setTodayCommentCount(countAny(vo, "todayCommentCount", "comments", todayAvailableCondition("comments"), tenantId))
                .setContactMessageCount(countAny(vo, "contactMessageCount", "contact_message", null, tenantId));

        vo.getGrowth()
                .setVolunteerHours(sumAny(vo, "volunteerHours", "volunteer_record", "service_hours", tenantId))
                .setCreditTotal(sumAny(vo, "creditTotal", "user_credit_record", "amount", tenantId))
                .setCreditRecordCount(countAny(vo, "creditRecordCount", "user_credit_record", null, tenantId));

        vo.getFinance()
                .setBudgetAmount(sumAny(vo, "budgetAmount", "finance_budget", "amount", tenantId))
                .setReimbursementAmount(sumAny(vo, "reimbursementAmount", "finance_reimbursement", "amount", tenantId))
                .setReimbursementPendingCount(countAny(vo, "reimbursementPendingCount", "finance_reimbursement", "status IN (0,'PENDING','pending')", tenantId));

        vo.getNotifications()
                .setNotificationCount(countAny(vo, "notificationCount", "notification", null, tenantId))
                .setEmailTaskCount(countAny(vo, "emailTaskCount", "email_send_task", null, tenantId))
                .setTodayReachCount(countAny(vo, "todayNotificationReachCount", "notification", todayAvailableCondition("notification"), tenantId)
                        + countAny(vo, "todayEmailReachCount", "email_send_task", todayAvailableCondition("email_send_task"), tenantId));
        fillFrontendCacheSnapshot(vo);
        fillSearchHealth(vo, tenantId);
        return vo;
    }

    private void fillFrontendCacheSnapshot(SaasVisualScreenVO vo) {
        SaasVisualScreenVO.CacheSnapshot snapshot = vo.getCacheSnapshot();
        try {
            RedisCallback<Void> callback = (RedisConnection connection) -> {
                scanFrontendCacheSnapshot(snapshot, connection, key -> {
                    try {
                        return redisTemplate.opsForValue().get(key);
                    } catch (RuntimeException ignored) {
                        return null;
                    }
                });
                return null;
            };
            redisTemplate.execute(callback);
            completeFrontendCacheSnapshot(snapshot);
            addSource(vo, "cacheSnapshot", FRONTEND_CACHE_KEY_PATTERN, "ok");
        } catch (RuntimeException e) {
            snapshot.setTotalSizeText("Redis unavailable");
            addSource(vo, "cacheSnapshot", FRONTEND_CACHE_KEY_PATTERN, "fallback:" + e.getClass().getSimpleName());
        }
    }

    void scanFrontendCacheSnapshot(
            SaasVisualScreenVO.CacheSnapshot snapshot,
            RedisConnection connection,
            Function<String, String> valueReader
    ) {
        ScanOptions options = ScanOptions.scanOptions().match("rk:frontend-cache:*").count(500).build();
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next(), StandardCharsets.UTF_8);
                snapshot.setTotalKeys(snapshot.getTotalKeys() + 1);
                snapshot.setKeyCount(snapshot.getTotalKeys());
                if (key.contains(":warmup:")) {
                    snapshot.setWarmupKeyCount(snapshot.getWarmupKeyCount() + 1);
                }
                String value = valueReader.apply(key);
                if (NULL_MARKER.equals(value)) {
                    snapshot.setNullMarkerCount(snapshot.getNullMarkerCount() + 1);
                    snapshot.setNullHitCount(snapshot.getNullMarkerCount());
                }
                long bytes = value == null ? 0L : value.getBytes(StandardCharsets.UTF_8).length;
                snapshot.setTotalBytes(snapshot.getTotalBytes() + bytes);
            }
        }
    }

    void completeFrontendCacheSnapshot(SaasVisualScreenVO.CacheSnapshot snapshot) {
        snapshot.setNormalKeyCount(Math.max(0L, snapshot.getTotalKeys() - snapshot.getWarmupKeyCount() - snapshot.getNullMarkerCount()));
        snapshot.setRandomExpireCount(Math.max(0L, snapshot.getTotalKeys() - snapshot.getWarmupKeyCount()));
        snapshot.setTotalSizeText(formatBytes(snapshot.getTotalBytes()) + " / " + snapshot.getTotalKeys() + " keys");
    }

    private void fillSearchHealth(SaasVisualScreenVO vo, Long tenantId) {
        SaasVisualScreenVO.SearchHealth searchHealth = vo.getSearchHealth();
        TableRef table = resolveTable("search_document");
        if (table == null) {
            searchHealth.setIndexStatus("MISSING");
            addSource(vo, "searchHealth", "search_document", "missing");
            return;
        }
        QueryParts where = buildWhere(table, null, tenantId);
        try {
            Long documentCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM " + table.qualifiedName() + where.sql,
                    Long.class,
                    where.args.toArray());
            searchHealth.setDocumentCount(documentCount == null ? 0L : documentCount);
            if (columnExists(table, "updated_at")) {
                LocalDateTime lastIndexedAt = jdbcTemplate.queryForObject(
                        "SELECT MAX(updated_at) FROM " + table.qualifiedName() + where.sql,
                        LocalDateTime.class,
                        where.args.toArray());
                searchHealth.setLastIndexedAt(lastIndexedAt);
                searchHealth.setStaleDocumentCount(countStaleSearchDocuments(table, where));
            }
            searchHealth.setIndexStatus(searchHealth.getDocumentCount() > 0 ? "UP" : "EMPTY");
            addSource(vo, "searchHealth", table.displayName(), "ok");
        } catch (RuntimeException e) {
            searchHealth.setIndexStatus("ERROR");
            addSource(vo, "searchHealth", table.displayName(), "fallback:" + e.getClass().getSimpleName());
        }
    }

    private long countStaleSearchDocuments(TableRef table, QueryParts baseWhere) {
        String staleCondition = "updated_at < '" + LocalDateTime.now().minusHours(24) + "'";
        String sql = "SELECT COUNT(1) FROM " + table.qualifiedName() + baseWhere.sql
                + (baseWhere.sql.isEmpty() ? " WHERE " : " AND ") + staleCondition;
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, baseWhere.args.toArray());
            return value == null ? 0L : value;
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private String resolveTenantName(Long tenantId) {
        if (tenantId == null) {
            return "全平台";
        }
        return "租户 " + tenantId;
    }

    private long countAny(SaasVisualScreenVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
        QueryParts where = buildWhere(table, extraCondition, tenantId);
        String sql = "SELECT COUNT(1) FROM " + table.qualifiedName() + where.sql;
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, where.args.toArray());
            addSource(vo, metric, table.displayName(), "ok");
            return value == null ? 0L : value;
        } catch (RuntimeException e) {
            addSource(vo, metric, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0L;
        }
    }

    private double sumAny(SaasVisualScreenVO vo, String metric, String baseTable, String column, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null || !columnExists(table, column)) {
            addSource(vo, metric, baseTable, "missing");
            return 0D;
        }
        QueryParts where = buildWhere(table, null, tenantId);
        String sql = "SELECT COALESCE(SUM(`" + column + "`),0) FROM " + table.qualifiedName() + where.sql;
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, metric, table.displayName(), "ok");
            return value == null ? 0D : value.doubleValue();
        } catch (RuntimeException e) {
            addSource(vo, metric, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0D;
        }
    }

    private TableRef resolveTable(String baseTable) {
        for (String candidate : tableCandidates(baseTable)) {
            TableRef table = TableRef.parse(candidate);
            if (tableExists(candidate)) {
                return table;
            }
        }
        return null;
    }

    private List<String> tableCandidates(String baseTable) {
        Map<String, List<String>> mapped = Map.ofEntries(
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant")),
                Map.entry("user", List.of("rk_user.rk_user", "rk_user")),
                Map.entry("member", List.of("rk_user.club_members", "club_members")),
                Map.entry("role", List.of("rk_auth.role", "role")),
                Map.entry("login_log", List.of("rk_user.sys_logininfor", "sys_logininfor")),
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("notice", List.of("rk_message.rk_notice", "rk_notice")),
                Map.entry("activity", List.of("rk_activity.rk_activity", "rk_activity")),
                Map.entry("competition", List.of("rk_activity.competition_competitions", "competition_competitions")),
                Map.entry("activity_registration", List.of("rk_activity.rk_activity_registration", "rk_activity_registration")),
                Map.entry("competition_participant", List.of("rk_activity.competition_participants", "competition_participants")),
                Map.entry("join_request", List.of("rk_user.join_requests", "join_requests")),
                Map.entry("comments", List.of("rk_content.comments", "comments")),
                Map.entry("contact_message", List.of("rk_message.contact_us_messages", "contact_us_messages")),
                Map.entry("volunteer_record", List.of("rk_user.volunteer_record", "volunteer_record")),
                Map.entry("user_credit_record", List.of("rk_user.user_credit_record", "user_credit_record")),
                Map.entry("finance_budget", List.of("rk_user.finance_budget", "finance_budget")),
                Map.entry("finance_reimbursement", List.of("rk_user.finance_reimbursement", "finance_reimbursement")),
                Map.entry("notification", List.of("rk_user.rk_notification", "rk_notification")),
                Map.entry("email_send_task", List.of("rk_user.email_send_task", "email_send_task")),
                Map.entry("search_document", List.of("rk_search.rk_search_index", "rk_search_index"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        candidates.add("rk_" + baseTable);
        candidates.add("sys_" + baseTable);
        return candidates;
    }

    private boolean tableExists(String tableName) {
        return tableExists(TableRef.parse(tableName));
    }

    private boolean tableExists(TableRef table) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = COALESCE(?, DATABASE()) AND table_name = ?",
                    Integer.class,
                    table.schema,
                    table.table);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean columnExists(TableRef table, String columnName) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.columns WHERE table_schema = COALESCE(?, DATABASE()) AND table_name = ? AND column_name = ?",
                    Integer.class,
                    table.schema,
                    table.table,
                    columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private QueryParts buildWhere(TableRef table, String extraCondition, Long tenantId) {
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();
        if (tenantId != null && columnExists(table, "tenant_id")) {
            where.append(" WHERE tenant_id = ?");
            args.add(tenantId);
        }
        String safeCondition = filterConditionByExistingColumns(table, extraCondition);
        if (safeCondition != null && !safeCondition.isEmpty()) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append(safeCondition);
        }
        return new QueryParts(where.toString(), args);
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        String filtered = condition;
        for (String column : List.of("is_deleted", "status", "approval_status")) {
            if (!columnExists(table, column)) {
                filtered = removeConditionForColumn(filtered, column);
            }
        }
        filtered = filtered.replaceAll("(?i)\\s+AND\\s+AND\\s+", " AND ");
        filtered = filtered.replaceAll("(?i)^\\s*AND\\s+", "");
        filtered = filtered.replaceAll("(?i)\\s+AND\\s*$", "");
        return filtered.trim();
    }

    private String removeConditionForColumn(String condition, String column) {
        return condition
                .replaceAll("(?i)\\s*AND\\s*\\(?\\s*" + column + "\\s+(IN|=)[^)]+\\)?", "")
                .replaceAll("(?i)\\(?\\s*" + column + "\\s+(IN|=)[^)]+\\)?\\s*AND\\s*", "");
    }

    private String todayAvailableCondition(String tableName) {
        TableRef resolved = resolveTable(tableName);
        if (resolved == null) {
            return null;
        }
        for (String column : TIME_COLUMNS) {
            if (columnExists(resolved, column)) {
                return todayCondition(column);
            }
        }
        return null;
    }

    private String todayCondition(String columnName) {
        LocalDate today = LocalDate.now();
        return columnName + " >= '" + today + " 00:00:00' AND " + columnName + " < '" + today.plusDays(1) + " 00:00:00'";
    }

    private void addSource(SaasVisualScreenVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new SaasVisualScreenVO.SourceInfo()
                .setMetric(metric)
                .setTableName(tableName)
                .setStatus(status.toLowerCase(Locale.ROOT))
                .setUpdatedAt(LocalDateTime.now()));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024D;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024D;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        return String.format(Locale.ROOT, "%.1f GB", mb / 1024D);
    }

    private static class QueryParts {
        private final String sql;
        private final List<Object> args;

        private QueryParts(String sql, List<Object> args) {
            this.sql = sql;
            this.args = args;
        }
    }

    private static class TableRef {
        private final String schema;
        private final String table;

        private TableRef(String schema, String table) {
            this.schema = schema;
            this.table = table;
        }

        static TableRef parse(String value) {
            String[] parts = value.split("\\.", 2);
            if (parts.length == 2) {
                return new TableRef(parts[0], parts[1]);
            }
            return new TableRef(null, value);
        }

        String qualifiedName() {
            if (schema == null) {
                return "`" + table + "`";
            }
            return "`" + schema + "`.`" + table + "`";
        }

        String displayName() {
            return schema == null ? table : schema + "." + table;
        }
    }
}
