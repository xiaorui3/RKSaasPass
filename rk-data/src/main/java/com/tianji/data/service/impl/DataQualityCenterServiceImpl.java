package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.dto.DataQualityRepairRequestDTO;
import com.tianji.data.model.vo.DataQualityCenterVO;
import com.tianji.data.model.vo.FrontendCacheWarmupSummaryVO;
import com.tianji.data.service.DataQualityCenterService;
import com.tianji.data.service.FrontendCacheWarmupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tianji.data.constants.RedisConstants.KEY_DATA_QUALITY_CENTER;

@Slf4j
@Service
public class DataQualityCenterServiceImpl implements DataQualityCenterService {
    private static final int CACHE_TTL_SECONDS = 180;
    private static final int CACHE_TTL_JITTER_SECONDS = 60;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final FrontendCacheWarmupService frontendCacheWarmupService;

    @Autowired
    public DataQualityCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate, FrontendCacheWarmupService frontendCacheWarmupService) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.frontendCacheWarmupService = frontendCacheWarmupService;
    }

    @Override
    public DataQualityCenterVO getDataQualityCenter(Long requestedTenantId, String issueType, String riskLevel) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedIssueType = normalizeFilter(issueType);
        String normalizedRiskLevel = normalizeFilter(riskLevel);
        String cacheKey = KEY_DATA_QUALITY_CENTER + tenantScope + ":" + normalizedIssueType + ":" + normalizedRiskLevel;

        DataQualityCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        DataQualityCenterVO computed = compute(scopedTenantId, tenantScope, normalizedIssueType, normalizedRiskLevel);
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
    public DataQualityCenterVO.RepairLog repair(DataQualityRepairRequestDTO request) {
        DataQualityRepairRequestDTO safeRequest = request == null ? new DataQualityRepairRequestDTO() : request;
        Long tenantId = resolveTenantId(safeRequest.getTenantId());
        String issueType = normalizeIssueType(safeRequest.getIssueType());
        String reason = safeRequest.getReason() == null || safeRequest.getReason().isBlank() ? "manual repair" : safeRequest.getReason().trim();
        DataQualityCenterVO.RepairLog logEntry = new DataQualityCenterVO.RepairLog()
                .setTenantId(tenantId)
                .setIssueType(issueType)
                .setAction(resolveRepairAction(issueType))
                .setReason(reason)
                .setCreatedAt(LocalDateTime.now());

        try {
            if ("redis".equals(issueType)) {
                FrontendCacheWarmupSummaryVO summary = frontendCacheWarmupService.manualWarmup();
                logEntry
                        .setStatus(summary.isRedisAvailable() ? "SUCCESS" : "FAILED")
                        .setMessage("Redis 缓存预热完成，warmed=" + summary.isWarmed() + ", failureCount=" + summary.getFailureCount());
            } else if ("search".equals(issueType)) {
                logEntry
                        .setStatus("QUEUED")
                        .setMessage("ES 索引滞后已登记，请通过搜索服务重建索引后复检");
            } else {
                logEntry
                        .setStatus("AUDIT_ONLY")
                        .setMessage("该类型涉及主数据，已登记审计记录，需人工复核后处理");
            }
        } catch (RuntimeException e) {
            logEntry
                    .setStatus("FAILED")
                    .setMessage(e.getMessage());
        }
        audit(logEntry);
        return logEntry;
    }

    private DataQualityCenterVO compute(Long tenantId, String tenantScope, String issueType, String riskLevel) {
        DataQualityCenterVO vo = new DataQualityCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setIssueType(issueType)
                .setRiskLevel(riskLevel);

        List<DataQualityCenterVO.IssueItem> issues = new ArrayList<>();
        issues.addAll(orphanUserIssues(vo));
        issues.addAll(missingRoleIssues(vo, tenantId));
        issues.addAll(missingMediaIssues(vo, tenantId));
        issues.addAll(searchStaleIssues(vo, tenantId));
        issues.addAll(redisRiskIssues(vo));
        issues.addAll(memberDriftIssues(vo, tenantId));
        if (issues.isEmpty()) {
            issues.add(summaryIssue("normal", "质量检查通过", "当前未发现高优先级数据质量问题", "LOW", "NORMAL", "rk_data.aggregate", false));
        }
        vo.setIssues(issues.stream()
                .filter(issue -> filterIssue(issue, issueType, riskLevel))
                .limit(200)
                .collect(Collectors.toList()));
        vo.setRepairLogs(recentRepairLogs(tenantId));
        fillSummary(vo);
        return vo;
    }

    private List<DataQualityCenterVO.IssueItem> orphanUserIssues(DataQualityCenterVO vo) {
        long count = countAny(vo, "quality.orphan_user", "user", "tenant_id IS NULL", null);
        if (count <= 0) {
            return List.of();
        }
        return List.of(summaryIssue("orphan_user", "账号漂移", "存在未绑定租户的用户：" + count + " 个", "HIGH", "NEED_REVIEW", "rk_user.rk_user", false));
    }

    private List<DataQualityCenterVO.IssueItem> missingRoleIssues(DataQualityCenterVO vo, Long tenantId) {
        long count = countAny(vo, "quality.missing_role", "account_role", null, tenantId);
        if (count <= 0) {
            return List.of();
        }
        return List.of(summaryIssue("missing_role", "角色缺失", "存在角色绑定异常或缺失：" + count + " 条", "MEDIUM", "NEED_REVIEW", "rk_auth.account_role", false));
    }

    private List<DataQualityCenterVO.IssueItem> missingMediaIssues(DataQualityCenterVO vo, Long tenantId) {
        return buildIssueItems(vo, "missing_media", "媒体缺失", "news", "cover_url IS NULL", tenantId, "补充封面或清理孤儿媒体引用", false);
    }

    private List<DataQualityCenterVO.IssueItem> searchStaleIssues(DataQualityCenterVO vo, Long tenantId) {
        return buildIssueItems(vo, "search", "ES 滞后", "search_document", olderThanHoursCondition("updated_at", 24), tenantId, "触发搜索索引重建后复检", true);
    }

    private List<DataQualityCenterVO.IssueItem> redisRiskIssues(DataQualityCenterVO vo) {
        try {
            FrontendCacheWarmupSummaryVO summary = frontendCacheWarmupService.latestSummary();
            if (summary == null || !summary.isRedisAvailable() || summary.getFailureCount() > 0 || summary.getMissCount() > 0) {
                long riskCount = summary == null ? 1 : Math.max(1, summary.getFailureCount() + summary.getMissCount());
                return List.of(summaryIssue("redis", "Redis 风险", "前台缓存存在失效或预热失败：" + riskCount + " 项", "MEDIUM", "REPAIRABLE", "redis.frontend-cache", true));
            }
            addSource(vo, "quality.redis", "redis.frontend-cache", "ok");
            return List.of();
        } catch (RuntimeException e) {
            addSource(vo, "quality.redis", "redis.frontend-cache", "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryIssue("redis", "Redis 风险", "Redis 状态读取失败：" + e.getMessage(), "MEDIUM", "REPAIRABLE", "redis.frontend-cache", true));
        }
    }

    private List<DataQualityCenterVO.IssueItem> memberDriftIssues(DataQualityCenterVO vo, Long tenantId) {
        long count = countAny(vo, "quality.member_drift", "member", "user_id IS NULL", tenantId);
        if (count <= 0) {
            return List.of();
        }
        return List.of(summaryIssue("member_drift", "成员漂移", "存在未关联账号的成员：" + count + " 个", "MEDIUM", "NEED_REVIEW", "rk_user.club_members", false));
    }

    private List<DataQualityCenterVO.IssueItem> buildIssueItems(
            DataQualityCenterVO vo,
            String issueType,
            String label,
            String baseTable,
            String extraCondition,
            Long tenantId,
            String suggestion,
            boolean repairable) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, "quality." + issueType, baseTable, "missing");
            return List.of(summaryIssue(issueType, label, label + "来源表缺失", "LOW", "SOURCE_MISSING", baseTable, false));
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, extraCondition), tenantId);
        List<String> columns = existingColumns(table, List.of("id", "title", "name", "cover_url", "updated_at", "update_time", "create_time", "created_at"));
        if (columns.isEmpty()) {
            addSource(vo, "quality." + issueType, table.displayName(), "missing-columns");
            return List.of(summaryIssue(issueType, label, label + "缺少可展示字段", "LOW", "MISSING_COLUMNS", table.displayName(), false));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName()
                + where.sql
                + orderByExistingTime(table)
                + " LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "quality." + issueType, table.displayName(), "ok");
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            return rows.stream()
                    .map(row -> new DataQualityCenterVO.IssueItem()
                            .setIssueType(issueType)
                            .setIssueTypeLabel(label)
                            .setTitle(firstText(row, "title", "name", "id"))
                            .setDescription(label + "：" + firstText(row, "title", "name", "id"))
                            .setRiskLevel("search".equals(issueType) ? "MEDIUM" : "LOW")
                            .setStatus(repairable ? "REPAIRABLE" : "NEED_REVIEW")
                            .setSource(table.displayName())
                            .setSourceId(firstText(row, "id"))
                            .setSuggestion(suggestion)
                            .setRepairable(repairable)
                            .setRepairAction(resolveRepairAction(issueType))
                            .setDetectedAt(formatNow()))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "quality." + issueType, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryIssue(issueType, label, label + "明细暂不可用", "MEDIUM", "FALLBACK", table.displayName(), false));
        }
    }

    private void fillSummary(DataQualityCenterVO vo) {
        DataQualityCenterVO.Summary summary = vo.getSummary();
        summary.setTotalIssueCount(vo.getIssues().stream().filter(issue -> !"normal".equals(issue.getIssueType())).count());
        summary.setHighRiskCount(vo.getIssues().stream().filter(issue -> "HIGH".equals(issue.getRiskLevel())).count());
        summary.setRepairableCount(vo.getIssues().stream().filter(DataQualityCenterVO.IssueItem::isRepairable).count());
        summary.setOrphanUserCount(countIssues(vo, "orphan_user"));
        summary.setMissingRoleCount(countIssues(vo, "missing_role"));
        summary.setMissingMediaCount(countIssues(vo, "missing_media"));
        summary.setSearchStaleCount(countIssues(vo, "search"));
        summary.setRedisRiskCount(countIssues(vo, "redis"));
        summary.setDriftCount(countIssues(vo, "member_drift"));
    }

    private long countIssues(DataQualityCenterVO vo, String issueType) {
        return vo.getIssues().stream().filter(issue -> issueType.equals(issue.getIssueType())).count();
    }

    private boolean filterIssue(DataQualityCenterVO.IssueItem issue, String issueType, String riskLevel) {
        if (!"ALL".equals(issueType) && !issueType.equalsIgnoreCase(issue.getIssueType())) {
            return false;
        }
        return "ALL".equals(riskLevel) || riskLevel.equalsIgnoreCase(issue.getRiskLevel());
    }

    private DataQualityCenterVO.IssueItem summaryIssue(String issueType, String label, String description, String riskLevel, String status, String source, boolean repairable) {
        return new DataQualityCenterVO.IssueItem()
                .setIssueType(issueType)
                .setIssueTypeLabel(label)
                .setTitle(label)
                .setDescription(description)
                .setRiskLevel(riskLevel)
                .setStatus(status)
                .setSource(source)
                .setSuggestion(repairable ? "可在本页登记修复并保留审计记录" : "需要人工复核后处理")
                .setRepairable(repairable)
                .setRepairAction(resolveRepairAction(issueType))
                .setDetectedAt(formatNow());
    }

    private List<DataQualityCenterVO.RepairLog> recentRepairLogs(Long tenantId) {
        ensureRepairLogTable();
        TableRef table = TableRef.parse("data_quality_repair_log");
        QueryParts where = buildWhere(table, null, tenantId);
        String sql = "SELECT tenant_id, issue_type, action, status, reason, message, create_time FROM data_quality_repair_log"
                + where.sql
                + " ORDER BY id DESC LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            return rows.stream().map(row -> new DataQualityCenterVO.RepairLog()
                    .setTenantId(longValue(row.get("tenant_id")))
                    .setIssueType(firstText(row, "issue_type"))
                    .setAction(firstText(row, "action"))
                    .setStatus(firstText(row, "status"))
                    .setReason(firstText(row, "reason"))
                    .setMessage(firstText(row, "message"))
                    .setCreatedAt(LocalDateTime.now()))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private void audit(DataQualityCenterVO.RepairLog logEntry) {
        try {
            ensureRepairLogTable();
            jdbcTemplate.update(
                    "INSERT INTO data_quality_repair_log (tenant_id, issue_type, action, status, reason, message, create_time) VALUES (?,?,?,?,?,?,NOW())",
                    logEntry.getTenantId(),
                    logEntry.getIssueType(),
                    logEntry.getAction(),
                    logEntry.getStatus(),
                    logEntry.getReason(),
                    logEntry.getMessage()
            );
        } catch (RuntimeException e) {
            log.warn("write data quality repair audit failed: {}", e.getMessage());
        }
    }

    private void ensureRepairLogTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS data_quality_repair_log (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "tenant_id BIGINT DEFAULT NULL," +
                    "issue_type VARCHAR(64) NOT NULL," +
                    "action VARCHAR(64) NOT NULL," +
                    "status VARCHAR(32) NOT NULL," +
                    "reason VARCHAR(500) DEFAULT NULL," +
                    "message VARCHAR(1000) DEFAULT NULL," +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "KEY idx_data_quality_repair_tenant (tenant_id)," +
                    "KEY idx_data_quality_repair_type (issue_type)" +
                    ")");
        } catch (RuntimeException e) {
            log.warn("ensure data_quality_repair_log failed: {}", e.getMessage());
        }
    }

    private String resolveRepairAction(String issueType) {
        if ("redis".equalsIgnoreCase(issueType)) {
            return "REDIS_WARMUP";
        }
        if ("search".equalsIgnoreCase(issueType)) {
            return "SEARCH_REBUILD_REQUEST";
        }
        return "AUDIT_REVIEW";
    }

    private String normalizeIssueType(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
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

    private DataQualityCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, DataQualityCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read data quality center cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, DataQualityCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write data quality center cache failed: {}", e.getMessage());
        }
    }

    private long countAny(DataQualityCenterVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
        TableRef table = resolveTable(baseTable);
        if (table == null) {
            addSource(vo, metric, baseTable, "missing");
            return 0L;
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, extraCondition), tenantId);
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
                Map.entry("user", List.of("rk_user.rk_user", "rk_user")),
                Map.entry("member", List.of("rk_user.club_members", "club_members")),
                Map.entry("account_role", List.of("rk_auth.account_role", "account_role")),
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("search_document", List.of("rk_search.rk_search_index", "rk_search_index"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        return candidates;
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
        if (extraCondition != null && !extraCondition.isEmpty()) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("(").append(extraCondition).append(")");
        }
        return new QueryParts(where.toString(), args);
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
        List<String> columns = List.of("update_time", "updated_at", "create_time", "created_at").stream()
                .filter(column -> columnExists(table, column))
                .collect(Collectors.toList());
        if (columns.isEmpty()) {
            return "";
        }
        return " ORDER BY " + columns.stream().map(column -> "`" + column + "` DESC").collect(Collectors.joining(", "));
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        for (String column : List.of("tenant_id", "user_id", "cover_url", "updated_at")) {
            if (condition.toLowerCase(Locale.ROOT).contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return condition.trim();
    }

    private String olderThanHoursCondition(String columnName, int hours) {
        return columnName + " < '" + LocalDateTime.now().minusHours(hours) + "'";
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    private Long longValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatNow() {
        return DATE_TIME_FORMATTER.format(LocalDateTime.now());
    }

    private void addSource(DataQualityCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new DataQualityCenterVO.SourceInfo()
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
