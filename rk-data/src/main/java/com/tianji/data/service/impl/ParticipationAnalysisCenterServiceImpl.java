package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ParticipationAnalysisCenterVO;
import com.tianji.data.service.ParticipationAnalysisCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tianji.data.constants.RedisConstants.KEY_PARTICIPATION_ANALYSIS_CENTER;

@Slf4j
@Service
public class ParticipationAnalysisCenterServiceImpl implements ParticipationAnalysisCenterService {
    private static final int CACHE_TTL_SECONDS = 180;
    private static final int CACHE_TTL_JITTER_SECONDS = 90;
    private static final List<String> TIME_COLUMNS = Arrays.asList("create_time", "created_at", "created_time", "registration_time", "login_time", "update_time", "updated_at");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public ParticipationAnalysisCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ParticipationAnalysisCenterVO getParticipationAnalysisCenter(Long requestedTenantId, String dimension, String timeRange) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedDimension = normalizeFilter(dimension);
        String normalizedTimeRange = normalizeTimeRange(timeRange);
        String cacheKey = cacheKey(tenantScope, normalizedDimension, normalizedTimeRange);

        ParticipationAnalysisCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        ParticipationAnalysisCenterVO computed = compute(scopedTenantId, tenantScope, normalizedDimension, normalizedTimeRange);
        computed.getCache()
                .setHit(false)
                .setMode("computed")
                .setKey(cacheKey)
                .setTtlSeconds(CACHE_TTL_SECONDS)
                .setCachedAt(LocalDateTime.now());
        writeCache(cacheKey, computed);
        return computed;
    }

    @Override
    public List<ParticipationAnalysisCenterVO> warmupAllTenants() {
        List<ParticipationAnalysisCenterVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            ParticipationAnalysisCenterVO computed = compute(tenantId, String.valueOf(tenantId), "ALL", "30D");
            computed.getCache()
                    .setHit(false)
                    .setMode("warmup")
                    .setKey(cacheKey(String.valueOf(tenantId), "ALL", "30D"))
                    .setTtlSeconds(CACHE_TTL_SECONDS)
                    .setCachedAt(LocalDateTime.now());
            writeCache(computed.getCache().getKey(), computed);
            warmed.add(computed);
        }
        return warmed;
    }

    private ParticipationAnalysisCenterVO compute(Long tenantId, String tenantScope, String dimension, String timeRange) {
        ParticipationAnalysisCenterVO vo = new ParticipationAnalysisCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setDimension(dimension)
                .setTimeRange(timeRange);

        String sinceCondition = sinceCondition(resolveTimeColumnFallback(timeRange), daysForRange(timeRange));
        fillSummary(vo, tenantId, sinceCondition);
        fillMetrics(vo);
        fillTrendRows(vo, tenantId, dimension, sinceCondition);
        fillTrend(vo);
        return vo;
    }

    private void fillSummary(ParticipationAnalysisCenterVO vo, Long tenantId, String sinceCondition) {
        ParticipationAnalysisCenterVO.Summary summary = vo.getSummary();
        long activityRegistration = countAny(vo, "participation.activity_registration", "activity_registration", sinceCondition, tenantId);
        long competitionRegistration = countAny(vo, "participation.competition_participants", "competition_participants", sinceCondition, tenantId);
        long newsView = sumAny(vo, "participation.news_view", "news", List.of("view_count", "views", "read_count", "browse_count"), tenantId);
        long worksView = sumAny(vo, "participation.works_view", "works", List.of("view_count", "views", "read_count", "browse_count"), tenantId);
        long commentCount = countAny(vo, "participation.comments", "comments", sinceCondition, tenantId);
        long followCount = followCount(vo, tenantId, sinceCondition);
        long revisitCount = revisitCount(vo, tenantId, sinceCondition);
        long activeMembers = activeMemberCount(vo, tenantId);

        summary
                .setViewCount(newsView + worksView)
                .setActivityRegistrationCount(activityRegistration)
                .setCompetitionRegistrationCount(competitionRegistration)
                .setCommentCount(commentCount)
                .setFollowCount(followCount)
                .setRevisitCount(revisitCount)
                .setActiveMemberCount(activeMembers)
                .setRegistrationConversionRate(roundRate(activityRegistration + competitionRegistration, Math.max(1, activeMembers)))
                .setInteractionRate(roundRate(commentCount + followCount + revisitCount, Math.max(1, activeMembers)));
    }

    private void fillMetrics(ParticipationAnalysisCenterVO vo) {
        ParticipationAnalysisCenterVO.Summary summary = vo.getSummary();
        vo.getMetrics().add(metric("viewCount", "浏览转化", summary.getViewCount(), "新闻与作品浏览量", "rk_content.news/rk_content.works"));
        vo.getMetrics().add(metric("registrationConversionRate", "报名转化", Math.round(summary.getRegistrationConversionRate()), "报名人数 / 活跃成员", "rk_activity.rk_activity_registration"));
        vo.getMetrics().add(metric("commentCount", "评论互动", summary.getCommentCount(), "新闻、活动和作品评论", "rk_content.comments"));
        vo.getMetrics().add(metric("followCount", "关注与复访", summary.getFollowCount() + summary.getRevisitCount(), "关注行为与重复登录", "rk_user/sys_logininfor"));
        vo.getMetrics().add(metric("activeMemberCount", "活跃成员", summary.getActiveMemberCount(), "当前租户可触达成员", "rk_user.club_members"));
    }

    private ParticipationAnalysisCenterVO.MetricItem metric(String metric, String label, long value, String hint, String source) {
        return new ParticipationAnalysisCenterVO.MetricItem()
                .setMetric(metric)
                .setLabel(label)
                .setValue(value)
                .setHint(hint)
                .setSource(source);
    }

    private void fillTrendRows(ParticipationAnalysisCenterVO vo, Long tenantId, String dimension, String sinceCondition) {
        List<ParticipationAnalysisCenterVO.TrendItem> rows = new ArrayList<>();
        rows.add(new ParticipationAnalysisCenterVO.TrendItem()
                .setDimension("activity")
                .setLabel("活动参与")
                .setRegistrationCount(countAny(vo, "engagementTrend.activity", "activity_registration", sinceCondition, tenantId))
                .setCommentCount(countByTarget(vo, "engagementTrend.activity_comment", "activity", sinceCondition, tenantId))
                .setRevisitCount(revisitCount(vo, tenantId, sinceCondition)));
        rows.add(new ParticipationAnalysisCenterVO.TrendItem()
                .setDimension("competition")
                .setLabel("比赛参与")
                .setRegistrationCount(countAny(vo, "engagementTrend.competition", "competition_participants", sinceCondition, tenantId))
                .setCommentCount(countByTarget(vo, "engagementTrend.competition_comment", "competition", sinceCondition, tenantId)));
        rows.add(new ParticipationAnalysisCenterVO.TrendItem()
                .setDimension("content")
                .setLabel("内容互动")
                .setViewCount(sumAny(vo, "engagementTrend.content_view", "news", List.of("view_count", "views", "read_count", "browse_count"), tenantId)
                        + sumAny(vo, "engagementTrend.works_view", "works", List.of("view_count", "views", "read_count", "browse_count"), tenantId))
                .setCommentCount(countByTarget(vo, "engagementTrend.content_comment", "news", sinceCondition, tenantId)
                        + countByTarget(vo, "engagementTrend.works_comment", "works", sinceCondition, tenantId))
                .setFollowCount(followCount(vo, tenantId, sinceCondition)));
        rows.add(new ParticipationAnalysisCenterVO.TrendItem()
                .setDimension("member")
                .setLabel("成员活跃")
                .setViewCount(activeMemberCount(vo, tenantId))
                .setRevisitCount(revisitCount(vo, tenantId, sinceCondition)));

        vo.setTrendRows(rows.stream()
                .filter(row -> "ALL".equals(dimension) || dimension.equalsIgnoreCase(row.getDimension()))
                .collect(Collectors.toList()));
    }

    private void fillTrend(ParticipationAnalysisCenterVO vo) {
        long totalView = vo.getTrendRows().stream().mapToLong(ParticipationAnalysisCenterVO.TrendItem::getViewCount).sum();
        long totalRegistration = vo.getTrendRows().stream().mapToLong(ParticipationAnalysisCenterVO.TrendItem::getRegistrationCount).sum();
        long totalComment = vo.getTrendRows().stream().mapToLong(ParticipationAnalysisCenterVO.TrendItem::getCommentCount).sum();
        long totalFollow = vo.getTrendRows().stream().mapToLong(ParticipationAnalysisCenterVO.TrendItem::getFollowCount).sum();
        long totalRevisit = vo.getTrendRows().stream().mapToLong(ParticipationAnalysisCenterVO.TrendItem::getRevisitCount).sum();
        vo.getTrend()
                .setTotalViewCount(totalView)
                .setTotalRegistrationCount(totalRegistration)
                .setTotalCommentCount(totalComment)
                .setTotalFollowCount(totalFollow)
                .setTotalRevisitCount(totalRevisit);
        vo.setEngagementTrend(vo.getTrend());
    }

    private long countByTarget(ParticipationAnalysisCenterVO vo, String metric, String targetType, String sinceCondition, Long tenantId) {
        TableRef table = resolveTable("comments");
        if (table == null) {
            addSource(vo, metric, "comments", "missing");
            return 0L;
        }
        String condition = filterConditionByExistingColumns(table, sinceCondition);
        if (columnExists(table, "target_type")) {
            condition = appendCondition(condition, "target_type = '" + targetType + "'");
        }
        return countTable(vo, metric, table, condition, tenantId);
    }

    private long followCount(ParticipationAnalysisCenterVO vo, Long tenantId, String sinceCondition) {
        for (String baseTable : List.of("user_follow", "follow", "club_follow", "content_follow")) {
            TableRef table = resolveTable(baseTable);
            if (table == null) {
                continue;
            }
            return countTable(vo, "participation.followCount", table, filterConditionByExistingColumns(table, sinceCondition), tenantId);
        }
        addSource(vo, "participation.followCount", "follow candidates", "missing");
        return 0L;
    }

    private long revisitCount(ParticipationAnalysisCenterVO vo, Long tenantId, String sinceCondition) {
        TableRef table = resolveTable("login_log");
        if (table == null) {
            addSource(vo, "participation.revisitCount", "sys_logininfor", "missing");
            return 0L;
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, sinceCondition), tenantId);
        String loginName = firstExistingColumn(table, List.of("login_name", "user_name", "username"));
        if (loginName == null) {
            addSource(vo, "participation.revisitCount", table.displayName(), "missing-columns");
            return 0L;
        }
        String sql = "SELECT COUNT(1) FROM (SELECT `" + loginName + "` FROM " + table.qualifiedName()
                + where.sql + " GROUP BY `" + loginName + "` HAVING COUNT(1) > 1) t";
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, "participation.revisitCount", table.displayName(), "ok");
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException e) {
            addSource(vo, "participation.revisitCount", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0L;
        }
    }

    private long activeMemberCount(ParticipationAnalysisCenterVO vo, Long tenantId) {
        TableRef table = resolveTable("member");
        if (table == null) {
            addSource(vo, "participation.activeMemberCount", "club_members", "missing");
            return 0L;
        }
        String condition = columnExists(table, "status") ? "status IN (1,'ACTIVE','active','在社','正常')" : null;
        return countTable(vo, "participation.activeMemberCount", table, filterConditionByExistingColumns(table, condition), tenantId);
    }

    private long countAny(ParticipationAnalysisCenterVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
        return countTable(vo, metric, table, filterConditionByExistingColumns(table, extraCondition), tenantId);
    }

    private long countTable(ParticipationAnalysisCenterVO vo, String metric, TableRef table, String extraCondition, Long tenantId) {
        QueryParts where = buildWhere(table, extraCondition, tenantId);
        String sql = "SELECT COUNT(1) FROM " + table.qualifiedName() + where.sql;
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, metric, table.displayName(), "ok");
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException e) {
            addSource(vo, metric, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0L;
        }
    }

    private long sumAny(ParticipationAnalysisCenterVO vo, String metric, String baseTable, List<String> candidateColumns, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
        String column = firstExistingColumn(table, candidateColumns);
        if (column == null) {
            addSource(vo, metric, table.displayName(), "missing-columns");
            return 0L;
        }
        QueryParts where = buildWhere(table, null, tenantId);
        String sql = "SELECT COALESCE(SUM(`" + column + "`),0) FROM " + table.qualifiedName() + where.sql;
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, metric, table.displayName(), "ok");
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException e) {
            addSource(vo, metric, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0L;
        }
    }

    private Long resolveTenantId(Long requestedTenantId) {
        if (Boolean.TRUE.equals(TenantContext.isSuperAdmin())) {
            return requestedTenantId != null && requestedTenantId > 0 ? requestedTenantId : null;
        }
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && currentTenantId > 0) {
            return currentTenantId;
        }
        return requestedTenantId != null && requestedTenantId > 0 ? requestedTenantId : 1L;
    }

    private List<Long> listTenantIds() {
        TableRef table = resolveTable("tenant");
        if (table == null) {
            return fallbackTenantIds();
        }
        String sql = "SELECT id FROM " + table.qualifiedName() + optionalWhere(table, "is_deleted = 0") + " ORDER BY id ASC LIMIT 200";
        try {
            List<Long> tenantIds = jdbcTemplate.queryForList(sql, Long.class);
            List<Long> filtered = tenantIds == null ? List.of() : tenantIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> id > 0)
                    .collect(Collectors.toList());
            return filtered.isEmpty() ? fallbackTenantIds() : filtered;
        } catch (RuntimeException e) {
            log.warn("list tenant ids for participation analysis failed: {}", e.getMessage());
            return fallbackTenantIds();
        }
    }

    private List<Long> fallbackTenantIds() {
        Long fallbackTenantId = resolveTenantId(null);
        return fallbackTenantId == null || fallbackTenantId <= 0 ? List.of(1L) : List.of(fallbackTenantId);
    }

    private ParticipationAnalysisCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, ParticipationAnalysisCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read participation analysis center cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, ParticipationAnalysisCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write participation analysis center cache failed: {}", e.getMessage());
        }
    }

    private String cacheKey(String tenantScope, String dimension, String timeRange) {
        return KEY_PARTICIPATION_ANALYSIS_CENTER + tenantScope + ":" + dimension + ":" + timeRange;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeRange(String value) {
        if (value == null || value.isBlank()) {
            return "30D";
        }
        String normalized = normalizeFilter(value);
        return List.of("7D", "30D", "90D", "ALL").contains(normalized) ? normalized : "30D";
    }

    private int daysForRange(String timeRange) {
        if ("7D".equals(timeRange)) {
            return 7;
        }
        if ("90D".equals(timeRange)) {
            return 90;
        }
        if ("ALL".equals(timeRange)) {
            return 3650;
        }
        return 30;
    }

    private String resolveTimeColumnFallback(String timeRange) {
        return "create_time";
    }

    private String sinceCondition(String columnName, int days) {
        LocalDate today = LocalDate.now();
        return columnName + " >= '" + today.minusDays(days) + " 00:00:00'";
    }

    private double roundRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round(numerator * 10000D / denominator) / 100D;
    }

    private TableRef resolveTable(String baseTable) {
        for (String candidate : tableCandidates(baseTable)) {
            TableRef table = TableRef.parse(candidate);
            if (tableExists(table)) {
                return table;
            }
        }
        return null;
    }

    private List<String> tableCandidates(String baseTable) {
        Map<String, List<String>> mapped = Map.ofEntries(
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant")),
                Map.entry("member", List.of("rk_user.club_members", "club_members")),
                Map.entry("login_log", List.of("rk_user.sys_logininfor", "sys_logininfor")),
                Map.entry("activity_registration", List.of("rk_activity.rk_activity_registration", "rk_activity_registration")),
                Map.entry("competition_participants", List.of("rk_activity.competition_participants", "competition_participants")),
                Map.entry("activity", List.of("rk_activity.rk_activity", "rk_activity")),
                Map.entry("competition", List.of("rk_activity.competition_competitions", "competition_competitions")),
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("works", List.of("rk_content.works", "works")),
                Map.entry("comments", List.of("rk_content.comments", "comments")),
                Map.entry("rk_notification", List.of("rk_message.rk_notification", "rk_notification")),
                Map.entry("rk_user_inbox", List.of("rk_message.rk_user_inbox", "rk_user_inbox")),
                Map.entry("user_follow", List.of("rk_user.user_follow", "user_follow")),
                Map.entry("follow", List.of("rk_user.follow", "follow", "rk_content.follow")),
                Map.entry("club_follow", List.of("rk_user.club_follow", "club_follow")),
                Map.entry("content_follow", List.of("rk_content.content_follow", "content_follow"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        candidates.add("rk_" + baseTable);
        return candidates;
    }

    private String optionalWhere(TableRef table, String condition) {
        String safeCondition = filterConditionByExistingColumns(table, condition);
        return safeCondition == null || safeCondition.isEmpty() ? "" : " WHERE " + safeCondition;
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
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("(").append(safeCondition).append(")");
        }
        return new QueryParts(where.toString(), args);
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (condition == null || condition.isBlank()) {
            return null;
        }
        String filtered = condition.trim();
        for (String column : List.of("is_deleted", "status", "create_time", "created_at", "created_time", "registration_time", "login_time", "target_type")) {
            if (filtered.toLowerCase(Locale.ROOT).contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return filtered;
    }

    private String appendCondition(String base, String extra) {
        if (base == null || base.isBlank()) {
            return extra;
        }
        return "(" + base + ") AND (" + extra + ")";
    }

    private String firstExistingColumn(TableRef table, List<String> columns) {
        for (String column : columns) {
            if (columnExists(table, column)) {
                return column;
            }
        }
        return null;
    }

    private boolean tableExists(TableRef table) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(1) FROM information_schema.tables WHERE table_schema = COALESCE(?, DATABASE()) AND table_name = ?",
                    Integer.class,
                    table.schema,
                    table.table);
            return count != null && count > 0;
        } catch (RuntimeException e) {
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
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void addSource(ParticipationAnalysisCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new ParticipationAnalysisCenterVO.SourceInfo()
                .setMetric(metric)
                .setTableName(tableName)
                .setStatus(status.toLowerCase(Locale.ROOT))
                .setUpdatedAt(LocalDateTime.now()));
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
