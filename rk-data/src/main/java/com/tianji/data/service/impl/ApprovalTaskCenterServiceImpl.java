package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ApprovalTaskCenterVO;
import com.tianji.data.service.ApprovalTaskCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tianji.data.constants.RedisConstants.KEY_APPROVAL_TASK_CENTER;

@Slf4j
@Service
public class ApprovalTaskCenterServiceImpl implements ApprovalTaskCenterService {
    private static final int CACHE_TTL_SECONDS = 120;
    private static final int CACHE_TTL_JITTER_SECONDS = 60;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> TIME_COLUMNS = Arrays.asList("create_time", "created_at", "created_time");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public ApprovalTaskCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ApprovalTaskCenterVO getApprovalTaskCenter(Long requestedTenantId, String taskType, String slaStatus, String status) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedTaskType = normalizeFilter(taskType);
        String normalizedSlaStatus = normalizeFilter(slaStatus);
        String normalizedStatus = normalizeFilter(status);
        String cacheKey = cacheKey(tenantScope, normalizedTaskType, normalizedSlaStatus, normalizedStatus);

        ApprovalTaskCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        ApprovalTaskCenterVO computed = compute(scopedTenantId, tenantScope, normalizedTaskType, normalizedSlaStatus, normalizedStatus);
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
    public List<ApprovalTaskCenterVO> warmupAllTenants() {
        List<ApprovalTaskCenterVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            ApprovalTaskCenterVO computed = compute(tenantId, String.valueOf(tenantId), "ALL", "ALL", "ALL");
            computed.getCache()
                    .setHit(false)
                    .setMode("warmup")
                    .setKey(cacheKey(String.valueOf(tenantId), "ALL", "ALL", "ALL"))
                    .setTtlSeconds(CACHE_TTL_SECONDS)
                    .setCachedAt(LocalDateTime.now());
            writeCache(computed.getCache().getKey(), computed);
            warmed.add(computed);
        }
        return warmed;
    }

    private ApprovalTaskCenterVO compute(Long tenantId, String tenantScope, String taskType, String slaStatus, String status) {
        ApprovalTaskCenterVO vo = new ApprovalTaskCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setTaskType(taskType)
                .setSlaStatus(slaStatus)
                .setStatus(status);

        List<TaskDefinition> definitions = taskDefinitions();
        List<ApprovalTaskCenterVO.TaskItem> allTasks = new ArrayList<>();
        for (TaskDefinition definition : definitions) {
            List<ApprovalTaskCenterVO.TaskItem> tasks = queryTasks(vo, definition, tenantId);
            allTasks.addAll(tasks);
            updateSummary(vo.getSummary(), definition.type, tasks);
        }
        vo.setTasks(allTasks.stream()
                .filter(task -> filterTask(task, taskType, slaStatus, status))
                .limit(200)
                .collect(Collectors.toList()));
        fillSlaSummary(vo);
        return vo;
    }

    private List<TaskDefinition> taskDefinitions() {
        return List.of(
                new TaskDefinition("admission", "入驻审核", "register_review_request", List.of(
                        new PendingCondition("review_status", "review_status IN ('PENDING','pending')")
                ), "/admin/club/applications", "申请人", 72),
                new TaskDefinition("join", "入社审核", "join_request", List.of(
                        new PendingCondition("status", "status IN (0,1,'PENDING','pending')"),
                        new PendingCondition("review_status", "review_status IN ('PENDING','pending')")
                ), "/admin/club/applications", "申请人", 72),
                new TaskDefinition("activity", "活动审核", "activity", List.of(
                        new PendingCondition("approval_status", "approval_status IN (0,1,'PENDING','pending')"),
                        new PendingCondition("manager_review_status", "manager_review_status = 0"),
                        new PendingCondition("teacher_review_status", "teacher_review_status = 0")
                ), "/admin/activity/approval", "活动", 48),
                new TaskDefinition("competition", "比赛审核", "competition", List.of(
                        new PendingCondition("approval_status", "approval_status IN (0,1,'PENDING','pending')"),
                        new PendingCondition("manager_review_status", "manager_review_status = 0"),
                        new PendingCondition("teacher_review_status", "teacher_review_status = 0")
                ), "/admin/activity/competition-approval", "比赛", 48),
                new TaskDefinition("content", "新闻审核", "news", List.of(
                        new PendingCondition("approval_status", "approval_status IN (1,'PENDING','pending')"),
                        new PendingCondition("manager_review_status", "manager_review_status = 0"),
                        new PendingCondition("teacher_review_status", "teacher_review_status = 0")
                ), "/admin/content/news-approval", "新闻", 48),
                new TaskDefinition("finance", "财务审核", "finance_reimbursement", List.of(
                        new PendingCondition("status", "status IN (0,'PENDING','pending')")
                ), "/admin/finance/reimbursement", "报销", 24),
                new TaskDefinition("notice", "通知审核", "notice", List.of(
                        new PendingCondition("manager_review_status", "manager_review_status = 0"),
                        new PendingCondition("teacher_review_status", "teacher_review_status = 0")
                ), "/admin/content/notices", "通知", 48)
        );
    }

    private List<ApprovalTaskCenterVO.TaskItem> queryTasks(ApprovalTaskCenterVO vo, TaskDefinition definition, Long tenantId) {
        TableRef table = resolveTable(definition.baseTable);
        if (table == null) {
            addSource(vo, "approval." + definition.type, definition.baseTable, "missing");
            return List.of(sourceMissingTask(definition));
        }
        String condition = pendingCondition(table, definition.pendingConditions);
        QueryParts where = buildWhere(table, condition, tenantId);
        List<String> columns = existingColumns(table, List.of(
                "id", "title", "activity_name", "name", "username", "real_name", "applicant_name",
                "status", "review_status", "approval_status", "manager_review_status", "teacher_review_status",
                "current_approver_id", "current_step_no", "create_time", "created_at", "created_time", "update_time", "updated_at"));
        if (columns.isEmpty()) {
            addSource(vo, "approval." + definition.type, table.displayName(), "missing-columns");
            return List.of(summaryTask(definition, "缺少可展示字段", "MISSING_COLUMNS"));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName()
                + where.sql
                + orderByExistingTime(table)
                + " LIMIT 50";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "approval." + definition.type, table.displayName(), "ok");
            if (rows == null || rows.isEmpty()) {
                return List.of();
            }
            return rows.stream()
                    .map(row -> toTaskItem(definition, table.displayName(), row))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "approval." + definition.type, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryTask(definition, "明细暂不可用", "FALLBACK"));
        }
    }

    private ApprovalTaskCenterVO.TaskItem toTaskItem(TaskDefinition definition, String source, Map<String, Object> row) {
        LocalDateTime createdAt = firstDateTime(row, "create_time", "created_at", "created_time", "update_time", "updated_at");
        LocalDateTime updatedAt = firstDateTime(row, "update_time", "updated_at", "create_time", "created_at", "created_time");
        long waitingHours = createdAt == null ? 0L : Math.max(0, ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()));
        String rawStatus = firstText(row, "status", "review_status", "approval_status", "manager_review_status", "teacher_review_status");
        String slaStatus = slaStatus(waitingHours, definition.slaHours);
        String title = firstText(row, "title", "activity_name", "name", "real_name", "username", "applicant_name");
        return new ApprovalTaskCenterVO.TaskItem()
                .setTaskType(definition.type)
                .setTaskTypeLabel(definition.label)
                .setTitle(blankToDefault(title, definition.defaultTitle + "待审核"))
                .setStatus(normalizeStatus(rawStatus))
                .setSlaStatus(slaStatus)
                .setRiskLevel("OVERDUE".equals(slaStatus) ? "HIGH" : ("DUE_SOON".equals(slaStatus) ? "MEDIUM" : "LOW"))
                .setSource(source)
                .setSourceId(firstText(row, "id"))
                .setSourcePath(definition.sourcePath)
                .setSubmitterName(firstText(row, "applicant_name", "real_name", "username", "name"))
                .setCurrentNode(resolveCurrentNode(definition, row))
                .setCreatedAt(formatDateTime(createdAt))
                .setUpdatedAt(formatDateTime(updatedAt))
                .setWaitingHours(waitingHours);
    }

    private ApprovalTaskCenterVO.TaskItem sourceMissingTask(TaskDefinition definition) {
        return summaryTask(definition, "来源表缺失", "SOURCE_MISSING");
    }

    private ApprovalTaskCenterVO.TaskItem summaryTask(TaskDefinition definition, String title, String status) {
        return new ApprovalTaskCenterVO.TaskItem()
                .setTaskType(definition.type)
                .setTaskTypeLabel(definition.label)
                .setTitle(definition.label + title)
                .setStatus(status)
                .setSlaStatus("NORMAL")
                .setRiskLevel("LOW")
                .setSource(definition.baseTable)
                .setSourcePath(definition.sourcePath)
                .setCurrentNode(definition.label)
                .setCreatedAt(formatDateTime(LocalDateTime.now()))
                .setUpdatedAt(formatDateTime(LocalDateTime.now()));
    }

    private void updateSummary(ApprovalTaskCenterVO.Summary summary, String type, List<ApprovalTaskCenterVO.TaskItem> tasks) {
        long pending = tasks.stream().filter(task -> !"SOURCE_MISSING".equals(task.getStatus())).count();
        switch (type) {
            case "admission":
                summary.setAdmissionPendingCount(pending);
                break;
            case "join":
                summary.setJoinPendingCount(pending);
                break;
            case "activity":
                summary.setActivityPendingCount(pending);
                break;
            case "competition":
                summary.setCompetitionPendingCount(pending);
                break;
            case "content":
                summary.setContentPendingCount(pending);
                break;
            case "finance":
                summary.setFinancePendingCount(pending);
                break;
            case "notice":
                summary.setNoticePendingCount(pending);
                break;
            default:
                break;
        }
    }

    private void fillSlaSummary(ApprovalTaskCenterVO vo) {
        long total = vo.getSummary().getAdmissionPendingCount()
                + vo.getSummary().getJoinPendingCount()
                + vo.getSummary().getActivityPendingCount()
                + vo.getSummary().getCompetitionPendingCount()
                + vo.getSummary().getContentPendingCount()
                + vo.getSummary().getFinancePendingCount()
                + vo.getSummary().getNoticePendingCount();
        vo.getSummary().setTotalPendingCount(total);
        vo.getSummary().setOverdueCount(vo.getTasks().stream().filter(task -> "OVERDUE".equals(task.getSlaStatus())).count());
        vo.getSummary().setDueSoonCount(vo.getTasks().stream().filter(task -> "DUE_SOON".equals(task.getSlaStatus())).count());
        vo.getSummary().setNormalCount(vo.getTasks().stream().filter(task -> "NORMAL".equals(task.getSlaStatus())).count());
    }

    private boolean filterTask(ApprovalTaskCenterVO.TaskItem task, String taskType, String slaStatus, String status) {
        if (!"ALL".equals(taskType) && !taskType.equalsIgnoreCase(task.getTaskType())) {
            return false;
        }
        if (!"ALL".equals(slaStatus) && !slaStatus.equalsIgnoreCase(task.getSlaStatus())) {
            return false;
        }
        return "ALL".equals(status) || status.equalsIgnoreCase(task.getStatus());
    }

    private String resolveCurrentNode(TaskDefinition definition, Map<String, Object> row) {
        String managerStatus = firstText(row, "manager_review_status");
        String teacherStatus = firstText(row, "teacher_review_status");
        if ("0".equals(managerStatus)) {
            return "负责人审核";
        }
        if ("0".equals(teacherStatus)) {
            return "指导老师审核";
        }
        String stepNo = firstText(row, "current_step_no");
        if (!stepNo.isBlank()) {
            return "第 " + stepNo + " 级审批";
        }
        return definition.label;
    }

    private String normalizeStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return "PENDING";
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        if ("0".equals(normalized) || "1".equals(normalized) || "PENDING".equals(normalized)) {
            return "PENDING";
        }
        return normalized;
    }

    private String slaStatus(long waitingHours, int slaHours) {
        if (waitingHours >= slaHours) {
            return "OVERDUE";
        }
        if (waitingHours >= Math.max(1, slaHours * 0.75D)) {
            return "DUE_SOON";
        }
        return "NORMAL";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME_FORMATTER.format(value);
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

    private LocalDateTime firstDateTime(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            LocalDateTime parsed = parseDateTime(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Date) {
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (RuntimeException ignored) {
            try {
                return LocalDateTime.parse(text, DATE_TIME_FORMATTER);
            } catch (RuntimeException ignoredAgain) {
                return null;
            }
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
            log.warn("list tenant ids for approval task center failed: {}", e.getMessage());
            return fallbackTenantIds();
        }
    }

    private List<Long> fallbackTenantIds() {
        Long fallbackTenantId = resolveTenantId(null);
        return fallbackTenantId == null || fallbackTenantId <= 0 ? List.of(1L) : List.of(fallbackTenantId);
    }

    private String cacheKey(String tenantScope, String taskType, String slaStatus, String status) {
        return KEY_APPROVAL_TASK_CENTER + tenantScope + ":" + taskType + ":" + slaStatus + ":" + status;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private ApprovalTaskCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, ApprovalTaskCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read approval task center cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, ApprovalTaskCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write approval task center cache failed: {}", e.getMessage());
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
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant")),
                Map.entry("register_review_request", List.of("rk_user.register_review_request", "register_review_request")),
                Map.entry("join_request", List.of("rk_user.join_requests", "join_requests")),
                Map.entry("finance_reimbursement", List.of("rk_user.finance_reimbursement", "finance_reimbursement")),
                Map.entry("activity", List.of("rk_activity.rk_activity", "rk_activity")),
                Map.entry("competition", List.of("rk_activity.competition_competitions", "competition_competitions")),
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("notice", List.of("rk_message.rk_notice", "rk_notice"))
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
        String safeCondition = extraCondition;
        if (safeCondition != null && !safeCondition.isEmpty()) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append("(").append(safeCondition).append(")");
        }
        return new QueryParts(where.toString(), args);
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        String lower = condition.toLowerCase(Locale.ROOT);
        for (String column : List.of("is_deleted", "status", "review_status", "approval_status", "manager_review_status", "teacher_review_status", "create_time")) {
            if (conditionReferencesColumn(lower, column) && !columnExists(table, column)) {
                return null;
            }
        }
        return condition.trim();
    }

    private boolean conditionReferencesColumn(String lowerCondition, String column) {
        return Pattern.compile("(?<![a-z0-9_])" + Pattern.quote(column.toLowerCase(Locale.ROOT)) + "(?![a-z0-9_])")
                .matcher(lowerCondition)
                .find();
    }

    private String pendingCondition(TableRef table, List<PendingCondition> conditions) {
        List<String> existingConditions = conditions.stream()
                .filter(condition -> columnExists(table, condition.columnName))
                .map(condition -> condition.expression)
                .collect(Collectors.toList());
        return existingConditions.isEmpty() ? null : String.join(" OR ", existingConditions);
    }

    private void addSource(ApprovalTaskCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new ApprovalTaskCenterVO.SourceInfo()
                .setMetric(metric)
                .setTableName(tableName)
                .setStatus(status.toLowerCase(Locale.ROOT))
                .setUpdatedAt(LocalDateTime.now()));
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static class TaskDefinition {
        private final String type;
        private final String label;
        private final String baseTable;
        private final List<PendingCondition> pendingConditions;
        private final String sourcePath;
        private final String defaultTitle;
        private final int slaHours;

        private TaskDefinition(String type, String label, String baseTable, List<PendingCondition> pendingConditions, String sourcePath, String defaultTitle, int slaHours) {
            this.type = type;
            this.label = label;
            this.baseTable = baseTable;
            this.pendingConditions = pendingConditions;
            this.sourcePath = sourcePath;
            this.defaultTitle = defaultTitle;
            this.slaHours = slaHours;
        }
    }

    private static class PendingCondition {
        private final String columnName;
        private final String expression;

        private PendingCondition(String columnName, String expression) {
            this.columnName = columnName;
            this.expression = expression;
        }
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
