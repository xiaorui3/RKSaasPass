package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.TenantOperationsCenterVO;
import com.tianji.data.service.TenantOperationsCenterService;
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

import static com.tianji.data.constants.RedisConstants.KEY_TENANT_OPERATIONS_CENTER;

@Slf4j
@Service
public class TenantOperationsCenterServiceImpl implements TenantOperationsCenterService {
    private static final int CACHE_TTL_SECONDS = 180;
    private static final int CACHE_TTL_JITTER_SECONDS = 60;
    private static final List<String> TIME_COLUMNS = Arrays.asList("create_time", "created_at", "created_time");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public TenantOperationsCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public TenantOperationsCenterVO getOverview(Long requestedTenantId) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String cacheKey = KEY_TENANT_OPERATIONS_CENTER + tenantScope;

        TenantOperationsCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        TenantOperationsCenterVO computed = compute(scopedTenantId, tenantScope);
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
    public List<TenantOperationsCenterVO> warmupAllTenants() {
        List<TenantOperationsCenterVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            String tenantScope = String.valueOf(tenantId);
            TenantOperationsCenterVO computed = compute(tenantId, tenantScope);
            computed.getCache()
                    .setHit(false)
                    .setMode("warmup")
                    .setKey(KEY_TENANT_OPERATIONS_CENTER + tenantScope)
                    .setTtlSeconds(CACHE_TTL_SECONDS)
                    .setCachedAt(LocalDateTime.now());
            writeCache(computed.getCache().getKey(), computed);
            warmed.add(computed);
        }
        return warmed;
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

    private TenantOperationsCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, TenantOperationsCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read tenant operations center cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, TenantOperationsCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write tenant operations center cache failed: {}", e.getMessage());
        }
    }

    private TenantOperationsCenterVO compute(Long tenantId, String tenantScope) {
        LocalDateTime now = LocalDateTime.now();
        TenantOperationsCenterVO vo = new TenantOperationsCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setTenantName(resolveTenantName(tenantId))
                .setUpdatedAt(now);

        fillApprovalCenter(vo, tenantId);
        fillParticipation(vo, tenantId);
        fillDataQuality(vo, tenantId);
        fillGrowthRetention(vo, tenantId);
        fillSecurityAudit(vo, tenantId);
        fillHealthScore(vo);
        return vo;
    }

    private void fillApprovalCenter(TenantOperationsCenterVO vo, Long tenantId) {
        TenantOperationsCenterVO.ApprovalCenter approvals = vo.getApprovalCenter();
        approvals
                .setJoinPendingCount(countAny(vo, "joinPendingCount", "join_request", "status IN (0,1,'PENDING','pending')", tenantId))
                .setActivityPendingCount(countAny(vo, "activityPendingCount", "activity", "approval_status IN (0,1,'PENDING','pending')", tenantId))
                .setCompetitionPendingCount(countAny(vo, "competitionPendingCount", "competition", "approval_status IN (0,1,'PENDING','pending')", tenantId))
                .setContentPendingCount(countAny(vo, "contentPendingCount", "news", "approval_status IN (0,1,'PENDING','pending')", tenantId))
                .setFinancePendingCount(countAny(vo, "financePendingCount", "finance_reimbursement", "status IN (0,'PENDING','pending')", tenantId));
        long total = approvals.getJoinPendingCount()
                + approvals.getActivityPendingCount()
                + approvals.getCompetitionPendingCount()
                + approvals.getContentPendingCount()
                + approvals.getFinancePendingCount();
        approvals.setTotalPendingCount(total);
        approvals.setOverdueCount(
                countAny(vo, "overdueJoinPendingCount", "join_request", olderThanDaysCondition("create_time", 3), tenantId)
                        + countAny(vo, "overdueFinancePendingCount", "finance_reimbursement", olderThanDaysCondition("create_time", 3), tenantId));
        approvals.setSlaRiskRate(total == 0 ? 0D : roundRate(approvals.getOverdueCount(), total));
        vo.setApprovalDetails(buildApprovalDetails(vo, tenantId, approvals));
    }

    private void fillParticipation(TenantOperationsCenterVO vo, Long tenantId) {
        TenantOperationsCenterVO.Participation participation = vo.getParticipation();
        long activities = countAny(vo, "activityCount", "activity", null, tenantId);
        long competitions = countAny(vo, "competitionCount", "competition", null, tenantId);
        participation
                .setActivityRegistrationCount(countAny(vo, "activityRegistrationCount", "activity_registration", null, tenantId))
                .setCompetitionRegistrationCount(countAny(vo, "competitionRegistrationCount", "competition_participant", null, tenantId))
                .setCommentCount(countAny(vo, "commentCount", "comments", null, tenantId))
                .setNewsCount(countAny(vo, "newsCount", "news", null, tenantId))
                .setNoticeCount(countAny(vo, "noticeCount", "notice", null, tenantId))
                .setActiveMemberCount(countAny(vo, "activeMemberCount", "member", "status IN (1,'ACTIVE','active')", tenantId));
        long totalEvents = activities + competitions;
        long totalRegistrations = participation.getActivityRegistrationCount() + participation.getCompetitionRegistrationCount();
        participation.setRegistrationConversionRate(totalEvents == 0 ? 0D : roundRate(totalRegistrations, totalEvents));
    }

    private void fillDataQuality(TenantOperationsCenterVO vo, Long tenantId) {
        TenantOperationsCenterVO.DataQuality quality = vo.getDataQuality();
        quality
                .setOrphanUserCount(countAny(vo, "orphanUserCount", "user", "tenant_id IS NULL", null))
                .setMissingRoleCount(countAny(vo, "missingRoleCount", "account_role", null, tenantId))
                .setMissingMediaCount(countAny(vo, "missingMediaCount", "news", "cover_url IS NULL", tenantId))
                .setSearchStaleCount(countAny(vo, "searchStaleCount", "search_document", olderThanHoursCondition("updated_at", 24), tenantId))
                .setRedisRiskCount(0L);
        quality.setIssueCount(quality.getOrphanUserCount()
                + quality.getMissingRoleCount()
                + quality.getMissingMediaCount()
                + quality.getSearchStaleCount()
                + quality.getRedisRiskCount());
        vo.setQualityDetails(buildQualityDetails(vo, tenantId, quality));
    }

    private void fillGrowthRetention(TenantOperationsCenterVO vo, Long tenantId) {
        TenantOperationsCenterVO.GrowthRetention growth = vo.getGrowthRetention();
        growth
                .setNewMemberCount(countAny(vo, "newMemberCount", "member", sinceDaysCondition("create_time", 30), tenantId))
                .setActiveMemberCount(countAny(vo, "growthActiveMemberCount", "member", "status IN (1,'ACTIVE','active')", tenantId))
                .setDormantMemberCount(countAny(vo, "dormantMemberCount", "member", "status IN (0,'DORMANT','dormant')", tenantId))
                .setVolunteerHours(sumAny(vo, "volunteerHours", "volunteer_record", "service_hours", tenantId))
                .setCreditTotal(sumAny(vo, "creditTotal", "user_credit_record", "amount", tenantId));
    }

    private void fillSecurityAudit(TenantOperationsCenterVO vo, Long tenantId) {
        TenantOperationsCenterVO.SecurityAudit security = vo.getSecurityAudit();
        security
                .setHighRiskOperationCount(countAny(vo, "highRiskOperationCount", "operation_log", "business_type IN (3,4,5,8,9)", tenantId))
                .setCrossTenantAttemptCount(countAny(vo, "crossTenantAttemptCount", "request_audit", "risk_level IN ('HIGH','high')", tenantId))
                .setAbnormalLoginCount(countAny(vo, "abnormalLoginCount", "login_log", "status IN (1,'FAIL','fail')", tenantId))
                .setPermissionChangeCount(countAny(vo, "permissionChangeCount", "operation_log", "title LIKE '%role%' OR title LIKE '%menu%'", tenantId));
        security.setTotalRiskCount(security.getHighRiskOperationCount()
                + security.getCrossTenantAttemptCount()
                + security.getAbnormalLoginCount()
                + security.getPermissionChangeCount());
        vo.setSecurityDetails(buildSecurityDetails(vo, tenantId, security));
    }

    private void fillHealthScore(TenantOperationsCenterVO vo) {
        long pending = vo.getApprovalCenter().getTotalPendingCount();
        long quality = vo.getDataQuality().getIssueCount();
        long security = vo.getSecurityAudit().getTotalRiskCount();
        long cache = vo.getDataQuality().getRedisRiskCount();
        long search = vo.getDataQuality().getSearchStaleCount();
        int score = 100
                - (int) Math.min(30, pending * 2)
                - (int) Math.min(25, quality * 3)
                - (int) Math.min(25, security * 4)
                - (int) Math.min(10, cache * 2)
                - (int) Math.min(10, search * 2);
        score = Math.max(0, Math.min(100, score));
        String level = score >= 85 ? "HEALTHY" : score >= 65 ? "WATCH" : "RISK";
        vo.getHealthScore()
                .setScore(score)
                .setLevel(level)
                .setPendingPressure(pending)
                .setQualityIssues(quality)
                .setSecurityRisks(security)
                .setCacheRisks(cache)
                .setSearchRisks(search);
        addSource(vo, "healthScore", "rk_data.aggregate", "ok");
    }

    private String resolveTenantName(Long tenantId) {
        return tenantId == null ? "All tenants" : "Tenant " + tenantId;
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
            log.warn("list tenant ids for operations center failed: {}", e.getMessage());
            return fallbackTenantIds();
        }
    }

    private List<Long> fallbackTenantIds() {
        Long fallbackTenantId = resolveTenantId(null);
        return fallbackTenantId == null || fallbackTenantId <= 0 ? List.of(1L) : List.of(fallbackTenantId);
    }

    private List<TenantOperationsCenterVO.DetailItem> buildApprovalDetails(
            TenantOperationsCenterVO vo,
            Long tenantId,
            TenantOperationsCenterVO.ApprovalCenter approvals) {
        List<TenantOperationsCenterVO.DetailItem> details = new ArrayList<>();
        details.addAll(buildDetailItems(vo, "approval", "入社审核", "join_request", "status IN (0,1,'PENDING','pending')", tenantId));
        details.addAll(buildDetailItems(vo, "approval", "活动审核", "activity", "approval_status IN (0,1,'PENDING','pending')", tenantId));
        details.addAll(buildDetailItems(vo, "approval", "比赛审核", "competition", "approval_status IN (0,1,'PENDING','pending')", tenantId));
        details.addAll(buildDetailItems(vo, "approval", "内容审核", "news", "approval_status IN (0,1,'PENDING','pending')", tenantId));
        details.addAll(buildDetailItems(vo, "approval", "财务审核", "finance_reimbursement", "status IN (0,'PENDING','pending')", tenantId));
        if (details.isEmpty() && approvals.getTotalPendingCount() >= 0) {
            details.add(summaryDetail("approval", "当前没有可下钻的审批明细", "LOW", "NORMAL", "rk_data.aggregate"));
        }
        return limitDetails(details);
    }

    private List<TenantOperationsCenterVO.DetailItem> buildQualityDetails(
            TenantOperationsCenterVO vo,
            Long tenantId,
            TenantOperationsCenterVO.DataQuality quality) {
        List<TenantOperationsCenterVO.DetailItem> details = new ArrayList<>();
        details.addAll(buildDetailItems(vo, "quality", "缺少封面媒体", "news", "cover_url IS NULL", tenantId));
        details.addAll(buildDetailItems(vo, "quality", "ES 索引滞后", "search_document", olderThanHoursCondition("updated_at", 24), tenantId));
        if (quality.getOrphanUserCount() > 0) {
            details.add(summaryDetail("quality", "存在未绑定租户的用户：" + quality.getOrphanUserCount() + " 个", "HIGH", "NEED_FIX", "rk_user.rk_user"));
        }
        if (quality.getMissingRoleCount() > 0) {
            details.add(summaryDetail("quality", "存在缺失角色绑定：" + quality.getMissingRoleCount() + " 条", "MEDIUM", "NEED_FIX", "rk_auth.account_role"));
        }
        if (details.isEmpty()) {
            details.add(summaryDetail("quality", "暂未发现高优先级数据质量问题", "LOW", "NORMAL", "rk_data.aggregate"));
        }
        return limitDetails(details);
    }

    private List<TenantOperationsCenterVO.DetailItem> buildSecurityDetails(
            TenantOperationsCenterVO vo,
            Long tenantId,
            TenantOperationsCenterVO.SecurityAudit security) {
        List<TenantOperationsCenterVO.DetailItem> details = new ArrayList<>();
        details.addAll(buildDetailItems(vo, "security", "高危后台操作", "operation_log", "business_type IN (3,4,5,8,9)", tenantId));
        details.addAll(buildDetailItems(vo, "security", "跨租户访问尝试", "request_audit", "risk_level IN ('HIGH','high')", tenantId));
        details.addAll(buildDetailItems(vo, "security", "异常登录", "login_log", "status IN (1,'FAIL','fail')", tenantId));
        details.addAll(buildDetailItems(vo, "security", "权限变更", "operation_log", "title LIKE '%role%' OR title LIKE '%menu%'", tenantId));
        if (details.isEmpty() && security.getTotalRiskCount() >= 0) {
            details.add(summaryDetail("security", "当前没有可下钻的安全风险明细", "LOW", "NORMAL", "rk_data.aggregate"));
        }
        return limitDetails(details);
    }

    private List<TenantOperationsCenterVO.DetailItem> buildDetailItems(
            TenantOperationsCenterVO vo,
            String type,
            String label,
            String baseTable,
            String extraCondition,
            Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, type + "." + baseTable + ".details", baseTable, "missing");
            return List.of(summaryDetail(type, label + "来源表缺失", "LOW", "MISSING_SOURCE", baseTable));
        }
        QueryParts where = buildWhere(table, extraCondition, tenantId);
        List<String> columns = existingColumns(table, List.of("id", "title", "activity_name", "name", "status", "approval_status",
                "risk_level", "business_type", "create_time", "created_at", "created_time", "update_time", "updated_at", "login_time"));
        if (columns.isEmpty()) {
            addSource(vo, type + "." + baseTable + ".details", table.displayName(), "missing-columns");
            return List.of(summaryDetail(type, label + "缺少可展示字段", "LOW", "MISSING_COLUMNS", table.displayName()));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName()
                + where.sql
                + orderByExistingTime(table)
                + " LIMIT 8";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, type + "." + baseTable + ".details", table.displayName(), "ok");
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            return rows.stream()
                    .map(row -> toDetailItem(type, label, table.displayName(), row))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, type + "." + baseTable + ".details", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryDetail(type, label + "明细暂不可用", "MEDIUM", "FALLBACK", table.displayName()));
        }
    }

    private TenantOperationsCenterVO.DetailItem toDetailItem(String type, String label, String source, Map<String, Object> row) {
        String title = firstText(row, "title", "activity_name", "name");
        String status = firstText(row, "status", "approval_status", "risk_level", "business_type");
        return new TenantOperationsCenterVO.DetailItem()
                .setType(type)
                .setTitle((title == null || title.isBlank()) ? label : label + "：" + title)
                .setRisk(riskFor(type, status))
                .setStatus(status == null || status.isBlank() ? "PENDING" : status)
                .setSource(source)
                .setSourceId(firstText(row, "id"))
                .setSourceTime(firstText(row, "update_time", "updated_at", "create_time", "created_at", "created_time", "login_time"));
    }

    private TenantOperationsCenterVO.DetailItem summaryDetail(String type, String title, String risk, String status, String source) {
        return new TenantOperationsCenterVO.DetailItem()
                .setType(type)
                .setTitle(title)
                .setRisk(risk)
                .setStatus(status)
                .setSource(source)
                .setSourceTime(LocalDateTime.now().toString());
    }

    private List<TenantOperationsCenterVO.DetailItem> limitDetails(List<TenantOperationsCenterVO.DetailItem> details) {
        return details.stream().limit(20).collect(Collectors.toList());
    }

    private String firstText(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private String riskFor(String type, String status) {
        String normalized = status == null ? "" : status.toUpperCase(Locale.ROOT);
        if ("security".equals(type)) {
            return "HIGH";
        }
        if (normalized.contains("HIGH") || normalized.contains("FAIL")) {
            return "HIGH";
        }
        if ("quality".equals(type)) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private long countAny(TenantOperationsCenterVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
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

    private double sumAny(TenantOperationsCenterVO vo, String metric, String baseTable, String column, Long tenantId) {
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
            if (tableExists(table)) {
                return table;
            }
        }
        return null;
    }

    private List<String> tableCandidates(String baseTable) {
        Map<String, List<String>> mapped = Map.ofEntries(
                Map.entry("role", List.of("rk_auth.role", "role")),
                Map.entry("account_role", List.of("rk_auth.account_role", "account_role")),
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant")),
                Map.entry("user", List.of("rk_user.rk_user", "rk_user")),
                Map.entry("member", List.of("rk_user.club_members", "club_members")),
                Map.entry("join_request", List.of("rk_user.join_requests", "join_requests")),
                Map.entry("operation_log", List.of("rk_user.sys_oper_log", "sys_oper_log")),
                Map.entry("login_log", List.of("rk_user.sys_logininfor", "sys_logininfor")),
                Map.entry("request_audit", List.of("rk_user.request_audit", "request_audit")),
                Map.entry("volunteer_record", List.of("rk_user.volunteer_record", "volunteer_record")),
                Map.entry("user_credit_record", List.of("rk_user.user_credit_record", "user_credit_record")),
                Map.entry("finance_reimbursement", List.of("rk_user.finance_reimbursement", "finance_reimbursement")),
                Map.entry("activity", List.of("rk_activity.rk_activity", "rk_activity")),
                Map.entry("competition", List.of("rk_activity.competition_competitions", "competition_competitions")),
                Map.entry("activity_registration", List.of("rk_activity.rk_activity_registration", "rk_activity_registration")),
                Map.entry("competition_participant", List.of("rk_activity.competition_participants", "competition_participants")),
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("comments", List.of("rk_content.comments", "comments")),
                Map.entry("notice", List.of("rk_message.rk_notice", "rk_notice")),
                Map.entry("search_document", List.of("rk_search.rk_search_index", "rk_search_index"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        candidates.add("rk_" + baseTable);
        candidates.add("sys_" + baseTable);
        return candidates;
    }

    private String optionalWhere(TableRef table, String condition) {
        String safeCondition = filterConditionByExistingColumns(table, condition);
        return safeCondition == null || safeCondition.isEmpty() ? "" : " WHERE " + safeCondition;
    }

    private List<String> existingColumns(TableRef table, List<String> columns) {
        return columns.stream()
                .filter(column -> columnExists(table, column))
                .collect(Collectors.toList());
    }

    private String selectColumns(List<String> columns) {
        return columns.stream()
                .map(column -> "`" + column + "`")
                .collect(Collectors.joining(", "));
    }

    private String orderByExistingTime(TableRef table) {
        List<String> existing = TIME_COLUMNS.stream()
                .filter(column -> columnExists(table, column))
                .collect(Collectors.toList());
        if (columnExists(table, "update_time")) {
            existing.add(0, "update_time");
        }
        if (columnExists(table, "updated_at")) {
            existing.add(0, "updated_at");
        }
        if (columnExists(table, "login_time")) {
            existing.add(0, "login_time");
        }
        if (existing.isEmpty()) {
            return "";
        }
        return " ORDER BY " + existing.stream()
                .distinct()
                .map(column -> "`" + column + "` DESC")
                .collect(Collectors.joining(", "));
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
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        String filtered = condition;
        for (String column : List.of("is_deleted", "status", "approval_status", "cover_url", "updated_at", "create_time", "title", "business_type", "risk_level")) {
            if (filtered.toLowerCase(Locale.ROOT).contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return filtered.trim();
    }

    private String olderThanDaysCondition(String columnName, int days) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        return columnName + " < '" + threshold + "'";
    }

    private String olderThanHoursCondition(String columnName, int hours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);
        return columnName + " < '" + threshold + "'";
    }

    private String sinceDaysCondition(String columnName, int days) {
        LocalDate today = LocalDate.now();
        return columnName + " >= '" + today.minusDays(days) + " 00:00:00'";
    }

    private double roundRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000D / denominator)) / 100D;
    }

    private void addSource(TenantOperationsCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new TenantOperationsCenterVO.SourceInfo()
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
