package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.domain.po.RegisterReviewRequest;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.JoinRequestMapper;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.SystemConfigMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.service.IEmailVerificationService;
import com.tianji.user.service.IReferralCodeService;
import com.tianji.user.service.ITenantWorkflowConfigService;
import com.tianji.user.service.IUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplTenantScopeTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private com.tianji.api.client.auth.AuthClient authClient;
    @Mock
    private IEmailCenterService emailCenterService;
    @Mock
    private IEmailVerificationService emailVerificationService;
    @Mock
    private IReferralCodeService referralCodeService;
    @Mock
    private IUserService userService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private JoinRequestMapper joinRequestMapper;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private ReferralCodeMapper referralCodeMapper;
    @Mock
    private com.tianji.message.api.client.EmailTemplateClient emailTemplateClient;
    @Mock
    private ReviewActionTokenServiceImpl reviewActionTokenService;
    @Mock
    private RegisterReviewRequestMapper registerReviewRequestMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ITenantWorkflowConfigService tenantWorkflowConfigService;
    @Mock
    private SystemConfigMapper systemConfigMapper;
    @Mock
    private ObjectMapper objectMapper;

    private AdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), JoinRequest.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RegisterReviewRequest.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        service = new AdmissionServiceImpl(
                redisTemplate,
                passwordEncoder,
                authClient,
                emailCenterService,
                emailVerificationService,
                referralCodeService,
                userService,
                userMapper,
                tenantMapper,
                referralCodeMapper,
                clubMemberMapper,
                emailTemplateClient,
                registerReviewRequestMapper,
                reviewActionTokenService,
                tenantWorkflowConfigService,
                systemConfigMapper,
                objectMapper,
                stringRedisTemplate
        );
        ReflectionTestUtils.setField(service, "baseMapper", joinRequestMapper);
    }

    @AfterEach
    void tearDown() {
        com.tianji.common.utils.TenantContext.clear();
    }

    @Test
    void getApplicationByStudentId_shouldApplyCurrentTenantScope() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(joinRequestMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        com.tianji.common.utils.TenantContext.setTenantId(2L);

        service.getApplicationByStudentId("S20260425001");

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(joinRequestMapper).selectOne(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment();

        assertTrue(sqlSegment.contains("tenant_id"), "student-id lookup must include tenant scope");
        assertTrue(sqlSegment.contains("student_id"), "student-id lookup must still filter by student id");
    }

    @Test
    void getAllApplications_shouldStayTenantScopedForTenantOneWithoutSuperAdminHeader() {
        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        com.tianji.common.utils.TenantContext.setTenantId(1L);
        com.tianji.common.utils.TenantContext.setSuperAdmin(false);

        service.getAllApplications();

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(joinRequestMapper, atLeastOnce()).selectList(wrapperCaptor.capture());
        List<Wrapper> capturedWrappers = wrapperCaptor.getAllValues();
        String sqlSegment = capturedWrappers.get(capturedWrappers.size() - 1).getCustomSqlSegment();

        assertTrue(sqlSegment.contains("tenant_id"), "tenant-1 admission list must not leak other tenants without super-admin header");
    }

    @Test
    void getAllApplications_shouldUseAllActiveTenantsForSuperAdminView() {
        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));
        com.tianji.common.utils.TenantContext.setTenantId(1L);
        com.tianji.common.utils.TenantContext.setSuperAdmin(true);

        service.getAllApplications();

        ArgumentCaptor<Wrapper> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(joinRequestMapper, atLeastOnce()).selectList(wrapperCaptor.capture());
        List<Wrapper> capturedWrappers = wrapperCaptor.getAllValues();
        String sqlSegment = capturedWrappers.get(capturedWrappers.size() - 1).getCustomSqlSegment();

        verify(tenantMapper).selectList(any(Wrapper.class));
        assertTrue(sqlSegment.contains("IN"), "super-admin admission list should expand to all active tenants");
    }

    @Test
    void getApplicationStatistics_shouldUseAllTenantCacheKeyWhenSuperAdminHasCurrentTenant() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any())).thenReturn(null);
        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));
        com.tianji.common.utils.TenantContext.setTenantId(1L);
        com.tianji.common.utils.TenantContext.setSuperAdmin(true);

        service.getApplicationStatistics();

        verify(valueOperations).get("admission:statistics:all");
        verify(tenantMapper).selectList(any(Wrapper.class));
    }

    @Test
    void getApplicationStatistics_shouldRecomputeLegacyCacheMissingUniqueApprovedCount() {
        Map<String, Object> legacyCache = new LinkedHashMap<>();
        legacyCache.put("total", 46);
        legacyCache.put("approvedCount", 46);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("admission:statistics:all")).thenReturn(legacyCache);
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));

        JoinRequest duplicateA = approvedJoinRequest(2L, 110L, "OLD-STUDENT");
        JoinRequest duplicateB = approvedJoinRequest(2L, 110L, "NEW-STUDENT");
        JoinRequest anotherApplicant = approvedJoinRequest(2L, 111L, "OTHER-STUDENT");
        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(duplicateA, duplicateB, anotherApplicant));
        when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(new ClubMember());
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(new User());
        com.tianji.common.utils.TenantContext.setTenantId(1L);
        com.tianji.common.utils.TenantContext.setSuperAdmin(true);

        Map<String, Object> statistics = service.getApplicationStatistics();

        assertTrue(statistics.containsKey("approvedUniqueApplicantCount"));
        assertEquals(2L, statistics.get("approvedUniqueApplicantCount"));
    }

    @Test
    void reconcileApprovedMembersForCurrentTenant_shouldRestoreSoftDeletedMemberRecords() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setId(9L);
        joinRequest.setTenantId(1L);
        joinRequest.setStudentId("S20260425002");
        joinRequest.setName("member-a");
        joinRequest.setEmail("member-a@example.com");
        joinRequest.setPhone("13800138000");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");
        joinRequest.setUsername("member_a");
        joinRequest.setAuthUserId(99L);

        User localUser = new User();
        localUser.setId(11L);
        localUser.setAuthUserId(99L);
        localUser.setTenantId(1L);

        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(joinRequest), List.of(joinRequest));
        when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(null, new ClubMember());
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(localUser, localUser, localUser);

        service.reconcileApprovedMembersForCurrentTenant();

        verify(userService).saveUser(any(UserDTO.class));
    }

    @Test
    void reconcileApprovedMembersForCurrentTenant_shouldBackfillMissingLocalUserForCrossTenantApprovedApplicant() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setId(19L);
        joinRequest.setTenantId(2L);
        joinRequest.setStudentId("CT1776439507179");
        joinRequest.setName("跨租户申请人");
        joinRequest.setEmail("cross_join_1776439507179@example.com");
        joinRequest.setPhone("13800138001");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");
        joinRequest.setUsername("member_a");
        joinRequest.setAuthUserId(888L);
        joinRequest.setSourceAuthUserId(8L);

        ClubMember existingMember = new ClubMember();
        existingMember.setId(104L);
        existingMember.setTenantId(2L);
        existingMember.setStudentId("CT1776439507179");
        existingMember.setIsDeleted(0);

        User localUser = new User();
        localUser.setId(501L);
        localUser.setAuthUserId(888L);
        localUser.setTenantId(2L);
        localUser.setUsername("member_a");

        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(joinRequest));
        when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(existingMember, existingMember);
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, null, null, null, localUser);

        service.reconcileApprovedMembersForCurrentTenant();

        ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService).saveUser(userCaptor.capture());
        UserDTO savedUser = userCaptor.getValue();
        assertEquals("CT1776439507179", savedUser.getStudentId());
        assertEquals("cross_join_1776439507179@example.com", savedUser.getEmail());
        assertEquals("Software", savedUser.getMajor());
        assertEquals("2026", savedUser.getGrade());
        assertEquals("13800138001", savedUser.getCellPhone());
    }

    @Test
    void ensureLocalUser_shouldSeedCompleteStudentProfileOnFirstSave() throws Exception {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setStudentId("CT1776439507179");
        joinRequest.setName("跨租户申请人");
        joinRequest.setEmail("cross_join_1776439507179@example.com");
        joinRequest.setPhone("13800138001");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");
        joinRequest.setUsername("member_a");
        joinRequest.setFormPayloadJson("{\"college\":\"Computer Science\"}");

        User localUser = new User();
        localUser.setId(501L);
        localUser.setAuthUserId(888L);
        localUser.setTenantId(2L);
        localUser.setUsername("member_a");

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, localUser);
        when(objectMapper.readValue(joinRequest.getFormPayloadJson(), LinkedHashMap.class)).thenReturn(
                new LinkedHashMap<>(Map.of("college", "Computer Science"))
        );

        ReflectionTestUtils.invokeMethod(service, "ensureLocalUser", joinRequest, 888L);

        ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService).saveUser(userCaptor.capture());
        UserDTO savedUser = userCaptor.getValue();
        assertEquals("CT1776439507179", savedUser.getStudentId());
        assertEquals("cross_join_1776439507179@example.com", savedUser.getEmail());
        assertEquals("Software", savedUser.getMajor());
        assertEquals("2026", savedUser.getGrade());
        assertEquals("13800138001", savedUser.getCellPhone());
        assertEquals("Computer Science", savedUser.getCollege());
    }

    @Test
    void ensureLocalUser_shouldStillUseUnifiedSaveUserForExistingLocalUserAndCarryJoinDate() throws Exception {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setStudentId("CT1776439507188");
        joinRequest.setName("Existing Local User");
        joinRequest.setEmail("existing_local_user@example.com");
        joinRequest.setPhone("13800138018");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");
        joinRequest.setUsername("existing_local_user");
        joinRequest.setCreateTime(LocalDateTime.of(2026, 4, 28, 9, 20, 0));
        joinRequest.setReviewTime(LocalDateTime.of(2026, 4, 29, 10, 30, 0));
        joinRequest.setFormPayloadJson("{\"college\":\"Computer Science\"}");

        User localUser = new User();
        localUser.setId(508L);
        localUser.setAuthUserId(889L);
        localUser.setTenantId(2L);
        localUser.setUsername("existing_local_user");
        localUser.setStudentId("OLD-CT1776439507188");

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(localUser, localUser);
        when(objectMapper.readValue(joinRequest.getFormPayloadJson(), LinkedHashMap.class)).thenReturn(
                new LinkedHashMap<>(Map.of("college", "Computer Science"))
        );

        ReflectionTestUtils.invokeMethod(service, "ensureLocalUser", joinRequest, 889L);

        ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService).saveUser(userCaptor.capture());
        UserDTO savedUser = userCaptor.getValue();
        assertEquals("CT1776439507188", savedUser.getStudentId());
        assertEquals("existing_local_user@example.com", savedUser.getEmail());
        assertEquals("Computer Science", savedUser.getCollege());
        assertEquals(LocalDateTime.of(2026, 4, 29, 0, 0), savedUser.getJoinDate());
    }

    @Test
    void reconcileApprovedMembersForCurrentTenant_shouldProvisionApprovedRegisterReviewIntoLocalUserAndMember() throws Exception {
        RegisterReviewRequest request = new RegisterReviewRequest();
        request.setId(41L);
        request.setTenantId(1L);
        request.setAuthUserId(5460L);
        request.setUsername("review_member");
        request.setName("测试成员");
        request.setEmail("review_member@example.com");
        request.setReviewStatus("APPROVED");
        request.setFormPayloadJson("{\"studentId\":\"20260001\",\"major\":\"Software\",\"grade\":\"2026\",\"college\":\"CS\",\"phone\":\"13800138002\"}");

        User localUser = new User();
        localUser.setId(91L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(5460L);
        localUser.setUsername("review_member");
        localUser.setRealName("测试成员");
        localUser.setEmail("review_member@example.com");
        localUser.setStudentId("20260001");
        localUser.setMajor("Software");
        localUser.setGrade("2026");
        localUser.setCellPhone("13800138002");

        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(registerReviewRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(request));
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, localUser, localUser);
        when(objectMapper.readValue(request.getFormPayloadJson(), LinkedHashMap.class)).thenReturn(new LinkedHashMap<>(Map.of(
                "studentId", "20260001",
                "major", "Software",
                "grade", "2026",
                "college", "CS",
                "phone", "13800138002"
        )));

        service.reconcileApprovedMembersForCurrentTenant();

        verify(userService).saveUser(any());
        verify(registerReviewRequestMapper).updateById(request);
    }

    @Test
    void reconcileApprovedMembersForCurrentTenant_shouldPassJoinDateIntoUnifiedRegisterProvision() throws Exception {
        RegisterReviewRequest request = new RegisterReviewRequest();
        request.setId(42L);
        request.setTenantId(1L);
        request.setAuthUserId(5461L);
        request.setUsername("review_member_join");
        request.setName("Join Date Register");
        request.setEmail("review_member_join@example.com");
        request.setReviewStatus("APPROVED");
        request.setCreateTime(LocalDateTime.of(2026, 4, 27, 8, 0, 0));
        request.setReviewTime(LocalDateTime.of(2026, 4, 30, 11, 0, 0));
        request.setFormPayloadJson("{\"studentId\":\"20260002\",\"major\":\"Software\",\"grade\":\"2026\",\"college\":\"CS\",\"phone\":\"13800138003\"}");

        User localUser = new User();
        localUser.setId(92L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(5461L);
        localUser.setUsername("review_member_join");
        localUser.setRealName("Join Date Register");
        localUser.setEmail("review_member_join@example.com");
        localUser.setStudentId("20260002");
        localUser.setMajor("Software");
        localUser.setGrade("2026");
        localUser.setCellPhone("13800138003");

        when(joinRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(registerReviewRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(request));
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, localUser, localUser);
        when(objectMapper.readValue(request.getFormPayloadJson(), LinkedHashMap.class)).thenReturn(new LinkedHashMap<>(Map.of(
                "studentId", "20260002",
                "major", "Software",
                "grade", "2026",
                "college", "CS",
                "phone", "13800138003"
        )));

        service.reconcileApprovedMembersForCurrentTenant();

        ArgumentCaptor<UserDTO> userCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userService).saveUser(userCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 4, 30, 0, 0), userCaptor.getValue().getJoinDate());
    }

    private RKTenant activeTenant(Long id) {
        RKTenant tenant = new RKTenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        return tenant;
    }

    private JoinRequest approvedJoinRequest(Long tenantId, Long sourceAuthUserId, String studentId) {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(tenantId);
        joinRequest.setAuthUserId(sourceAuthUserId);
        joinRequest.setSourceAuthUserId(sourceAuthUserId);
        joinRequest.setUsername("user_" + sourceAuthUserId);
        joinRequest.setStudentId(studentId);
        joinRequest.setReviewStatus("通过");
        joinRequest.setApplicationTime(LocalDateTime.now());
        return joinRequest;
    }
}
