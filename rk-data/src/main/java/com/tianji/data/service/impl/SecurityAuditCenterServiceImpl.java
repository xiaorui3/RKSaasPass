package com.tianji.data.service.impl;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.TenantContext;
import com.tianji.data.model.dto.SecurityAuditNotifyRequestDTO;
import com.tianji.data.model.vo.SecurityAuditCenterVO;
import com.tianji.data.service.SecurityAuditCenterService;
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

import static com.tianji.data.constants.RedisConstants.KEY_SECURITY_AUDIT_CENTER;

@Slf4j
@Service
public class SecurityAuditCenterServiceImpl implements SecurityAuditCenterService {
    private static final int CACHE_TTL_SECONDS = 180;
    private static final int CACHE_TTL_JITTER_SECONDS = 90;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public SecurityAuditCenterServiceImpl(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public SecurityAuditCenterVO getSecurityAuditCenter(Long tenantId, String riskType, String riskLevel, String notifyStatus) {
        Long scopedTenantId = resolveTenantId(tenantId);
        String tenantScope = scopedTenantId == null ? "ALL" : String.valueOf(scopedTenantId);
        String normalizedRiskType = normalizeFilter(riskType);
        String normalizedRiskLevel = normalizeFilter(riskLevel);
        String normalizedNotifyStatus = normalizeFilter(notifyStatus);
        String cacheKey = KEY_SECURITY_AUDIT_CENTER + tenantScope + ":" + normalizedRiskType + ":" + normalizedRiskLevel + ":" + normalizedNotifyStatus;

        SecurityAuditCenterVO cached = readCache(cacheKey);
        if (cached != null) {
            cached.getCache()
                    .setHit(true)
                    .setMode("redis")
                    .setKey(cacheKey)
                    .setTtlSeconds(CACHE_TTL_SECONDS);
            return cached;
        }

        SecurityAuditCenterVO computed = compute(scopedTenantId, tenantScope, normalizedRiskType, normalizedRiskLevel, normalizedNotifyStatus);
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
    public SecurityAuditCenterVO.NotifyLog notifyRisk(SecurityAuditNotifyRequestDTO request) {
        SecurityAuditNotifyRequestDTO safeRequest = request == null ? new SecurityAuditNotifyRequestDTO() : request;
        Long tenantId = resolveTenantId(safeRequest.getTenantId());
        String riskType = normalizeRiskType(safeRequest.getRiskType());
        String riskLevel = normalizeFilter(safeRequest.getRiskLevel());
        String audience = blankToDefault(safeRequest.getAudience(), "tenant-admin");
        String reason = blankToDefault(safeRequest.getReason(), "manual notify");
        String operator = blankToDefault(safeRequest.getOperator(), "system");

        SecurityAuditCenterVO.NotifyLog logEntry = new SecurityAuditCenterVO.NotifyLog()
                .setTenantId(tenantId)
                .setRiskType(riskType)
                .setRiskLevel(riskLevel)
                .setAudience(audience)
                .setReason(reason)
                .setCreatedAt(LocalDateTime.now());

        try {
            ensureNotifyLogTable();
            jdbcTemplate.update(
                    "INSERT INTO security_audit_notify_log (tenant_id, risk_type, risk_level, status, audience, reason, message, operator, create_time) VALUES (?,?,?,?,?,?,?,?,NOW())",
                    tenantId,
                    riskType,
                    riskLevel,
                    "REGISTERED",
                    audience,
                    reason,
                    "高风险安全审计已登记，后续可接通知服务闭环",
                    operator
            );
            logEntry
                    .setStatus("REGISTERED")
                    .setMessage("高风险通知已登记，未直接跨服务发送，保留审计闭环");
        } catch (RuntimeException e) {
            logEntry
                    .setStatus("FAILED")
                    .setMessage(e.getMessage());
        }
        return logEntry;
    }

    @Override
    public List<SecurityAuditCenterVO> warmupAllTenants() {
        List<SecurityAuditCenterVO> warmed = new ArrayList<>();
        for (Long tenantId : listTenantIds()) {
            if (tenantId == null || tenantId <= 0) {
                continue;
            }
            SecurityAuditCenterVO computed = compute(tenantId, String.valueOf(tenantId), "ALL", "ALL", "ALL");
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

    private SecurityAuditCenterVO compute(Long tenantId, String tenantScope, String riskType, String riskLevel, String notifyStatus) {
        SecurityAuditCenterVO vo = new SecurityAuditCenterVO()
                .setTenantId(tenantId)
                .setTenantScope(tenantScope)
                .setUpdatedAt(LocalDateTime.now());
        vo.getFilters()
                .setRiskType(riskType)
                .setRiskLevel(riskLevel)
                .setNotifyStatus(notifyStatus);

        List<SecurityAuditCenterVO.RiskItem> risks = new ArrayList<>();
        risks.addAll(crossTenantRisks(vo, tenantId));
        risks.addAll(highRiskOperationRisks(vo, tenantId));
        risks.addAll(abnormalLoginRisks(vo, tenantId));
        risks.addAll(permissionChangeRisks(vo, tenantId));
        if (risks.isEmpty()) {
            risks.add(summaryRisk("normal", "安全检查通过", "当前未发现高优先级安全风险", "LOW", "NORMAL", "rk_data.aggregate", false, "NORMAL"));
        }
        vo.setRisks(risks.stream()
                .filter(risk -> filterRisk(risk, riskType, riskLevel, notifyStatus))
                .limit(200)
                .collect(Collectors.toList()));
        vo.setNotifyLogs(recentNotifyLogs(tenantId));
        fillSummary(vo);
        return vo;
    }

    private List<SecurityAuditCenterVO.RiskItem> crossTenantRisks(SecurityAuditCenterVO vo, Long tenantId) {
        List<SecurityAuditCenterVO.RiskItem> items = new ArrayList<>();
        long opCount = countAny(vo, "security.cross_tenant_operation", "operation_log", "oper_name LIKE '%tenant%' OR oper_name LIKE '%租户%'", tenantId);
        if (opCount > 0) {
            items.add(summaryRisk("cross_tenant", "跨租户访问", "存在可能跨租户访问尝试：" + opCount + " 条", "HIGH", "NEED_REVIEW", "rk_user.sys_oper_log", true, "PENDING"));
        }
        return items;
    }

    private List<SecurityAuditCenterVO.RiskItem> highRiskOperationRisks(SecurityAuditCenterVO vo, Long tenantId) {
        TableRef table = resolveTable("operation_log");
        if (table == null) {
            addSource(vo, "security.high_operation", "sys_oper_log", "missing");
            return List.of(summaryRisk("high_risk_operation", "高风险后台操作", "操作日志来源表缺失", "LOW", "SOURCE_MISSING", "rk_user.sys_oper_log", false, "UNKNOWN"));
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, "business_type IN (3,4,5,8,9)"), tenantId);
        List<String> columns = existingColumns(table, List.of("id", "title", "business_type", "oper_name", "oper_url", "oper_time", "create_time", "tenant_id"));
        if (columns.isEmpty()) {
            addSource(vo, "security.high_operation", table.displayName(), "missing-columns");
            return List.of(summaryRisk("high_risk_operation", "高风险后台操作", "高风险操作缺少可展示字段", "LOW", "MISSING_COLUMNS", table.displayName(), false, "UNKNOWN"));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName() + where.sql + orderByExistingTime(table) + " LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "security.high_operation", table.displayName(), "ok");
            return rows.stream()
                    .map(row -> new SecurityAuditCenterVO.RiskItem()
                            .setRiskType("high_risk_operation")
                            .setRiskTypeLabel("高风险后台操作")
                            .setTitle(blankToDefault(firstText(row, "title", "oper_name", "id"), "高风险操作"))
                            .setDescription("后台高风险操作需要审计确认")
                            .setRiskLevel("HIGH")
                            .setStatus("NEED_REVIEW")
                            .setSource(table.displayName())
                            .setSourceId(firstText(row, "id"))
                            .setSourcePath("/admin/operation/logs")
                            .setMenuName("日志管理")
                            .setCreatedAt(formatRowTime(row, "oper_time", "create_time"))
                            .setUpdatedAt(formatRowTime(row, "oper_time", "create_time"))
                            .setNotifyable(true)
                            .setMenuPermissionLinked(true)
                            .setNotifyStatus("PENDING"))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "security.high_operation", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryRisk("high_risk_operation", "高风险后台操作", "高风险操作明细暂不可用", "MEDIUM", "FALLBACK", table.displayName(), false, "UNKNOWN"));
        }
    }

    private List<SecurityAuditCenterVO.RiskItem> abnormalLoginRisks(SecurityAuditCenterVO vo, Long tenantId) {
        TableRef table = resolveTable("login_log");
        if (table == null) {
            addSource(vo, "security.abnormal_login", "sys_logininfor", "missing");
            return List.of(summaryRisk("abnormal_login", "异常登录位置", "登录日志来源表缺失", "LOW", "SOURCE_MISSING", "rk_user.sys_logininfor", false, "UNKNOWN"));
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, "status IN (1,'FAIL','fail')"), tenantId);
        List<String> columns = existingColumns(table, List.of("id", "login_name", "ipaddr", "login_location", "status", "login_time", "create_time", "tenant_id"));
        if (columns.isEmpty()) {
            addSource(vo, "security.abnormal_login", table.displayName(), "missing-columns");
            return List.of(summaryRisk("abnormal_login", "异常登录位置", "登录日志缺少可展示字段", "LOW", "MISSING_COLUMNS", table.displayName(), false, "UNKNOWN"));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName() + where.sql + orderByExistingTime(table) + " LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "security.abnormal_login", table.displayName(), "ok");
            return rows.stream()
                    .map(row -> new SecurityAuditCenterVO.RiskItem()
                            .setRiskType("abnormal_login")
                            .setRiskTypeLabel("异常登录位置")
                            .setTitle(blankToDefault(firstText(row, "login_name", "id"), "异常登录"))
                            .setDescription("异常登录位置需要复核")
                            .setRiskLevel("MEDIUM")
                            .setStatus("NEED_REVIEW")
                            .setSource(table.displayName())
                            .setSourceId(firstText(row, "id"))
                            .setSourcePath("/admin/operation/logs")
                            .setActorName(firstText(row, "login_name"))
                            .setLoginLocation(firstText(row, "login_location", "ipaddr"))
                            .setCreatedAt(formatRowTime(row, "login_time", "create_time"))
                            .setUpdatedAt(formatRowTime(row, "login_time", "create_time"))
                            .setNotifyable(true)
                            .setMenuPermissionLinked(false)
                            .setNotifyStatus("PENDING"))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "security.abnormal_login", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryRisk("abnormal_login", "异常登录位置", "异常登录明细暂不可用", "MEDIUM", "FALLBACK", table.displayName(), false, "UNKNOWN"));
        }
    }

    private List<SecurityAuditCenterVO.RiskItem> permissionChangeRisks(SecurityAuditCenterVO vo, Long tenantId) {
        TableRef table = resolveTable("operation_log");
        if (table == null) {
            addSource(vo, "security.permission_change", "sys_oper_log", "missing");
            return List.of(summaryRisk("permission_change", "权限变更记录", "权限变更来源表缺失", "LOW", "SOURCE_MISSING", "rk_user.sys_oper_log", false, "UNKNOWN"));
        }
        QueryParts where = buildWhere(table, filterConditionByExistingColumns(table, "title LIKE '%role%' OR title LIKE '%menu%'"), tenantId);
        List<String> columns = existingColumns(table, List.of("id", "title", "oper_name", "oper_url", "oper_time", "create_time", "tenant_id"));
        if (columns.isEmpty()) {
            addSource(vo, "security.permission_change", table.displayName(), "missing-columns");
            return List.of(summaryRisk("permission_change", "权限变更记录", "权限变更缺少可展示字段", "LOW", "MISSING_COLUMNS", table.displayName(), false, "UNKNOWN"));
        }
        String sql = "SELECT " + selectColumns(columns) + " FROM " + table.qualifiedName() + where.sql + orderByExistingTime(table) + " LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            addSource(vo, "security.permission_change", table.displayName(), "ok");
            return rows.stream()
                    .map(row -> new SecurityAuditCenterVO.RiskItem()
                            .setRiskType("permission_change")
                            .setRiskTypeLabel("权限变更记录")
                            .setTitle(blankToDefault(firstText(row, "title", "oper_name", "id"), "权限变更"))
                            .setDescription("角色或菜单权限变更需要审计确认")
                            .setRiskLevel("MEDIUM")
                            .setStatus("NEED_REVIEW")
                            .setSource(table.displayName())
                            .setSourceId(firstText(row, "id"))
                            .setSourcePath("/admin/system/roles")
                            .setMenuName("角色管理")
                            .setCreatedAt(formatRowTime(row, "oper_time", "create_time"))
                            .setUpdatedAt(formatRowTime(row, "oper_time", "create_time"))
                            .setNotifyable(true)
                            .setMenuPermissionLinked(true)
                            .setNotifyStatus("PENDING"))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            addSource(vo, "security.permission_change", table.displayName(), "fallback:" + e.getClass().getSimpleName());
            return List.of(summaryRisk("permission_change", "权限变更记录", "权限变更明细暂不可用", "MEDIUM", "FALLBACK", table.displayName(), false, "UNKNOWN"));
        }
    }

    private void fillSummary(SecurityAuditCenterVO vo) {
        SecurityAuditCenterVO.Summary summary = vo.getSummary();
        summary.setTotalRiskCount(vo.getRisks().stream().filter(risk -> !"normal".equals(risk.getRiskType())).count());
        summary.setCrossTenantCount(countRisks(vo, "cross_tenant"));
        summary.setHighRiskOperationCount(countRisks(vo, "high_risk_operation"));
        summary.setAbnormalLoginCount(countRisks(vo, "abnormal_login"));
        summary.setPermissionChangeCount(countRisks(vo, "permission_change"));
        summary.setNotifyPendingCount(vo.getRisks().stream().filter(SecurityAuditCenterVO.RiskItem::isNotifyable).filter(risk -> "PENDING".equalsIgnoreCase(risk.getNotifyStatus())).count());
        summary.setNotifySentCount(vo.getNotifyLogs().stream().filter(log -> "REGISTERED".equalsIgnoreCase(log.getStatus())).count());
    }

    private long countRisks(SecurityAuditCenterVO vo, String riskType) {
        return vo.getRisks().stream().filter(risk -> riskType.equals(risk.getRiskType())).count();
    }

    private boolean filterRisk(SecurityAuditCenterVO.RiskItem risk, String riskType, String riskLevel, String notifyStatus) {
        if (!"ALL".equals(riskType) && !riskType.equalsIgnoreCase(risk.getRiskType())) {
            return false;
        }
        if (!"ALL".equals(riskLevel) && !riskLevel.equalsIgnoreCase(risk.getRiskLevel())) {
            return false;
        }
        return "ALL".equals(notifyStatus) || notifyStatus.equalsIgnoreCase(blankToDefault(risk.getNotifyStatus(), "UNKNOWN"));
    }

    private SecurityAuditCenterVO.RiskItem summaryRisk(String riskType, String label, String description, String riskLevel, String status, String source, boolean notifyable, String notifyStatus) {
        return new SecurityAuditCenterVO.RiskItem()
                .setRiskType(riskType)
                .setRiskTypeLabel(label)
                .setTitle(label)
                .setDescription(description)
                .setRiskLevel(riskLevel)
                .setStatus(status)
                .setSource(source)
                .setNotifyable(notifyable)
                .setMenuPermissionLinked("permission_change".equals(riskType) || "high_risk_operation".equals(riskType))
                .setNotifyStatus(notifyStatus)
                .setCreatedAt(formatNow())
                .setUpdatedAt(formatNow())
                .setSourcePath("/admin/operation/logs");
    }

    private List<SecurityAuditCenterVO.NotifyLog> recentNotifyLogs(Long tenantId) {
        ensureNotifyLogTable();
        QueryParts where = buildWhere(TableRef.parse("security_audit_notify_log"), null, tenantId);
        String sql = "SELECT tenant_id, risk_type, risk_level, status, audience, reason, message, create_time FROM security_audit_notify_log"
                + where.sql
                + " ORDER BY id DESC LIMIT 20";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, where.args.toArray());
            return rows.stream().map(row -> new SecurityAuditCenterVO.NotifyLog()
                    .setTenantId(longValue(row.get("tenant_id")))
                    .setRiskType(firstText(row, "risk_type"))
                    .setRiskLevel(firstText(row, "risk_level"))
                    .setStatus(firstText(row, "status"))
                    .setAudience(firstText(row, "audience"))
                    .setReason(firstText(row, "reason"))
                    .setMessage(firstText(row, "message"))
                    .setCreatedAt(LocalDateTime.now()))
                    .collect(Collectors.toList());
        } catch (RuntimeException e) {
            return List.of();
        }
    }

    private void ensureNotifyLogTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS security_audit_notify_log (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                    "tenant_id BIGINT DEFAULT NULL," +
                    "risk_type VARCHAR(64) NOT NULL," +
                    "risk_level VARCHAR(32) NOT NULL," +
                    "status VARCHAR(32) NOT NULL," +
                    "audience VARCHAR(64) DEFAULT NULL," +
                    "reason VARCHAR(500) DEFAULT NULL," +
                    "message VARCHAR(1000) DEFAULT NULL," +
                    "operator VARCHAR(128) DEFAULT NULL," +
                    "create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                    "KEY idx_security_notify_tenant (tenant_id)," +
                    "KEY idx_security_notify_type (risk_type)" +
                    ")");
        } catch (RuntimeException e) {
            log.warn("ensure security_audit_notify_log failed: {}", e.getMessage());
        }
    }

    private List<Long> listTenantIds() {
        try {
            if (!tableExists(TableRef.parse("rk_tenant")) && !tableExists(TableRef.parse("tenant"))) {
                return List.of(resolveTenantId(null));
            }
            String table = tableExists(TableRef.parse("rk_tenant")) ? "rk_tenant" : "tenant";
            String sql = "SELECT id FROM " + table + " ORDER BY id";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            List<Long> ids = rows.stream()
                    .map(row -> longValue(row.get("id")))
                    .filter(id -> id != null && id > 0)
                    .collect(Collectors.toList());
            return ids.isEmpty() ? List.of(resolveTenantId(null)) : ids;
        } catch (RuntimeException e) {
            return List.of(resolveTenantId(null));
        }
    }

    private String cacheKey(String tenantScope, String riskType, String riskLevel, String notifyStatus) {
        return KEY_SECURITY_AUDIT_CENTER + tenantScope + ":" + riskType + ":" + riskLevel + ":" + notifyStatus;
    }

    private SecurityAuditCenterVO readCache(String cacheKey) {
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json == null || json.isBlank()) {
                return null;
            }
            return JsonUtils.toBean(json, SecurityAuditCenterVO.class);
        } catch (RuntimeException e) {
            log.warn("read security audit cache failed: {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String cacheKey, SecurityAuditCenterVO data) {
        try {
            int ttl = CACHE_TTL_SECONDS + ThreadLocalRandom.current().nextInt(CACHE_TTL_JITTER_SECONDS + 1);
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJsonStr(data), ttl, TimeUnit.SECONDS);
            data.getCache().setTtlSeconds(ttl);
        } catch (RuntimeException e) {
            data.getCache().setMode("fallback");
            log.warn("write security audit cache failed: {}", e.getMessage());
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

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return "ALL";
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeRiskType(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
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

    private String formatRowTime(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            Object value = row.get(key);
            if (value instanceof LocalDateTime) {
                return DATE_TIME_FORMATTER.format((LocalDateTime) value);
            }
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return formatNow();
    }

    private void addSource(SecurityAuditCenterVO vo, String metric, String tableName, String status) {
        vo.getSources().add(new SecurityAuditCenterVO.SourceInfo()
                .setMetric(metric)
                .setTableName(tableName)
                .setStatus(status.toLowerCase(Locale.ROOT))
                .setUpdatedAt(LocalDateTime.now()));
    }

    private long countAny(SecurityAuditCenterVO vo, String metric, String baseTable, String extraCondition, Long tenantId) {
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
                Map.entry("operation_log", List.of("rk_user.sys_oper_log", "sys_oper_log")),
                Map.entry("login_log", List.of("rk_user.sys_logininfor", "sys_logininfor")),
                Map.entry("role_menu", List.of("rk_auth.role_menu", "role_menu")),
                Map.entry("menu", List.of("rk_auth.menu", "menu")),
                Map.entry("role", List.of("rk_auth.role", "role")),
                Map.entry("tenant", List.of("rk_user.rk_tenant", "rk_tenant"))
        );
        List<String> candidates = new ArrayList<>(mapped.getOrDefault(baseTable, List.of()));
        candidates.add(baseTable);
        candidates.add("rk_" + baseTable);
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
        List<String> existing = List.of("update_time", "updated_at", "create_time", "created_at").stream()
                .filter(column -> columnExists(table, column))
                .collect(Collectors.toList());
        if (existing.isEmpty()) {
            return "";
        }
        return " ORDER BY " + existing.stream().distinct().map(column -> "`" + column + "` DESC").collect(Collectors.joining(", "));
    }

    private String filterConditionByExistingColumns(TableRef table, String condition) {
        if (condition == null || condition.isEmpty()) {
            return null;
        }
        String lower = condition.toLowerCase(Locale.ROOT);
        for (String column : List.of("tenant_id", "oper_name", "title", "status", "business_type", "login_name", "login_location", "ipaddr", "login_time", "create_time")) {
            if (lower.contains(column.toLowerCase(Locale.ROOT)) && !columnExists(table, column)) {
                return null;
            }
        }
        return condition.trim();
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
