package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.ContentPublishCalendarVO;
import com.tianji.data.service.ContentPublishCalendarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tianji.data.constants.RedisConstants.KEY_CONTENT_PUBLISH_CALENDAR;

@Slf4j
@Service
public class ContentPublishCalendarServiceImpl implements ContentPublishCalendarService {
    private static final int CACHE_TTL_SECONDS = 210;
    private static final int CACHE_TTL_JITTER_SECONDS = 120;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<String> RANGE_COLUMNS = Arrays.asList(
            "publish_time", "start_time", "competition_start", "create_time", "created_at", "update_time"
    );

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public ContentPublishCalendarServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ContentPublishCalendarVO getContentPublishCalendar(Long requestedTenantId, String timeRange, String contentType, String status) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedTimeRange = normalizeTimeRange(timeRange);
        String normalizedContentType = normalizeFilter(contentType);
        String normalizedStatus = normalizeFilter(status);
        String cacheKey = cacheKey(tenantScope, normalizedTimeRange, normalizedContentType, normalizedStatus);

        ContentPublishCalendarVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        ContentPublishCalendarVO computed = compute(scopedTenantId, tenantScope, normalizedTimeRange, normalizedContentType, normalizedStatus);
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
    public List<ContentPublishCalendarVO> warmupAllTenants() {
        List<ContentPublishCalendarVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            ContentPublishCalendarVO computed = compute(tenantId, String.valueOf(tenantId), "30D", "ALL", "ALL");
            computed.getCache()
                    .setHit(false)
                    .setMode("warmup")
                    .setKey(cacheKey(String.valueOf(tenantId), "30D", "ALL", "ALL"))
                    .setTtlSeconds(CACHE_TTL_SECONDS)
                    .setCachedAt(LocalDateTime.now());
            writeCache(computed.getCache().getKey(), computed);
            warmed.add(computed);
        }
        return warmed;
    }

    private ContentPublishCalendarVO compute(Long tenantId, String tenantScope, String timeRange, String contentType, String status) {
        ContentPublishCalendarVO vo = new ContentPublishCalendarVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setTimeRange(timeRange)
                .setContentType(contentType)
                .setStatus(status);

        List<ContentPublishCalendarVO.TimelineItem> timelineRows = new ArrayList<>();
        for (ContentSource source : contentSources()) {
            if (!"ALL".equals(contentType) && !contentType.equalsIgnoreCase(source.type)) {
                continue;
            }
            timelineRows.addAll(queryTimelineRows(vo, source, tenantId, timeRange, status));
        }
        timelineRows.sort(Comparator.comparing(ContentPublishCalendarVO.TimelineItem::getPublishAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        vo.setTimelineRows(timelineRows.stream().limit(200).collect(Collectors.toList()));
        fillSummary(vo);
        fillSummaryCards(vo);
        fillCalendarRows(vo);
        fillRisks(vo);
        return vo;
    }

    private List<ContentPublishCalendarVO.TimelineItem> queryTimelineRows(
            ContentPublishCalendarVO vo,
            ContentSource source,
            Long tenantId,
            String timeRange,
            String statusFilter
    ) {
        TableRef table = resolveTable(source.baseTable);
        if (table == null) {
            addSource(vo, "calendar." + source.type, source.baseTable, "missing");
            return List.of();
        }
        String titleColumn = firstExistingColumn(table, source.titleColumns);
        String dateColumn = firstExistingColumn(table, source.dateColumns);
        if (titleColumn == null || dateColumn == null) {
            addSource(vo, "calendar." + source.type, table.displayName(), "missing-columns");
            return List.of();
        }

        String statusColumn = firstExistingColumn(table, source.statusColumns);
        String condition = rangeCondition(dateColumn, daysForRange(timeRange));
        condition = appendCondition(condition, optionalNotDeleted(table));
        QueryParts where = buildWhere(table, condition, tenantId);
        String idExpr = columnExists(table, "id") ? "`id`" : "0";
        String statusExpr = statusColumn == null ? "NULL" : "`" + statusColumn + "`";
        String sql = "SELECT " + idExpr + " AS id, `" + titleColumn + "` AS title, `" + dateColumn + "` AS date_value, "
                + statusExpr + " AS status_value FROM " + table.qualifiedName()
                + where.sql + " ORDER BY `" + dateColumn + "` DESC LIMIT 80";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "calendar." + source.type, table.displayName(), "ok");
            return rows.stream()
                    .map(row -> toTimelineItem(source, table, row))
                    .filter(item -> "ALL".equals(statusFilter) || statusFilter.equalsIgnoreCase(item.getStatus()))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "calendar." + source.type, table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of();
        }
    }

    private ContentPublishCalendarVO.TimelineItem toTimelineItem(ContentSource source, TableRef table, Map<String, Object> row) {
        Long id = toLong(row.get("id"));
        LocalDateTime publishAt = toLocalDateTime(row.get("date_value"));
        String status = normalizeStatus(row.get("status_value"), publishAt);
        return new ContentPublishCalendarVO.TimelineItem()
                .setId(id)
                .setTitle(toText(row.get("title"), source.typeName + " #" + (id == null ? "-" : id)))
                .setContentType(source.type.toLowerCase(Locale.ROOT))
                .setContentTypeName(source.typeName)
                .setStatus(status)
                .setStatusName(statusName(status))
                .setPublishAt(publishAt)
                .setSource(table.displayName())
                .setTargetPath(source.targetPathPrefix + (id == null ? "" : id));
    }

    private void fillSummary(ContentPublishCalendarVO vo) {
        ContentPublishCalendarVO.Summary summary = vo.getSummary();
        for (ContentPublishCalendarVO.TimelineItem item : vo.getTimelineRows()) {
            summary.setTotalCount(summary.getTotalCount() + 1);
            if ("PUBLISHED".equals(item.getStatus())) {
                summary.setPublishedCount(summary.getPublishedCount() + 1);
            } else if ("PENDING".equals(item.getStatus())) {
                summary.setPendingCount(summary.getPendingCount() + 1);
            } else if ("SCHEDULED".equals(item.getStatus())) {
                summary.setScheduledCount(summary.getScheduledCount() + 1);
            } else if ("OVERDUE".equals(item.getStatus())) {
                summary.setOverdueCount(summary.getOverdueCount() + 1);
            } else if ("REJECTED".equals(item.getStatus())) {
                summary.setRejectedCount(summary.getRejectedCount() + 1);
            }
            switch (item.getContentType()) {
                case "news":
                    summary.setNewsCount(summary.getNewsCount() + 1);
                    break;
                case "notice":
                    summary.setNoticeCount(summary.getNoticeCount() + 1);
                    break;
                case "works":
                    summary.setWorksCount(summary.getWorksCount() + 1);
                    break;
                case "activity":
                    summary.setActivityCount(summary.getActivityCount() + 1);
                    break;
                case "competition":
                    summary.setCompetitionCount(summary.getCompetitionCount() + 1);
                    break;
                default:
                    break;
            }
        }
    }

    private void fillSummaryCards(ContentPublishCalendarVO vo) {
        ContentPublishCalendarVO.Summary summary = vo.getSummary();
        vo.getSummaryCards().add(card("totalCount", "发布排期", summary.getTotalCount(), "当前筛选范围内的内容总数", "primary"));
        vo.getSummaryCards().add(card("publishedCount", "已发布", summary.getPublishedCount(), "已通过并可展示的内容", "success"));
        vo.getSummaryCards().add(card("pendingCount", "待审核", summary.getPendingCount(), "需要负责人或指导老师处理", "warning"));
        vo.getSummaryCards().add(card("scheduledCount", "计划发布", summary.getScheduledCount(), "日期在当前时间之后", "info"));
        vo.getSummaryCards().add(card("overdueCount", "延期风险", summary.getOverdueCount(), "排期已过但仍未发布", "danger"));
    }

    private ContentPublishCalendarVO.SummaryCard card(String metric, String label, long value, String hint, String tone) {
        return new ContentPublishCalendarVO.SummaryCard()
                .setMetric(metric)
                .setLabel(label)
                .setValue(value)
                .setHint(hint)
                .setTone(tone);
    }

    private void fillCalendarRows(ContentPublishCalendarVO vo) {
        Map<LocalDate, ContentPublishCalendarVO.CalendarItem> grouped = new LinkedHashMap<>();
        for (ContentPublishCalendarVO.TimelineItem item : vo.getTimelineRows()) {
            LocalDate date = item.getPublishAt() == null ? LocalDate.now() : item.getPublishAt().toLocalDate();
            ContentPublishCalendarVO.CalendarItem row = grouped.computeIfAbsent(date, key -> new ContentPublishCalendarVO.CalendarItem().setDate(key));
            row.setTotalCount(row.getTotalCount() + 1);
            if ("PUBLISHED".equals(item.getStatus())) {
                row.setPublishedCount(row.getPublishedCount() + 1);
            } else if ("PENDING".equals(item.getStatus())) {
                row.setPendingCount(row.getPendingCount() + 1);
            } else if ("SCHEDULED".equals(item.getStatus())) {
                row.setScheduledCount(row.getScheduledCount() + 1);
            } else if ("OVERDUE".equals(item.getStatus())) {
                row.setOverdueCount(row.getOverdueCount() + 1);
            } else if ("REJECTED".equals(item.getStatus())) {
                row.setRejectedCount(row.getRejectedCount() + 1);
            }
            if ("news".equals(item.getContentType())) {
                row.setNewsCount(row.getNewsCount() + 1);
            } else if ("notice".equals(item.getContentType())) {
                row.setNoticeCount(row.getNoticeCount() + 1);
            } else if ("works".equals(item.getContentType())) {
                row.setWorksCount(row.getWorksCount() + 1);
            } else if ("activity".equals(item.getContentType())) {
                row.setActivityCount(row.getActivityCount() + 1);
            } else if ("competition".equals(item.getContentType())) {
                row.setCompetitionCount(row.getCompetitionCount() + 1);
            }
        }
        vo.setCalendarRows(grouped.values().stream()
                .sorted(Comparator.comparing(ContentPublishCalendarVO.CalendarItem::getDate))
                .collect(Collectors.toList()));
    }

    private void fillRisks(ContentPublishCalendarVO vo) {
        ContentPublishCalendarVO.Summary summary = vo.getSummary();
        if (summary.getPendingCount() > 0) {
            vo.getRiskRows().add(risk("pending_review", "待审核内容积压", "MEDIUM", "NEED_REVIEW", "优先处理新闻、活动、比赛的负责人和指导老师审核"));
        }
        if (summary.getOverdueCount() > 0) {
            vo.getRiskRows().add(risk("overdue_publish", "存在延期发布风险", "HIGH", "NEED_ACTION", "核对已过排期但未发布的内容，补审或调整发布时间"));
        }
        if (summary.getScheduledCount() == 0 && summary.getTotalCount() > 0) {
            vo.getRiskRows().add(risk("no_schedule", "近期缺少后续发布排期", "LOW", "WATCH", "补充活动、比赛或内容预告，保持前台内容连续更新"));
        }
        if (vo.getRiskRows().isEmpty()) {
            vo.getRiskRows().add(risk("normal", "内容发布节奏正常", "LOW", "NORMAL", "保持定时预热和审核节奏"));
        }
    }

    private ContentPublishCalendarVO.RiskItem risk(String riskType, String title, String riskLevel, String status, String suggestion) {
        return new ContentPublishCalendarVO.RiskItem()
                .setRiskType(riskType)
                .setTitle(title)
                .setRiskLevel(riskLevel)
                .setStatus(status)
                .setSuggestion(suggestion)
                .setDetectedAt(LocalDateTime.now());
    }

    private String normalizeStatus(Object rawStatus, LocalDateTime dateTime) {
        if (dateTime != null && dateTime.isAfter(LocalDateTime.now())) {
            return "SCHEDULED";
        }
        String value = rawStatus == null ? "" : String.valueOf(rawStatus).trim();
        if (value.isEmpty()) {
            return dateTime != null && dateTime.isBefore(LocalDateTime.now().minusDays(1)) ? "OVERDUE" : "PENDING";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if (List.of("1", "2", "TRUE", "APPROVED", "PASS", "PASSED", "PUBLISHED", "REGISTRATION", "ONGOING", "COMPLETED").contains(upper)) {
            return "PUBLISHED";
        }
        if (List.of("0", "PENDING", "DRAFT", "REVIEWING").contains(upper)) {
            return dateTime != null && dateTime.isBefore(LocalDateTime.now().minusDays(1)) ? "OVERDUE" : "PENDING";
        }
        if (List.of("3", "2", "FALSE", "REJECTED", "CANCELLED").contains(upper)) {
            return "REJECTED";
        }
        return "PUBLISHED";
    }

    private String statusName(String status) {
        if ("PUBLISHED".equals(status)) {
            return "已发布";
        }
        if ("PENDING".equals(status)) {
            return "待审核";
        }
        if ("SCHEDULED".equals(status)) {
            return "计划发布";
        }
        if ("OVERDUE".equals(status)) {
            return "延期风险";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        return "未知";
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
            log.warn("list tenant ids for content publish calendar failed: {}", e.getMessage());
            return fallbackTenantIds();
        }
    }

    private List<Long> fallbackTenantIds() {
        Long fallbackTenantId = resolveTenantId(null);
        return fallbackTenantId == null || fallbackTenantId <= 0 ? List.of(1L) : List.of(fallbackTenantId);
    }

    private ContentPublishCalendarVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, ContentPublishCalendarVO.class);
        } catch (RuntimeException e) {
            log.warn("read content publish calendar cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, ContentPublishCalendarVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write content publish calendar cache failed: {}", e.getMessage());
        }
    }

    private String cacheKey(String tenantScope, String timeRange, String contentType, String status) {
        return KEY_CONTENT_PUBLISH_CALENDAR + tenantScope + ":" + timeRange + ":" + contentType + ":" + status;
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
        return List.of("7D", "30D", "90D", "180D", "ALL").contains(normalized) ? normalized : "30D";
    }

    private int daysForRange(String timeRange) {
        if ("7D".equals(timeRange)) {
            return 7;
        }
        if ("90D".equals(timeRange)) {
            return 90;
        }
        if ("180D".equals(timeRange)) {
            return 180;
        }
        if ("ALL".equals(timeRange)) {
            return 3650;
        }
        return 30;
    }

    private String rangeCondition(String columnName, int days) {
        return "`" + columnName + "` >= '" + LocalDate.now().minusDays(days) + " 00:00:00'";
    }

    private String optionalNotDeleted(TableRef table) {
        if (columnExists(table, "is_deleted")) {
            return "is_deleted = 0";
        }
        if (columnExists(table, "deleted")) {
            return "deleted = 0";
        }
        return null;
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
                Map.entry("news", List.of("rk_content.news", "news")),
                Map.entry("notice", List.of("rk_content.notice", "notice", "rk_content.notices", "notices")),
                Map.entry("works", List.of("rk_content.works", "works")),
                Map.entry("activity", List.of("rk_activity.rk_activity", "rk_activity")),
                Map.entry("competition", List.of("rk_activity.competition_competitions", "competition_competitions"))
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
        for (String column : RANGE_COLUMNS) {
            if (lower.contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        for (String column : List.of("is_deleted", "deleted", "tenant_id")) {
            if (lower.contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return condition.trim();
    }

    private String appendCondition(String base, String extra) {
        if (extra == null || extra.isBlank()) {
            return base;
        }
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

    private void addSource(ContentPublishCalendarVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new ContentPublishCalendarVO.SourceInfo()
                .setMetric(metric)
                .setTableName(tableName)
                .setStatus(status.toLowerCase(Locale.ROOT))
                .setUpdatedAt(LocalDateTime.now()));
    }

    private List<ContentSource> contentSources() {
        return List.of(
                new ContentSource("NEWS", "新闻", "news", List.of("title", "name"), List.of("publish_time", "create_time", "update_time"), List.of("is_published", "approval_status", "manager_review_status"), "/admin/content/news?id="),
                new ContentSource("NOTICE", "公告", "notice", List.of("title", "notice_title", "name"), List.of("publish_time", "create_time", "update_time"), List.of("is_published", "status"), "/admin/content/notices?id="),
                new ContentSource("WORKS", "作品", "works", List.of("title", "name"), List.of("create_time", "update_time"), List.of("teacher_review_status", "manager_review_status"), "/admin/content/works?id="),
                new ContentSource("ACTIVITY", "活动", "activity", List.of("activity_name", "title", "name"), List.of("start_time", "registration_start_time", "create_time"), List.of("activity_status", "teacher_review_status", "manager_review_status"), "/admin/activity/list?id="),
                new ContentSource("COMPETITION", "比赛", "competition", List.of("title", "name"), List.of("competition_start", "registration_start", "create_time"), List.of("status", "is_published", "teacher_review_status", "manager_review_status"), "/admin/activity/competition?id=")
        );
    }

    private Long toLong(Object value) {
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

    private String toText(Object value, String fallback) {
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime) {
            return (LocalDateTime) value;
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        String text = String.valueOf(value).trim();
        try {
            return LocalDateTime.parse(text.replace('T', ' ').substring(0, Math.min(19, text.length())), DATE_TIME_FORMATTER);
        } catch (RuntimeException e) {
            try {
                return LocalDate.parse(text.substring(0, Math.min(10, text.length()))).atStartOfDay();
            } catch (RuntimeException ignored) {
                return null;
            }
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

    private static class ContentSource {
        private final String type;
        private final String typeName;
        private final String baseTable;
        private final List<String> titleColumns;
        private final List<String> dateColumns;
        private final List<String> statusColumns;
        private final String targetPathPrefix;

        private ContentSource(String type, String typeName, String baseTable, List<String> titleColumns, List<String> dateColumns, List<String> statusColumns, String targetPathPrefix) {
            this.type = type;
            this.typeName = typeName;
            this.baseTable = baseTable;
            this.titleColumns = titleColumns;
            this.dateColumns = dateColumns;
            this.statusColumns = statusColumns;
            this.targetPathPrefix = targetPathPrefix;
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
