package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.GrowthRetentionCenterVO;
import com.tianji.data.service.GrowthRetentionCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tianji.data.constants.RedisConstants.KEY_GROWTH_RETENTION_CENTER;

@Slf4j
@Service
public class GrowthRetentionCenterServiceImpl implements GrowthRetentionCenterService {
    private static final int CACHE_TTL_SECONDS = 240;
    private static final int CACHE_TTL_JITTER_SECONDS = 120;
    private static final List<String> TIME_COLUMNS = Arrays.asList("create_time", "created_at", "created_time", "join_date", "login_time", "service_date", "last_updated", "update_time");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public GrowthRetentionCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GrowthRetentionCenterVO getGrowthRetentionCenter(Long requestedTenantId, String timeRange, String riskLevel) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedTimeRange = normalizeTimeRange(timeRange);
        String normalizedRiskLevel = normalizeFilter(riskLevel);
        String cacheKey = cacheKey(tenantScope, normalizedTimeRange, normalizedRiskLevel);

        GrowthRetentionCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        GrowthRetentionCenterVO computed = compute(scopedTenantId, tenantScope, normalizedTimeRange, normalizedRiskLevel);
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
    public List<GrowthRetentionCenterVO> warmupAllTenants() {
        List<GrowthRetentionCenterVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            GrowthRetentionCenterVO computed = compute(tenantId, String.valueOf(tenantId), "90D", "ALL");
            computed.getCache()
                    .setHit(false)
                    .setMode("warmup")
                    .setKey(cacheKey(String.valueOf(tenantId), "90D", "ALL"))
                    .setTtlSeconds(CACHE_TTL_SECONDS)
                    .setCachedAt(LocalDateTime.now());
            writeCache(computed.getCache().getKey(), computed);
            warmed.add(computed);
        }
        return warmed;
    }

    private GrowthRetentionCenterVO compute(Long tenantId, String tenantScope, String timeRange, String riskLevel) {
        GrowthRetentionCenterVO vo = new GrowthRetentionCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setTimeRange(timeRange)
                .setRiskLevel(riskLevel);

        fillSummary(vo, tenantId, timeRange);
        fillMetrics(vo);
        fillTrendRows(vo, tenantId);
        fillMonthlyTrend(vo);
        fillRisks(vo, riskLevel);
        return vo;
    }

    private void fillSummary(GrowthRetentionCenterVO vo, Long tenantId, String timeRange) {
        GrowthRetentionCenterVO.Summary summary = vo.getSummary();
        String memberSince = sinceCondition("create_time", daysForRange(timeRange));
        String activeSince = sinceCondition("login_time", Math.min(30, daysForRange(timeRange)));

        long newMembers = countAny(vo, "growth.newMemberCount", "member", memberSince, tenantId);
        long activeMembers = activeMemberCount(vo, tenantId, activeSince);
        long dormantMembers = dormantMemberCount(vo, tenantId, activeSince);
        long lostMembers = countAny(vo, "growth.lostMemberCount", "member", "status IN (0,'LEFT','left','QUIT','quit','离社','退出')", tenantId);
        long activeClubs = activeClubCount(vo, tenantId);
        double volunteerHours = volunteerHours(vo, tenantId);
        double creditTotal = creditTotal(vo, tenantId);
        long creditRecordCount = countAny(vo, "growth.creditRecordCount", "user_credit_record", filterConditionByExistingColumns(resolveTable("user_credit_record"), "status IN (1,'APPROVED','approved')"), tenantId);

        summary
                .setNewMemberCount(newMembers)
                .setActiveMemberCount(activeMembers)
                .setDormantMemberCount(dormantMembers)
                .setLostMemberCount(lostMembers)
                .setActiveClubCount(activeClubs)
                .setVolunteerHours(volunteerHours)
                .setCreditTotal(creditTotal)
                .setCreditRecordCount(creditRecordCount)
                .setRetentionRate(roundRate(activeMembers, Math.max(1, activeMembers + dormantMembers + lostMembers)));
    }

    private void fillMetrics(GrowthRetentionCenterVO vo) {
        GrowthRetentionCenterVO.Summary summary = vo.getSummary();
        vo.getMetrics().add(metric("newMemberCount", "新增成员", summary.getNewMemberCount(), "按加入/创建时间统计", "rk_user.club_members"));
        vo.getMetrics().add(metric("activeClubCount", "活跃社团", summary.getActiveClubCount(), "有成员或登录活动的租户组织", "rk_user.club_members/sys_logininfor"));
        vo.getMetrics().add(metric("dormantMemberCount", "沉默成员", summary.getDormantMemberCount(), "近 30 天未登录或状态沉默", "rk_user.club_members"));
        vo.getMetrics().add(metric("retentionRate", "留存率", summary.getRetentionRate(), "活跃成员 / 可触达成员", "rk_user.club_members"));
        vo.getMetrics().add(metric("volunteerHours", "志愿时长", summary.getVolunteerHours(), "已通过志愿记录时长", "rk_user.volunteer_record"));
        vo.getMetrics().add(metric("creditTotal", "积分总额", summary.getCreditTotal(), "积分记录或积分汇总", "rk_user.user_credit_record/user_credit_summary"));
    }

    private GrowthRetentionCenterVO.MetricItem metric(String metric, String label, double value, String hint, String source) {
        return new GrowthRetentionCenterVO.MetricItem()
                .setMetric(metric)
                .setLabel(label)
                .setValue(value)
                .setHint(hint)
                .setSource(source);
    }

    private void fillTrendRows(GrowthRetentionCenterVO vo, Long tenantId) {
        List<GrowthRetentionCenterVO.TrendItem> rows = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            String range = monthRangeCondition(month);
            rows.add(new GrowthRetentionCenterVO.TrendItem()
                    .setMonth(month.toString())
                    .setNewMemberCount(countAny(vo, "growth.monthly.newMember." + month, "member", range, tenantId))
                    .setActiveMemberCount(activeMemberCount(vo, tenantId, loginRangeCondition(month)))
                    .setDormantMemberCount(dormantMemberCount(vo, tenantId, loginRangeCondition(month)))
                    .setVolunteerHours(volunteerHours(vo, tenantId, range))
                    .setCreditTotal(creditTotal(vo, tenantId, range)));
        }
        vo.setTrendRows(rows);
    }

    private void fillMonthlyTrend(GrowthRetentionCenterVO vo) {
        GrowthRetentionCenterVO.MonthlyTrend monthlyTrend = vo.getMonthlyTrend();
        monthlyTrend
                .setTotalNewMemberCount(vo.getTrendRows().stream().mapToLong(GrowthRetentionCenterVO.TrendItem::getNewMemberCount).sum())
                .setTotalActiveMemberCount(vo.getTrendRows().stream().mapToLong(GrowthRetentionCenterVO.TrendItem::getActiveMemberCount).sum())
                .setTotalDormantMemberCount(vo.getTrendRows().stream().mapToLong(GrowthRetentionCenterVO.TrendItem::getDormantMemberCount).sum())
                .setTotalVolunteerHours(roundDouble(vo.getTrendRows().stream().mapToDouble(GrowthRetentionCenterVO.TrendItem::getVolunteerHours).sum()))
                .setTotalCreditTotal(roundDouble(vo.getTrendRows().stream().mapToDouble(GrowthRetentionCenterVO.TrendItem::getCreditTotal).sum()));
    }

    private void fillRisks(GrowthRetentionCenterVO vo, String riskLevel) {
        List<GrowthRetentionCenterVO.RiskItem> risks = new ArrayList<>();
        GrowthRetentionCenterVO.Summary summary = vo.getSummary();
        if (summary.getDormantMemberCount() > 0) {
            risks.add(risk("dormant_member", "沉默成员预警", "MEDIUM", "NEED_FOLLOW_UP", "rk_user.club_members", "对沉默成员发起召回或活动推荐"));
        }
        if (summary.getRetentionRate() < 50D) {
            risks.add(risk("retention_low", "留存率偏低", "HIGH", "NEED_REVIEW", "rk_user.club_members", "复核入社后触达与活动供给"));
        }
        if (summary.getVolunteerHours() <= 0D) {
            risks.add(risk("volunteer_missing", "志愿时长为空", "LOW", "WATCH", "rk_user.volunteer_record", "确认是否已配置志愿服务记录"));
        }
        if (summary.getCreditTotal() <= 0D) {
            risks.add(risk("credit_missing", "积分总额为空", "LOW", "WATCH", "rk_user.user_credit_record", "确认第二课堂积分审核链路"));
        }
        if (risks.isEmpty()) {
            risks.add(risk("normal", "增长留存健康", "LOW", "NORMAL", "rk_data.aggregate", "保持定时预热和月度复盘"));
        }
        vo.setRiskRows(risks.stream()
                .filter(item -> "ALL".equals(riskLevel) || riskLevel.equalsIgnoreCase(item.getRiskLevel()))
                .collect(Collectors.toList()));
    }

    private GrowthRetentionCenterVO.RiskItem risk(String riskType, String title, String riskLevel, String status, String source, String suggestion) {
        return new GrowthRetentionCenterVO.RiskItem()
                .setRiskType(riskType)
                .setTitle(title)
                .setRiskLevel(riskLevel)
                .setStatus(status)
                .setSource(source)
                .setSuggestion(suggestion)
                .setDetectedAt(LocalDateTime.now());
    }

    private long activeMemberCount(GrowthRetentionCenterVO vo, Long tenantId, String loginCondition) {
        long loginUsers = distinctLoginUsers(vo, tenantId, loginCondition);
        long statusActive = countAny(vo, "growth.activeMemberByStatus", "member", "status IN (1,'ACTIVE','active','在社','正常')", tenantId);
        return Math.max(loginUsers, statusActive);
    }

    private long dormantMemberCount(GrowthRetentionCenterVO vo, Long tenantId, String loginCondition) {
        long explicitDormant = countAny(vo, "growth.dormantMemberByStatus", "member", "status IN (0,'DORMANT','dormant','沉默')", tenantId);
        long totalMembers = countAny(vo, "growth.totalMemberCount", "member", null, tenantId);
        long activeMembers = activeMemberCount(vo, tenantId, loginCondition);
        return Math.max(explicitDormant, Math.max(0L, totalMembers - activeMembers));
    }

    private long distinctLoginUsers(GrowthRetentionCenterVO vo, Long tenantId, String timeCondition) {
        TableRef table = resolveTable("login_log");
        if (table == null) {
            addSource(vo, "growth.activeMemberLogin", "sys_logininfor", "missing");
            return 0L;
        }
        String loginColumn = firstExistingColumn(table, List.of("login_name", "user_name", "username"));
        if (loginColumn == null) {
            addSource(vo, "growth.activeMemberLogin", table.displayName(), "missing-columns");
            return 0L;
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, timeCondition), tenantId);
        String sql = "SELECT COUNT(DISTINCT `" + loginColumn + "`) FROM " + table.qualifiedName() + where.sql;
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, "growth.activeMemberLogin", table.displayName(), "ok");
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException e) {
            addSource(vo, "growth.activeMemberLogin", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0L;
        }
    }

    private long activeClubCount(GrowthRetentionCenterVO vo, Long tenantId) {
        TableRef table = resolveTable("member");
        if (table == null) {
            addSource(vo, "growth.activeClubCount", "club_members", "missing");
            return 0L;
        }
        if (columnExists(table, "department")) {
            QueryParts where = buildWhere(table, "department IS NOT NULL AND department <> ''", tenantId);
            String sql = "SELECT COUNT(DISTINCT department) FROM " + table.qualifiedName() + where.sql;
            try {
                Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
                addSource(vo, "growth.activeClubCount", table.displayName(), "ok");
                return value == null ? 0L : value.longValue();
            } catch (RuntimeException e) {
                addSource(vo, "growth.activeClubCount", table.displayName(), "fallback:" + e.getClass().getSimpleName());
                return 0L;
            }
        }
        return countAny(vo, "growth.activeClubCount", "member", null, tenantId) > 0 ? 1L : 0L;
    }

    private double volunteerHours(GrowthRetentionCenterVO vo, Long tenantId) {
        return volunteerHours(vo, tenantId, null);
    }

    private double volunteerHours(GrowthRetentionCenterVO vo, Long tenantId, String timeCondition) {
        return sumAny(vo, "growth.volunteerHours", "volunteer_record", List.of("service_hours", "volunteer_hours", "hours"), approvedCondition(timeCondition), tenantId);
    }

    private double creditTotal(GrowthRetentionCenterVO vo, Long tenantId) {
        return creditTotal(vo, tenantId, null);
    }

    private double creditTotal(GrowthRetentionCenterVO vo, Long tenantId, String timeCondition) {
        double summaryTotal = sumAny(vo, "growth.creditSummaryTotal", "user_credit_summary", List.of("total_credits", "total_hours", "volunteer_hours"), timeCondition, tenantId);
        if (summaryTotal > 0D) {
            return summaryTotal;
        }
        return sumAny(vo, "growth.creditRecordTotal", "user_credit_record", List.of("credit_score", "credit_hours"), approvedCondition(timeCondition), tenantId);
    }

    private long countAny(GrowthRetentionCenterVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
        return countTable(vo, metric, table, filterConditionByExistingColumns(table, extraCondition), tenantId);
    }

    private long countTable(GrowthRetentionCenterVO vo, String metric, TableRef table, String extraCondition, Long tenantId) {
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

    private double sumAny(GrowthRetentionCenterVO vo, String metric, String baseTable, List<String> candidateColumns, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0D;
        }
        String column = firstExistingColumn(table, candidateColumns);
        if (column == null) {
            addSource(vo, metric, table.displayName(), "missing-columns");
            return 0D;
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, extraCondition), tenantId);
        String sql = "SELECT COALESCE(SUM(`" + column + "`),0) FROM " + table.qualifiedName() + where.sql;
        try {
            Number value = jdbcTemplate.queryForObject(sql, Number.class, where.args.toArray());
            addSource(vo, metric, table.displayName(), "ok");
            return value == null ? 0D : roundDouble(value.doubleValue());
        } catch (RuntimeException e) {
            addSource(vo, metric, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return 0D;
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
            log.warn("list tenant ids for growth retention failed: {}", e.getMessage());
            return fallbackTenantIds();
        }
    }

    private List<Long> fallbackTenantIds() {
        Long fallbackTenantId = resolveTenantId(null);
        return fallbackTenantId == null || fallbackTenantId <= 0 ? List.of(1L) : List.of(fallbackTenantId);
    }

    private GrowthRetentionCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, GrowthRetentionCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read growth retention center cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, GrowthRetentionCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write growth retention center cache failed: {}", e.getMessage());
        }
    }

    private String cacheKey(String tenantScope, String timeRange, String riskLevel) {
        return KEY_GROWTH_RETENTION_CENTER + tenantScope + ":" + timeRange + ":" + riskLevel;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTimeRange(String value) {
        if (value == null || value.isBlank()) {
            return "90D";
        }
        String normalized = normalizeFilter(value);
        return List.of("30D", "90D", "180D", "ALL").contains(normalized) ? normalized : "90D";
    }

    private int daysForRange(String timeRange) {
        if ("30D".equals(timeRange)) {
            return 30;
        }
        if ("180D".equals(timeRange)) {
            return 180;
        }
        if ("ALL".equals(timeRange)) {
            return 3650;
        }
        return 90;
    }

    private String approvedCondition(String timeCondition) {
        String approved = "status IN (1,'APPROVED','approved','PASS','pass')";
        if (timeCondition == null || timeCondition.isBlank()) {
            return approved;
        }
        return "(" + approved + ") AND (" + timeCondition + ")";
    }

    private String sinceCondition(String columnName, int days) {
        return columnName + " >= '" + LocalDate.now().minusDays(days) + " 00:00:00'";
    }

    private String monthRangeCondition(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        return "create_time >= '" + start + " 00:00:00' AND create_time < '" + end + " 00:00:00'";
    }

    private String loginRangeCondition(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        return "login_time >= '" + start + " 00:00:00' AND login_time < '" + end + " 00:00:00'";
    }

    private double roundRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round(numerator * 10000D / denominator) / 100D;
    }

    private double roundDouble(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private TableRef resolveTable(String baseTable) {
        if (baseTable == null) {
            return null;
        }
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
                Map.entry("volunteer_record", List.of("rk_user.volunteer_record", "volunteer_record")),
                Map.entry("user_credit_record", List.of("rk_user.user_credit_record", "user_credit_record")),
                Map.entry("user_credit_summary", List.of("rk_user.user_credit_summary", "user_credit_summary"))
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
        if (table == null || condition == null || condition.isBlank()) {
            return null;
        }
        String lower = condition.toLowerCase(Locale.ROOT);
        for (String column : List.of("is_deleted", "status", "create_time", "created_at", "created_time", "join_date", "login_time", "service_date", "last_updated", "department")) {
            if (lower.contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return condition.trim();
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

    private void addSource(GrowthRetentionCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new GrowthRetentionCenterVO.SourceInfo()
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
