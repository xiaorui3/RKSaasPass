package com.tianji.data.service.impl;

import com.tianji.data.service.FrontendCacheSnapshotProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DefaultFrontendCacheSnapshotProvider implements FrontendCacheSnapshotProvider {

    private static final int SNAPSHOT_LIMIT = 30;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Long> listTenantIds() {
        TableRef tenantTable = resolveTable("tenant", "rk_user.rk_tenant", "rk_tenant");
        if (tenantTable == null) {
            return List.of(1L);
        }
        String sql = "SELECT id FROM " + tenantTable.qualifiedName()
                + optionalWhere(tenantTable, "is_deleted = 0")
                + " ORDER BY id ASC LIMIT 200";
        try {
            List<Long> tenantIds = jdbcTemplate.queryForList(sql, Long.class);
            return CollectionUtils.isEmpty(tenantIds) ? List.of(1L) : tenantIds;
        } catch (RuntimeException e) {
            return List.of(1L);
        }
    }

    @Override
    public List<FrontendCacheWarmupServiceImpl.FrontendCacheItem> loadVisibleContent(Long tenantId) {
        List<FrontendCacheWarmupServiceImpl.FrontendCacheItem> items = new ArrayList<>();
        items.add(snapshot("tenant-public", tenantPublicSnapshot(tenantId)));
        items.add(snapshot("news", listRows(tenantId, "news", List.of("id", "title", "summary", "cover_image", "category", "publish_time", "update_time"),
                "is_deleted = 0 AND is_published = 1", "COALESCE(publish_time, update_time, create_time) DESC")));
        items.add(snapshot("works", listRows(tenantId, "works", List.of("id", "title", "description", "cover_image", "category", "update_time"),
                "is_deleted = 0", "COALESCE(update_time, create_time) DESC")));
        items.add(snapshot("notices", listRows(tenantId, "notice", List.of("id", "title", "content", "cover_image", "notice_type", "publish_time", "update_time"),
                "is_deleted = 0 AND (is_published = 1 OR is_published IS TRUE)", "COALESCE(publish_time, update_time, create_time) DESC")));
        items.add(snapshot("activities", listRows(tenantId, "activity", List.of("id", "activity_name", "cover_image", "location", "activity_status", "start_time", "end_time", "update_time"),
                "is_deleted = 0", "COALESCE(start_time, update_time, create_time) DESC")));
        items.add(snapshot("competitions", listRows(tenantId, "competition", List.of("id", "title", "summary", "cover_image", "status", "competition_start", "competition_end", "update_time"),
                "is_deleted = 0", "COALESCE(competition_start, update_time, create_time) DESC")));
        items.add(snapshot("members", listRows(tenantId, "member", List.of("id", "name", "student_id", "department", "position", "status", "update_time"),
                "is_deleted = 0", "COALESCE(update_time, create_time) DESC")));
        items.add(snapshot("alumni", listRows(tenantId, "alumni", List.of("id", "name", "student_id", "department", "position", "work_unit", "is_active", "update_time"),
                "is_deleted = 0", "COALESCE(update_time, create_time) DESC")));
        items.add(snapshot("history", listRows(tenantId, "history", List.of("id", "title", "description", "event_date", "year", "event_type", "update_time"),
                "is_deleted = 0", "COALESCE(event_date, update_time, create_time) DESC")));
        items.add(snapshot("summary", summarySnapshot(tenantId, items)));
        return items;
    }

    private FrontendCacheWarmupServiceImpl.FrontendCacheItem snapshot(String contentType, Object payload) {
        boolean empty = payload == null
                || payload instanceof List && ((List<?>) payload).isEmpty()
                || payload instanceof Map && ((Map<?, ?>) payload).isEmpty();
        return new FrontendCacheWarmupServiceImpl.FrontendCacheItem(contentType, payload == null ? List.of() : payload, empty);
    }

    private Map<String, Object> summarySnapshot(Long tenantId, List<FrontendCacheWarmupServiceImpl.FrontendCacheItem> items) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tenantId", tenantId);
        summary.put("generatedAt", LocalDateTime.now().toString());
        summary.put("contentTypes", items.stream().map(FrontendCacheWarmupServiceImpl.FrontendCacheItem::getContentType).collect(Collectors.toList()));
        Map<String, Object> counts = new LinkedHashMap<>();
        for (FrontendCacheWarmupServiceImpl.FrontendCacheItem item : items) {
            Object payload = item.getPayload();
            counts.put(item.getContentType(), payload instanceof List ? ((List<?>) payload).size() : (item.isEmptyResult() ? 0 : 1));
        }
        summary.put("counts", counts);
        return summary;
    }

    private Map<String, Object> tenantPublicSnapshot(Long tenantId) {
        TableRef table = resolveTable("tenant", "rk_user.rk_tenant", "rk_tenant");
        if (table == null) {
            return Map.of("tenantId", tenantId);
        }
        List<String> columns = existingColumns(table, List.of("id", "tenant_name", "tenant_code", "logo_url", "description", "status", "update_time"));
        if (columns.isEmpty()) {
            return Map.of("tenantId", tenantId);
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName()
                + optionalWhere(table, "id = ? AND is_deleted = 0")
                + " LIMIT 1";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, tenantId);
            return rows.isEmpty() ? Map.of("tenantId", tenantId) : rows.get(0);
        } catch (RuntimeException e) {
            return Map.of("tenantId", tenantId);
        }
    }

    private List<Map<String, Object>> listRows(Long tenantId, String logicalTable, List<String> columns, String extraCondition, String orderBy) {
        TableRef table = resolveTable(logicalTable);
        if (table == null) {
            return List.of();
        }
        List<String> existing = existingColumns(table, columns);
        if (existing.isEmpty()) {
            return List.of();
        }
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        if (columnExists(table, "tenant_id")) {
            where.append(" WHERE tenant_id = ?");
            args.add(tenantId);
        }
        String safeCondition = existingCondition(table, extraCondition);
        if (StringUtils.hasText(safeCondition)) {
            where.append(where.length() == 0 ? " WHERE " : " AND ").append(safeCondition);
        }
        String safeOrder = existingOrderBy(table, orderBy);
        String sql = "SELECT " + selectColumns(existing) + " FROM " + table.qualifiedName()
                + where
                + (StringUtils.hasText(safeOrder) ? " ORDER BY " + safeOrder : "")
                + " LIMIT " + SNAPSHOT_LIMIT;
        try {
            return jdbcTemplate.queryForList(sql, args.toArray());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private TableRef resolveTable(String logicalTable, String... explicitCandidates) {
        List<String> candidates = new ArrayList<>();
        candidates.addAll(Arrays.asList(explicitCandidates));
        switch (logicalTable) {
            case "tenant":
                candidates.addAll(List.of("rk_user.rk_tenant", "rk_tenant"));
                break;
            case "news":
                candidates.addAll(List.of("rk_content.news", "news"));
                break;
            case "works":
                candidates.addAll(List.of("rk_content.works", "works"));
                break;
            case "notice":
                candidates.addAll(List.of("rk_message.rk_notice", "rk_notice"));
                break;
            case "activity":
                candidates.addAll(List.of("rk_activity.rk_activity", "rk_activity"));
                break;
            case "competition":
                candidates.addAll(List.of("rk_activity.competition_competitions", "competition_competitions"));
                break;
            case "member":
                candidates.addAll(List.of("rk_user.club_members", "club_members"));
                break;
            case "alumni":
                candidates.addAll(List.of("rk_user.club_alumni", "club_alumni"));
                break;
            case "history":
                candidates.addAll(List.of("rk_user.club_history", "club_history"));
                break;
            default:
                candidates.add(logicalTable);
                break;
        }
        for (String candidate : candidates) {
            TableRef table = TableRef.parse(candidate);
            if (tableExists(table)) {
                return table;
            }
        }
        return null;
    }

    private String optionalWhere(TableRef table, String condition) {
        String safeCondition = existingCondition(table, condition);
        return StringUtils.hasText(safeCondition) ? " WHERE " + safeCondition : "";
    }

    private String existingCondition(TableRef table, String condition) {
        if (!StringUtils.hasText(condition)) {
            return "";
        }
        String result = condition;
        result = removeMissingColumnCondition(table, result, "is_deleted");
        result = removeMissingColumnCondition(table, result, "is_published");
        result = removeMissingColumnCondition(table, result, "status");
        result = result.replaceAll("(?i)\\s+AND\\s+AND\\s+", " AND ");
        result = result.replaceAll("(?i)^\\s*AND\\s+", "");
        result = result.replaceAll("(?i)\\s+AND\\s*$", "");
        return result.trim();
    }

    private String removeMissingColumnCondition(TableRef table, String condition, String column) {
        if (columnExists(table, column)) {
            return condition;
        }
        return condition
                .replaceAll("(?i)\\s*AND\\s*\\(?\\s*" + column + "\\s*=[^)]+\\)?", "")
                .replaceAll("(?i)\\(?\\s*" + column + "\\s*=[^)]+\\)?\\s*AND\\s*", "");
    }

    private String existingOrderBy(TableRef table, String orderBy) {
        if (!StringUtils.hasText(orderBy)) {
            return "";
        }
        if (orderBy.contains("COALESCE")) {
            List<String> columns = List.of("publish_time", "update_time", "create_time", "start_time", "competition_start", "event_date");
            List<String> existing = columns.stream().filter(column -> columnExists(table, column)).collect(Collectors.toList());
            return existing.isEmpty() ? "" : "COALESCE(" + String.join(", ", existing) + ") DESC";
        }
        return orderBy;
    }

    private List<String> existingColumns(TableRef table, List<String> columns) {
        return columns.stream().filter(column -> columnExists(table, column)).collect(Collectors.toList());
    }

    private String selectColumns(List<String> columns) {
        return columns.stream().map(column -> "`" + column + "`").collect(Collectors.joining(", "));
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
    }
}
