package com.tianji.data.service.impl;

import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.vo.OpenSourceHealthVO;
import com.tianji.data.service.FrontendCacheWarmupService;
import com.tianji.data.service.OpenSourceHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenSourceHealthServiceImpl implements OpenSourceHealthService {
    private static final Set<String> REQUIRED_MENU_PATHS = Set.of(
            "/admin/system/users",
            "/admin/system/roles",
            "/admin/system/menus",
            "/admin/system/config",
            "/admin/system/open-source-health",
            "/admin/statistics/tenant-operations",
            "/admin/statistics/data-quality"
    );
    private static final Set<String> REQUIRED_ROLE_CODES = Set.of("ADMIN", "USER", "CLUB_MANAGER", "TEACHER");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final FrontendCacheWarmupService frontendCacheWarmupService;

    @Override
    public OpenSourceHealthVO check(Long requestedTenantId) {
        Long scopedTenantId = resolveTenantId(requestedTenantId);
        OpenSourceHealthVO vo = new OpenSourceHealthVO()
                .setTenantId(scopedTenantId)
                .setTenantScope(scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId))
                .setCheckedAt(LocalDateTime.now());

        List<OpenSourceHealthVO.CheckItem> checks = new ArrayList<>();
        checks.add(safeCheck("MENU_SEED", "菜单 seed", "菜单 seed", "open-source-health.checkMenuSeed", this::checkMenuSeed));
        checks.add(safeCheck("DEFAULT_ROLES", "默认角色", "默认角色", "open-source-health.checkDefaultRoles", () -> checkDefaultRoles(scopedTenantId)));
        checks.add(safeCheck("SEARCH_INDEX", "ES 索引", "ES 索引", "open-source-health.checkSearchIndex", () -> checkSearchIndex(scopedTenantId)));
        checks.add(safeCheck("REDIS_CACHE", "Redis 缓存", "Redis 缓存", "open-source-health.checkRedisCache", this::checkRedisCache));
        checks.add(safeCheck("SYSTEM_CONFIG", "系统配置", "系统配置", "open-source-health.checkSystemConfig", () -> checkSystemConfig(scopedTenantId)));
        checks.add(safeCheck("DEMO_DATA", "演示数据", "演示数据", "open-source-health.checkDemoData", () -> checkDemoData(scopedTenantId)));
        checks.add(safeCheck("FILE_STORAGE", "文件存储", "文件存储", "open-source-health.checkFileStorage", () -> checkFileStorage(scopedTenantId)));
        vo.setChecks(checks);
        fillSummary(vo);
        return vo;
    }

    private OpenSourceHealthVO.CheckItem safeCheck(String code, String category, String title, String source, HealthCheckSupplier supplier) {
        try {
            OpenSourceHealthVO.CheckItem result = supplier.get();
            return result == null
                    ? item(code, category, title, "UNKNOWN", "检查异常：返回结果为空", "检查对应服务和数据库连接", source)
                    : result;
        } catch (Throwable e) {
            return item(code, category, title, "UNKNOWN",
                    "检查异常：" + safeText(e.getMessage(), e.getClass().getSimpleName()),
                    "查看 rk-data 日志并检查对应服务、数据库表和字段是否可用", source);
        }
    }

    private OpenSourceHealthVO.CheckItem checkMenuSeed() {
        TableRef menuTable = resolveTable("menu");
        if (menuTable == null) {
            return item("MENU_SEED", "菜单 seed", "菜单 seed", "UNKNOWN",
                    "菜单表不存在或当前服务无法访问菜单库",
                    "确认 rk-auth 已启动并完成菜单 seed 初始化", "rk_auth.menu");
        }
        try {
            RequiredValueCheck check = countRequiredValues(menuTable, "path", REQUIRED_MENU_PATHS, "is_deleted = 0 AND visible = 1", null, false);
            if (check.missingValues.isEmpty()) {
                return item("MENU_SEED", "菜单 seed", "菜单 seed", "PASS",
                        "核心后台菜单均已落库，必需菜单命中 " + check.matchedCount + "/" + REQUIRED_MENU_PATHS.size(),
                        "无需处理", menuTable.displayName());
            }
            return item("MENU_SEED", "菜单 seed", "菜单 seed", "FAIL",
                    "缺失菜单路径：" + String.join("、", check.missingValues),
                    "重启 rk-auth 或执行菜单目录同步，确保菜单管理和前端路由一致", menuTable.displayName());
        } catch (RuntimeException e) {
            return item("MENU_SEED", "菜单 seed", "菜单 seed", "UNKNOWN",
                    "菜单 seed 检查失败：" + e.getMessage(),
                    "检查 rk-auth 数据库连接和 menu 表结构", menuTable.displayName());
        }
    }
    private OpenSourceHealthVO.CheckItem checkDefaultRoles(Long tenantId) {
        TableRef roleTable = resolveTable("role");
        if (roleTable == null) {
            return item("DEFAULT_ROLES", "默认角色", "默认角色", "UNKNOWN",
                    "角色表不存在或当前服务无法访问认证库",
                    "确认 rk-auth 初始化成功并具备 role 表", "rk_auth.role");
        }
        try {
            RequiredValueCheck check = countRequiredValues(roleTable, "code", REQUIRED_ROLE_CODES, "is_deleted = 0", tenantId, true);
            if (check.missingValues.isEmpty()) {
                return item("DEFAULT_ROLES", "默认角色", "默认角色", "PASS",
                        "默认四类角色已存在：" + String.join("、", REQUIRED_ROLE_CODES),
                        "无需处理", roleTable.displayName());
            }
            String status = check.matchedCount == 0 ? "FAIL" : "WARN";
            return item("DEFAULT_ROLES", "默认角色", "默认角色", status,
                    "缺失默认角色：" + String.join("、", check.missingValues),
                    "在角色管理中补齐默认角色 ADMIN、USER、CLUB_MANAGER、TEACHER，或重启 rk-auth 触发默认角色补齐", roleTable.displayName());
        } catch (RuntimeException e) {
            return item("DEFAULT_ROLES", "默认角色", "默认角色", "UNKNOWN",
                    "默认角色检查失败：" + e.getMessage(),
                    "检查 role 表字段 code、tenant_id、is_deleted 是否存在", roleTable.displayName());
        }
    }
    private OpenSourceHealthVO.CheckItem checkSearchIndex(Long tenantId) {
        TableRef searchTable = resolveTable("search_document");
        if (searchTable == null) {
            return item("SEARCH_INDEX", "ES 索引", "ES 索引", "UNKNOWN",
                    "搜索索引影子表不存在，无法从业务层确认 ES 索引状态",
                    "确认 rk-search 已启动，并重建 rk_global_search 索引后复检", "rk_search.rk_search_index");
        }
        long count = countRows(searchTable, tenantId, "updated_at IS NOT NULL");
        if (count > 0) {
            return item("SEARCH_INDEX", "ES 索引", "ES 索引", "PASS",
                    "搜索索引已有 " + count + " 条可检索文档",
                    "无需处理", searchTable.displayName());
        }
        return item("SEARCH_INDEX", "ES 索引", "ES 索引", "WARN",
                "当前租户没有可确认的搜索索引文档",
                "通过 rk-search 的全局索引重建入口重建 ES 索引", searchTable.displayName());
    }

    private OpenSourceHealthVO.CheckItem checkRedisCache() {
        try {
            Boolean reachable = redisTemplate.hasKey("FRONTEND:CACHE:WARMUP:SUMMARY");
            if (Boolean.TRUE.equals(reachable)) {
                return item("REDIS_CACHE", "Redis 缓存", "Redis 缓存", "PASS",
                        "Redis 可访问，已检测到前台缓存预热摘要 key",
                        "无需处理", "redis.frontend-cache");
            }
            return item("REDIS_CACHE", "Redis 缓存", "Redis 缓存", "WARN",
                    "Redis 可访问但未检测到前台缓存预热摘要 key",
                    "触发前台缓存预热任务，确认 Redis 可用且 TTL 随机化生效", "redis.frontend-cache");
        } catch (RuntimeException e) {
            return item("REDIS_CACHE", "Redis 缓存", "Redis 缓存", "WARN",
                    "Redis 状态读取失败：" + e.getMessage(),
                    "检查 Redis 连接并重新执行前台缓存预热", "redis.frontend-cache");
        }
    }
    private OpenSourceHealthVO.CheckItem checkSystemConfig(Long tenantId) {
        TableRef configTable = resolveTable("system_config");
        if (configTable == null) {
            return item("SYSTEM_CONFIG", "系统配置", "系统配置", "UNKNOWN",
                    "系统配置表不存在",
                    "确认 rk-user 已初始化 system_config 表", "rk_user.system_config");
        }
        long count = countRows(configTable, tenantId, "is_enabled = 1");
        if (count > 0) {
            return item("SYSTEM_CONFIG", "系统配置", "系统配置", "PASS",
                    "当前作用域存在 " + count + " 条启用配置",
                    "无需处理", configTable.displayName());
        }
        return item("SYSTEM_CONFIG", "系统配置", "系统配置", "WARN",
                "当前作用域未发现启用的系统配置",
                "进入系统配置或租户自助配置保存一次默认配置", configTable.displayName());
    }

    private OpenSourceHealthVO.CheckItem checkDemoData(Long tenantId) {
        long tenantCount = countRows(resolveTable("tenant"), tenantId, null);
        long newsCount = countRows(resolveTable("news"), tenantId, null);
        if (tenantCount > 0 && newsCount > 0) {
            return item("DEMO_DATA", "演示数据", "演示数据", "PASS",
                    "租户和公开内容均存在，tenantCount=" + tenantCount + "，newsCount=" + newsCount,
                    "无需处理", "rk_user.rk_tenant/rk_content.news");
        }
        if (tenantCount > 0) {
            return item("DEMO_DATA", "演示数据", "演示数据", "WARN",
                    "租户存在但演示新闻或公开内容不足",
                    "使用后台手动新增少量新闻、活动和比赛，便于开源体验验证", "rk_user.rk_tenant/rk_content.news");
        }
        return item("DEMO_DATA", "演示数据", "演示数据", "UNKNOWN",
                "未发现可用租户或租户表不可访问",
                "确认 rk-user 租户数据已初始化", "rk_user.rk_tenant");
    }

    private OpenSourceHealthVO.CheckItem checkFileStorage(Long tenantId) {
        TableRef fileTable = resolveTable("file");
        if (fileTable == null) {
            return item("FILE_STORAGE", "文件存储", "文件存储", "UNKNOWN",
                    "文件台账表不存在，无法从业务层确认文件上传记录",
                    "确认 rk-file 已启动并完成文件表初始化", "rk_file.file/rk_file.rk_file");
        }
        long count = countRows(fileTable, tenantId, null);
        if (count > 0) {
            return item("FILE_STORAGE", "文件存储", "文件存储", "PASS",
                    "文件台账存在 " + count + " 条记录",
                    "无需处理", fileTable.displayName());
        }
        return item("FILE_STORAGE", "文件存储", "文件存储", "WARN",
                "文件台账暂无记录，无法验证文件存储链路是否完整",
                "通过后台上传一张租户 Logo 或新闻封面后复检文件存储", fileTable.displayName());
    }

    private RequiredValueCheck countRequiredValues(TableRef table, String columnName, Set<String> requiredValues, String extraCondition, Long tenantId, boolean caseInsensitive) {
        RequiredValueCheck result = new RequiredValueCheck();
        if (table == null || !columnExists(table, columnName)) {
            result.missingValues.addAll(requiredValues);
            return result;
        }
        for (String value : requiredValues) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            long count = countRequiredValue(table, columnName, value, extraCondition, tenantId, caseInsensitive);
            if (count > 0) {
                result.matchedCount++;
            } else {
                result.missingValues.add(value);
            }
        }
        return result;
    }

    private long countRequiredValue(TableRef table, String columnName, String value, String extraCondition, Long tenantId, boolean caseInsensitive) {
        try {
            QueryParts where = buildWhere(table, extraCondition, tenantId);
            List<Object> args = new ArrayList<>(where.args);
            StringBuilder sql = new StringBuilder("SELECT COUNT(1) FROM ")
                    .append(table.qualifiedName())
                    .append(where.sql);
            sql.append(where.sql.isEmpty() ? " WHERE " : " AND ");
            if (caseInsensitive) {
                sql.append("UPPER(").append(quoteColumn(columnName)).append(") = ?");
                args.add(value.toUpperCase(Locale.ROOT));
            } else {
                sql.append(quoteColumn(columnName)).append(" = ?");
                args.add(value);
            }
            Number count = jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
            return count == null ? 0L : count.longValue();
        } catch (RuntimeException e) {
            return 0L;
        }
    }
    private long countRows(TableRef table, Long tenantId, String extraCondition) {
        if (table == null) {
            return 0L;
        }
        try {
            QueryParts where = buildWhere(table, extraCondition, tenantId);
            String sql = "SELECT COUNT(1) FROM " + table.qualifiedName() + where.sql;
            Number value = jdbcTemplate.queryForObject(sql, Long.class, where.args.toArray());
            return value == null ? 0L : value.longValue();
        } catch (RuntimeException e) {
            return 0L;
        }
    }

    private void fillSummary(OpenSourceHealthVO vo) {
        OpenSourceHealthVO.Summary summary = new OpenSourceHealthVO.Summary();
        summary.setTotalCount(vo.getChecks().size());
        for (OpenSourceHealthVO.CheckItem item : vo.getChecks()) {
            String status = item.getStatus();
            if ("PASS".equals(status)) {
                summary.setPassCount(summary.getPassCount() + 1);
            } else if ("WARN".equals(status)) {
                summary.setWarnCount(summary.getWarnCount() + 1);
            } else if ("FAIL".equals(status)) {
                summary.setFailCount(summary.getFailCount() + 1);
            } else {
                summary.setUnknownCount(summary.getUnknownCount() + 1);
            }
        }
        vo.setSummary(summary);
        if (summary.getFailCount() > 0) {
            vo.setOverallStatus("FAIL");
        } else if (summary.getWarnCount() > 0) {
            vo.setOverallStatus("WARN");
        } else if (summary.getUnknownCount() > 0) {
            vo.setOverallStatus("UNKNOWN");
        } else {
            vo.setOverallStatus("PASS");
        }
    }

    private OpenSourceHealthVO.CheckItem item(String code, String category, String title, String status, String diagnostics, String suggestion, String source) {
        return new OpenSourceHealthVO.CheckItem()
                .setCode(code)
                .setCategory(category)
                .setTitle(title)
                .setStatus(status)
                .setDiagnostics(safeText(diagnostics, "-"))
                .setSuggestion(safeText(suggestion, "-"))
                .setSource(safeText(source, "-"))
                .setCheckedAt(LocalDateTime.now());
    }

    private Long resolveTenantId(Long requestedTenantId) {
        Long currentTenant = TenantContext.getTenantId();
        if (currentTenant != null) {
            return currentTenant;
        }
        return requestedTenantId;
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
                Map.entry("menu", List.of("rk_auth.rk_menu", "rk_auth.menu", "menu")),
                Map.entry("role", List.of("rk_auth.rk_role", "rk_auth.role", "role")),
                Map.entry("search_document", List.of("rk_search.rk_search_index", "rk_search_index")),
                Map.entry("system_config", List.of("rk_user.rk_system_config", "rk_user.system_config", "system_config")),
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant")),
                Map.entry("news", List.of("rk_content.rk_news", "rk_content.news", "news")),
                Map.entry("file", List.of("rk_file.rk_file", "rk_file.file", "rk_file"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        return candidates.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
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
        String condition = filterConditionByExistingColumns(table, extraCondition);
        if (StringUtils.hasText(condition)) {
            where.append(where.length() == 0 ? " WHERE " : " AND ");
            where.append("(").append(condition).append(")");
        }
        return new QueryParts(where.toString(), args);
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (!StringUtils.hasText(condition)) {
            return null;
        }
        String normalized = condition;
        for (String token : List.of("is_deleted", "visible", "updated_at", "tenant_id", "is_enabled")) {
            if (normalized.contains(token) && !columnExists(table, token)) {
                return null;
            }
        }
        return normalized;
    }

    private String quoteColumn(String columnName) {
        return "`" + columnName.replace("`", "") + "`";
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
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

        private static TableRef parse(String value) {
            String[] parts = value.split("\\.", 2);
            if (parts.length == 2) {
                return new TableRef(parts[0], parts[1]);
            }
            return new TableRef(null, value);
        }

        private String qualifiedName() {
            if (!StringUtils.hasText(schema)) {
                return "`" + table + "`";
            }
            return "`" + schema + "`.`" + table + "`";
        }

        private String displayName() {
            return StringUtils.hasText(schema) ? schema + "." + table : table;
        }
    }

    private static class RequiredValueCheck {
        private long matchedCount;
        private final Set<String> missingValues = new LinkedHashSet<>();
    }

    @FunctionalInterface
    private interface HealthCheckSupplier {
        OpenSourceHealthVO.CheckItem get();
    }
}
