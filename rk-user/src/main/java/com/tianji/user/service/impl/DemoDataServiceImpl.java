package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.SystemConfig;
import com.tianji.user.domain.vo.DemoDataCenterVO;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.service.IDemoDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemoDataServiceImpl implements IDemoDataService {
    static final String CONFIG_KEY = "open.source.demo.data";
    static final String SAFE_TAG = "DEMO_OPEN_SOURCE";
    static final String FIELD_DEMO_BATCH_ID = "demoBatchId";
    static final String FIELD_DEMO_TENANT_CODE = "demoTenantCode";
    static final String DEMO_TENANT_CODE = "open-source-demo";
    private static final Long CONFIG_TENANT_ID = 1L;
    private static final String DEMO_EMAIL_DOMAIN = "open-source.local";
    private static final List<String> MODULES = List.of("tenant", "users", "club", "news", "activity", "competition");

    private final SystemConfigMapper systemConfigMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public DemoDataCenterVO getStatus() {
        return normalize(loadManifest());
    }

    @Override
    public DemoDataCenterVO generate() {
        cleanupDemoRows();
        DemoDataCenterVO vo = baseManifest("GENERATED")
                .setDemoBatchId("demo-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()))
                .setGeneratedAt(LocalDateTime.now());

        vo.getModuleStatuses().add(seedDemoTenant());
        vo.getModuleStatuses().add(seedDemoUsers());
        vo.getModuleStatuses().add(seedDemoClubMembers());
        vo.getModuleStatuses().add(seedDemoNews());
        vo.getModuleStatuses().add(seedDemoActivity());
        vo.getModuleStatuses().add(seedDemoCompetition());
        fillCountsAndSamples(vo);
        saveManifest(vo);
        return vo;
    }

    @Override
    public DemoDataCenterVO cleanup() {
        cleanupDemoRows();
        DemoDataCenterVO vo = normalize(loadManifest())
                .setStatus("CLEANED")
                .setCleanedAt(LocalDateTime.now())
                .setUpdatedAt(LocalDateTime.now());
        if (vo.getCounts() == null || vo.getCounts().isEmpty()) {
            vo.setCounts(defaultCounts(0));
        }
        saveManifest(vo);
        return vo;
    }

    @Override
    public DemoDataCenterVO reset() {
        cleanupDemoRows();
        DemoDataCenterVO vo = baseManifest("GENERATED")
                .setDemoBatchId("demo-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()))
                .setGeneratedAt(LocalDateTime.now());
        vo.getModuleStatuses().add(seedDemoTenant());
        vo.getModuleStatuses().add(seedDemoUsers());
        vo.getModuleStatuses().add(seedDemoClubMembers());
        vo.getModuleStatuses().add(seedDemoNews());
        vo.getModuleStatuses().add(seedDemoActivity());
        vo.getModuleStatuses().add(seedDemoCompetition());
        fillCountsAndSamples(vo);
        saveManifest(vo);
        return vo;
    }

    private DemoDataCenterVO loadManifest() {
        SystemConfig row = loadConfigRow();
        if (row == null || !StringUtils.hasText(row.getConfigValue())) {
            return baseManifest("NOT_GENERATED")
                    .setCounts(defaultCounts(0))
                    .setWarnings(List.of("尚未生成开源演示数据"));
        }
        try {
            return manifestMapper().readValue(row.getConfigValue(), DemoDataCenterVO.class);
        } catch (JsonProcessingException e) {
            return baseManifest("UNKNOWN")
                    .setCounts(defaultCounts(0))
                    .setWarnings(List.of("演示数据清单解析失败：" + e.getMessage()));
        }
    }

    private DemoDataCenterVO normalize(DemoDataCenterVO source) {
        DemoDataCenterVO vo = source == null ? baseManifest("NOT_GENERATED") : source;
        if (!StringUtils.hasText(vo.getStatus())) vo.setStatus("NOT_GENERATED");
        if (!StringUtils.hasText(vo.getSafeTag())) vo.setSafeTag(SAFE_TAG);
        if (!StringUtils.hasText(vo.getDemoTenantCode())) vo.setDemoTenantCode(DEMO_TENANT_CODE);
        if (vo.getTenantId() == null) vo.setTenantId(resolveTenantId());
        if (vo.getRepeatable() == null) vo.setRepeatable(true);
        if (vo.getCleanupSupported() == null) vo.setCleanupSupported(true);
        if (vo.getModules() == null || vo.getModules().isEmpty()) vo.setModules(new ArrayList<>(MODULES));
        if (vo.getCounts() == null) vo.setCounts(defaultCounts(0));
        if (vo.getSampleItems() == null) vo.setSampleItems(new ArrayList<>());
        if (vo.getModuleStatuses() == null) vo.setModuleStatuses(new ArrayList<>());
        if (vo.getWarnings() == null) vo.setWarnings(new ArrayList<>());
        return vo.setUpdatedAt(LocalDateTime.now());
    }

    private DemoDataCenterVO baseManifest(String status) {
        return new DemoDataCenterVO()
                .setStatus(status)
                .setSafeTag(SAFE_TAG)
                .setDemoTenantCode(DEMO_TENANT_CODE)
                .setTenantId(resolveTenantId())
                .setRepeatable(true)
                .setCleanupSupported(true)
                .setModules(new ArrayList<>(MODULES))
                .setCounts(defaultCounts(0))
                .setSampleItems(new ArrayList<>())
                .setModuleStatuses(new ArrayList<>())
                .setWarnings(new ArrayList<>())
                .setUpdatedAt(LocalDateTime.now());
    }

    private Map<String, Integer> defaultCounts(int value) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("demoTenants", value);
        counts.put("demoUsers", value);
        counts.put("demoClubMembers", value);
        counts.put("demoNews", value);
        counts.put("demoActivities", value);
        counts.put("demoCompetitions", value);
        return counts;
    }

    private void fillCountsAndSamples(DemoDataCenterVO vo) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("demoTenants", 1);
        counts.put("demoUsers", 3);
        counts.put("demoClubMembers", 3);
        counts.put("demoNews", 2);
        counts.put("demoActivities", 2);
        counts.put("demoCompetitions", 1);
        vo.setCounts(counts);
        vo.setSampleItems(List.of(
                sample("tenant", "开源演示租户", "独立演示租户，编码 " + DEMO_TENANT_CODE, "已规划", "rk_user.rk_tenant"),
                sample("users", "演示管理员/指导老师/学生", "邮箱均使用 @" + DEMO_EMAIL_DOMAIN + "，无真实个人信息", "脱敏", "rk_user.rk_user"),
                sample("club", "开源协作社团", "用于展示成员、社团台账和入社流程", "可重复执行", "rk_user.club_members"),
                sample("news", "黑河学院开源社团新闻样例", "用于前台新闻列表和搜索链路体验", SAFE_TAG, "rk_content.news"),
                sample("activity", "开源项目共创活动", "用于活动报名、状态和日历体验", SAFE_TAG, "rk_activity.rk_activity"),
                sample("competition", "校园开源作品挑战赛", "用于比赛发布和报名体验", SAFE_TAG, "rk_activity.competition_competitions")
        ));
    }

    private DemoDataCenterVO.SampleItem sample(String module, String title, String description, String status, String source) {
        return new DemoDataCenterVO.SampleItem()
                .setModule(module)
                .setTitle(title)
                .setDescription(description)
                .setStatus(status)
                .setSource(source);
    }

    private DemoDataCenterVO.ModuleStatus seedDemoTenant() {
        if (!tableExists("rk_user.rk_tenant") && !tableExists("rk_tenant")) {
            return module("tenant", "演示租户", "SKIPPED", 1, "租户表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_user.rk_tenant") ? "rk_user.rk_tenant" : "rk_tenant";
        try {
            int updated = jdbcTemplate.update("UPDATE " + table + " SET tenant_name=?, logo_url=?, description=?, contact_person=?, contact_phone=?, contact_email=?, status=1, expire_time=?, update_time=NOW(), is_deleted=0 WHERE tenant_code=?",
                    "黑河学院开源演示社团", "", SAFE_TAG + " 开源演示租户，可清理可重置", "演示管理员", "18800000000", "demo-admin@" + DEMO_EMAIL_DOMAIN, LocalDateTime.now().plusYears(3), DEMO_TENANT_CODE);
            if (updated == 0) {
                jdbcTemplate.update("INSERT INTO " + table + " (tenant_code, tenant_name, logo_url, display_order, description, contact_person, contact_phone, contact_email, status, expire_time, create_time, update_time, creator, updater, is_deleted) VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),1,1,0)",
                        DEMO_TENANT_CODE, "黑河学院开源演示社团", "", 999, SAFE_TAG + " 开源演示租户，可清理可重置", "演示管理员", "18800000000", "demo-admin@" + DEMO_EMAIL_DOMAIN, 1, LocalDateTime.now().plusYears(3));
            }
            return module("tenant", "演示租户", "PASS", 1, "演示租户已生成或恢复");
        } catch (RuntimeException e) {
            return module("tenant", "演示租户", "WARN", 1, "演示租户写入跳过：" + e.getMessage());
        }
    }

    private DemoDataCenterVO.ModuleStatus seedDemoUsers() {
        if (!tableExists("rk_user.rk_user") && !tableExists("rk_user")) {
            return module("users", "演示用户", "SKIPPED", 3, "用户表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_user.rk_user") ? "rk_user.rk_user" : "rk_user";
        try {
            seedUser(table, "demo_admin", "开源演示管理员", "demo-admin@" + DEMO_EMAIL_DOMAIN, "20260001", 3);
            seedUser(table, "demo_teacher", "开源演示指导老师", "demo-teacher@" + DEMO_EMAIL_DOMAIN, "20260002", 2);
            seedUser(table, "demo_student", "开源演示学生", "demo-student@" + DEMO_EMAIL_DOMAIN, "20260003", 1);
            return module("users", "演示用户", "PASS", 3, "演示用户已生成或恢复，邮箱均为脱敏域名");
        } catch (RuntimeException e) {
            return module("users", "演示用户", "WARN", 3, "演示用户写入跳过：" + e.getMessage());
        }
    }

    private void seedUser(String table, String username, String realName, String email, String studentId, int userType) {
        int updated = jdbcTemplate.update("UPDATE " + table + " SET tenant_id=COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?), real_name=?, nickname=?, email=?, student_id=?, college=?, major=?, grade=?, user_type=?, status=1, join_time=NOW(), update_time=NOW(), is_deleted=0 WHERE username=? OR email=?",
                DEMO_TENANT_CODE, resolveTenantId(), realName, realName, email, studentId, "黑河学院", "计算机科学与技术", "2026级", userType, username, email);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO " + table + " (tenant_id, username, password, real_name, nickname, avatar, gender, mobile, email, student_id, college, major, grade, user_type, status, join_time, create_time, update_time, creator, updater, is_deleted) VALUES (COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?),?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),NOW(),1,1,0)",
                    DEMO_TENANT_CODE, resolveTenantId(), username, "{noop}demo-not-for-login", realName, realName, "", 1, "18800000000", email, studentId, "黑河学院", "计算机科学与技术", "2026级", userType, 1);
        }
    }

    private DemoDataCenterVO.ModuleStatus seedDemoClubMembers() {
        if (!tableExists("rk_user.club_members") && !tableExists("club_members")) {
            return module("club", "演示社团成员", "SKIPPED", 3, "社团成员表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_user.club_members") ? "rk_user.club_members" : "club_members";
        try {
            seedClubMember(table, "开源演示管理员", "20260001", "demo-admin@" + DEMO_EMAIL_DOMAIN, "项目负责人");
            seedClubMember(table, "开源演示指导老师", "20260002", "demo-teacher@" + DEMO_EMAIL_DOMAIN, "指导老师");
            seedClubMember(table, "开源演示学生", "20260003", "demo-student@" + DEMO_EMAIL_DOMAIN, "成员");
            return module("club", "演示社团成员", "PASS", 3, "演示社团成员已生成或恢复");
        } catch (RuntimeException e) {
            return module("club", "演示社团成员", "WARN", 3, "演示社团成员写入跳过：" + e.getMessage());
        }
    }

    private void seedClubMember(String table, String name, String studentId, String email, String position) {
        int updated = jdbcTemplate.update("UPDATE " + table + " SET tenant_id=COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?), name=?, phone=?, major=?, grade=?, department=?, position=?, join_date=NOW(), status='ACTIVE', update_time=NOW(), is_deleted=0 WHERE email=? OR student_id=?",
                DEMO_TENANT_CODE, resolveTenantId(), name, "18800000000", "计算机科学与技术", "2026级", "开源协作社团", position, email, studentId);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO " + table + " (tenant_id, name, student_id, email, phone, major, grade, department, position, join_date, status, create_time, update_time, is_deleted) VALUES (COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?),?,?,?,?,?,?,?,?,?,NOW(),NOW(),NOW(),0)",
                    DEMO_TENANT_CODE, resolveTenantId(), name, studentId, email, "18800000000", "计算机科学与技术", "2026级", "开源协作社团", position, "ACTIVE");
        }
    }

    private DemoDataCenterVO.ModuleStatus seedDemoNews() {
        if (!tableExists("rk_content.news") && !tableExists("news")) {
            return module("news", "演示新闻", "SKIPPED", 2, "新闻表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_content.news") ? "rk_content.news" : "news";
        try {
            seedNews(table, "黑河学院开源社团管理系统演示数据发布", "用于开源体验的脱敏新闻样例");
            seedNews(table, "黑河学院学生社团开展开源共创交流", "用于前台新闻和搜索链路验证的样例");
            return module("news", "演示新闻", "PASS", 2, "演示新闻已生成或恢复");
        } catch (RuntimeException e) {
            return module("news", "演示新闻", "WARN", 2, "演示新闻写入跳过：" + e.getMessage());
        }
    }

    private void seedNews(String table, String title, String summary) {
        int updated = jdbcTemplate.update("UPDATE " + table + " SET summary=?, content=?, author=?, category=?, tags=?, is_published=1, approval_status=2, manager_review_status=1, teacher_review_status=1, is_featured=0, is_cross_tenant=0, view_count=0, publish_time=NOW(), update_time=NOW(), is_deleted=0 WHERE title=? AND tags LIKE ?",
                summary, summary + "。数据标记：" + SAFE_TAG + "，可在演示数据中心一键清理。", "开源演示", "社团动态", SAFE_TAG + ",黑河学院,开源演示", title, title, "%" + SAFE_TAG + "%");
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO " + table + " (tenant_id, title, summary, content, cover_image, author, category, tags, is_published, manager_review_status, teacher_review_status, approval_status, approver, approval_time, is_featured, is_cross_tenant, view_count, publish_time, created_by, create_time, update_time, is_deleted) VALUES (COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?),?,?,?,?,?,?,?,?,?,?,?,'system',NOW(),?,?,0,NOW(),?,NOW(),NOW(),0)",
                    DEMO_TENANT_CODE, resolveTenantId(), title, summary, summary + "。数据标记：" + SAFE_TAG + "，可在演示数据中心一键清理。", "", "开源演示", "社团动态", SAFE_TAG + ",黑河学院,开源演示", 1, 1, 1, 2, 0, 0, "demo-data-center");
        }
    }

    private DemoDataCenterVO.ModuleStatus seedDemoActivity() {
        if (!tableExists("rk_activity.rk_activity") && !tableExists("rk_activity")) {
            return module("activity", "演示活动", "SKIPPED", 2, "活动表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_activity.rk_activity") ? "rk_activity.rk_activity" : "rk_activity";
        try {
            seedActivity(table, "DEMO_OPEN_SOURCE_ACTIVITY_1", "黑河学院开源项目共创活动", 2);
            seedActivity(table, "DEMO_OPEN_SOURCE_ACTIVITY_2", "黑河学院社团系统体验工作坊", 1);
            return module("activity", "演示活动", "PASS", 2, "演示活动已生成或恢复");
        } catch (RuntimeException e) {
            return module("activity", "演示活动", "WARN", 2, "演示活动写入跳过：" + e.getMessage());
        }
    }

    private void seedActivity(String table, String code, String name, int status) {
        int updated = jdbcTemplate.update("UPDATE " + table + " SET activity_name=?, organizer=?, location=?, max_participants=80, current_participants=0, activity_status=?, start_time=?, end_time=?, registration_start_time=NOW(), registration_end_time=?, content=?, requirements=?, points=2, is_top=0, is_hot=1, is_cross_tenant=0, view_count=0, manager_review_status=1, teacher_review_status=1, update_time=NOW(), is_deleted=0 WHERE activity_code=?",
                name, "黑河学院开源协作社团", "黑河学院创新创业中心", status, LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2), LocalDateTime.now().plusDays(6), SAFE_TAG + " 演示活动内容，可一键清理。", "面向开源体验的脱敏活动样例", code);
        if (updated == 0) {
            jdbcTemplate.update("INSERT INTO " + table + " (tenant_id, activity_name, activity_code, category_id, cover_image, activity_type, organizer, location, max_participants, current_participants, activity_status, start_time, end_time, registration_start_time, registration_end_time, content, requirements, points, is_top, is_hot, is_cross_tenant, view_count, manager_review_status, teacher_review_status, create_time, update_time, creator, updater, is_deleted) VALUES (COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),1,1,0)",
                    DEMO_TENANT_CODE, resolveTenantId(), name, code, 1L, "", 1, "黑河学院开源协作社团", "黑河学院创新创业中心", 80, 0, status, LocalDateTime.now().plusDays(7), LocalDateTime.now().plusDays(7).plusHours(2), LocalDateTime.now(), LocalDateTime.now().plusDays(6), SAFE_TAG + " 演示活动内容，可一键清理。", "面向开源体验的脱敏活动样例", 2, 0, 1, 0, 0, 1, 1);
        }
    }

    private DemoDataCenterVO.ModuleStatus seedDemoCompetition() {
        if (!tableExists("rk_activity.competition_competitions") && !tableExists("competition_competitions")) {
            return module("competition", "演示比赛", "SKIPPED", 1, "比赛表不可访问，已仅写入演示清单");
        }
        String table = tableExists("rk_activity.competition_competitions") ? "rk_activity.competition_competitions" : "competition_competitions";
        try {
            int updated = jdbcTemplate.update("UPDATE " + table + " SET subtitle=?, description=?, content=?, organizer=?, competition_type=?, level=?, max_participants=120, registration_start=NOW(), registration_end=?, competition_start=?, competition_end=?, location=?, status='UPCOMING', summary=?, awards=?, participation_points=2, first_prize_points=8, second_prize_points=5, third_prize_points=3, contact_person=?, contact_phone=?, contact_email=?, tags=?, priority=10, is_published=1, is_featured=1, is_cross_tenant=0, view_count=0, registration_count=0, manager_review_status=1, teacher_review_status=1, update_time=NOW(), updated_by=? , is_deleted=0 WHERE title=? AND tags LIKE ?",
                    "开源演示比赛", SAFE_TAG + " 校园开源作品挑战赛", "围绕黑河学院社团管理系统开源体验设计的脱敏比赛样例。", "黑河学院开源协作社团", "PROGRAMMING", "SCHOOL", LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(14), LocalDateTime.now().plusDays(14).plusHours(4), "黑河学院创新创业中心", "提交开源作品或体验报告", "一等奖、二等奖、三等奖", "演示管理员", "18800000000", "demo-admin@" + DEMO_EMAIL_DOMAIN, SAFE_TAG + ",黑河学院,开源演示", "demo-data-center", "黑河学院校园开源作品挑战赛", "%" + SAFE_TAG + "%");
            if (updated == 0) {
                jdbcTemplate.update("INSERT INTO " + table + " (tenant_id, title, subtitle, description, content, organizer, competition_type, level, max_participants, registration_start, registration_end, competition_start, competition_end, location, status, summary, awards, participation_points, first_prize_points, second_prize_points, third_prize_points, excellent_prize_points, contact_person, contact_phone, contact_email, tags, priority, is_published, is_featured, is_cross_tenant, view_count, registration_count, manager_review_status, teacher_review_status, create_time, update_time, created_by, updated_by, is_deleted) VALUES (COALESCE((SELECT id FROM rk_user.rk_tenant WHERE tenant_code=? LIMIT 1),?),?,?,?,?,?,?,?,?,NOW(),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),?,?,0)",
                        DEMO_TENANT_CODE, resolveTenantId(), "黑河学院校园开源作品挑战赛", "开源演示比赛", SAFE_TAG + " 校园开源作品挑战赛", "围绕黑河学院社团管理系统开源体验设计的脱敏比赛样例。", "黑河学院开源协作社团", "PROGRAMMING", "SCHOOL", 120, LocalDateTime.now().plusDays(10), LocalDateTime.now().plusDays(14), LocalDateTime.now().plusDays(14).plusHours(4), "黑河学院创新创业中心", "UPCOMING", "提交开源作品或体验报告", "一等奖、二等奖、三等奖", 2, 8, 5, 3, 1, "演示管理员", "18800000000", "demo-admin@" + DEMO_EMAIL_DOMAIN, SAFE_TAG + ",黑河学院,开源演示", 10, true, true, false, 0, 0, 1, 1, "demo-data-center", "demo-data-center");
            }
            return module("competition", "演示比赛", "PASS", 1, "演示比赛已生成或恢复");
        } catch (RuntimeException e) {
            return module("competition", "演示比赛", "WARN", 1, "演示比赛写入跳过：" + e.getMessage());
        }
    }

    void cleanupDemoRows() {
        safeLogicalCleanup("rk_content.news", "UPDATE rk_content.news SET is_deleted=1, update_time=NOW() WHERE tags LIKE ? OR created_by=? OR title LIKE ?", "%" + SAFE_TAG + "%", "demo-data-center", "黑河学院%开源%");
        safeLogicalCleanup("news", "UPDATE news SET is_deleted=1, update_time=NOW() WHERE tags LIKE ? OR created_by=? OR title LIKE ?", "%" + SAFE_TAG + "%", "demo-data-center", "黑河学院%开源%");
        safeLogicalCleanup("rk_activity.rk_activity", "UPDATE rk_activity.rk_activity SET is_deleted=1, update_time=NOW() WHERE activity_code LIKE ? OR content LIKE ?", SAFE_TAG + "%", "%" + SAFE_TAG + "%");
        safeLogicalCleanup("rk_activity", "UPDATE rk_activity SET is_deleted=1, update_time=NOW() WHERE activity_code LIKE ? OR content LIKE ?", SAFE_TAG + "%", "%" + SAFE_TAG + "%");
        safeLogicalCleanup("rk_activity.competition_competitions", "UPDATE rk_activity.competition_competitions SET is_deleted=1, update_time=NOW() WHERE tags LIKE ? OR created_by=? OR title LIKE ?", "%" + SAFE_TAG + "%", "demo-data-center", "黑河学院%开源%");
        safeLogicalCleanup("competition_competitions", "UPDATE competition_competitions SET is_deleted=1, update_time=NOW() WHERE tags LIKE ? OR created_by=? OR title LIKE ?", "%" + SAFE_TAG + "%", "demo-data-center", "黑河学院%开源%");
        safeLogicalCleanup("rk_user.club_members", "UPDATE rk_user.club_members SET is_deleted=1, update_time=NOW() WHERE email LIKE ? OR name LIKE ?", "%@" + DEMO_EMAIL_DOMAIN, "开源演示%");
        safeLogicalCleanup("club_members", "UPDATE club_members SET is_deleted=1, update_time=NOW() WHERE email LIKE ? OR name LIKE ?", "%@" + DEMO_EMAIL_DOMAIN, "开源演示%");
        safeLogicalCleanup("rk_user.rk_user", "UPDATE rk_user.rk_user SET is_deleted=1, update_time=NOW() WHERE email LIKE ? OR username LIKE ?", "%@" + DEMO_EMAIL_DOMAIN, "demo_%");
        safeLogicalCleanup("rk_user", "UPDATE rk_user SET is_deleted=1, update_time=NOW() WHERE email LIKE ? OR username LIKE ?", "%@" + DEMO_EMAIL_DOMAIN, "demo_%");
        safeLogicalCleanup("rk_user.rk_tenant", "UPDATE rk_user.rk_tenant SET status=0, is_deleted=1, update_time=NOW() WHERE tenant_code=? AND description LIKE ?", DEMO_TENANT_CODE, "%" + SAFE_TAG + "%");
        safeLogicalCleanup("rk_tenant", "UPDATE rk_tenant SET status=0, is_deleted=1, update_time=NOW() WHERE tenant_code=? AND description LIKE ?", DEMO_TENANT_CODE, "%" + SAFE_TAG + "%");
    }

    private void safeLogicalCleanup(String table, String sql, Object... args) {
        if (!tableExists(table)) {
            return;
        }
        try {
            jdbcTemplate.update(sql, args);
        } catch (RuntimeException ignored) {
            // Cleanup must never remove real data because of a schema mismatch; unknown tables are skipped.
        }
    }

    private DemoDataCenterVO.ModuleStatus module(String module, String title, String status, int count, String message) {
        return new DemoDataCenterVO.ModuleStatus()
                .setModule(module)
                .setTitle(title)
                .setStatus(status)
                .setCount(count)
                .setMessage(message);
    }

    private boolean tableExists(String tableName) {
        String schema = null;
        String table = tableName;
        int dot = tableName.indexOf('.');
        if (dot > 0) {
            schema = tableName.substring(0, dot);
            table = tableName.substring(dot + 1);
        }
        try {
            Integer count;
            if (schema == null) {
                count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                        Integer.class,
                        table);
            } else {
                count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(1) FROM information_schema.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?",
                        Integer.class,
                        schema,
                        table);
            }
            return count != null && count > 0;
        } catch (DataAccessException e) {
            return false;
        }
    }

    private SystemConfig loadConfigRow() {
        return systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getTenantId, CONFIG_TENANT_ID)
                .eq(SystemConfig::getConfigKey, CONFIG_KEY)
                .last("LIMIT 1"));
    }

    private void saveManifest(DemoDataCenterVO vo) {
        DemoDataCenterVO normalized = normalize(vo);
        SystemConfig row = loadConfigRow();
        if (row == null) {
            row = new SystemConfig()
                    .setTenantId(CONFIG_TENANT_ID)
                    .setConfigKey(CONFIG_KEY)
                    .setDescription("open source demo data manifest")
                    .setIsEnabled(true)
                    .setIsDeleted(false)
                    .setCreateTime(LocalDateTime.now());
        }
        row.setConfigValue(toJson(normalized));
        row.setUpdateTime(LocalDateTime.now());
        if (row.getId() == null) {
            systemConfigMapper.insert(row);
        } else {
            systemConfigMapper.updateById(row);
        }
    }

    private String toJson(DemoDataCenterVO vo) {
        try {
            return manifestMapper().writeValueAsString(vo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("demo data manifest serialize failed", e);
        }
    }

    private ObjectMapper manifestMapper() {
        return objectMapper.copy()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? CONFIG_TENANT_ID : tenantId;
    }
}
