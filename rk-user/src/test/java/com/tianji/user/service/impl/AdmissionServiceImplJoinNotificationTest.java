package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.RoleAccountDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.message.api.client.EmailTemplateClient;
import com.tianji.user.domain.dto.EmailCenterSendDTO;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.JoinRequest;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.ReferralCode;
import com.tianji.user.domain.po.RegisterReviewRequest;
import com.tianji.user.domain.po.User;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.JoinRequestMapper;
import com.tianji.user.mapper.RegisterReviewRequestMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.ReferralCodeMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IEmailCenterService;
import com.tianji.user.service.IEmailVerificationService;
import com.tianji.user.service.IReferralCodeService;
import com.tianji.user.service.ITenantWorkflowConfigService;
import com.tianji.user.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

import org.apache.ibatis.builder.MapperBuilderAssistant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdmissionServiceImplJoinNotificationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthClient authClient;
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
    private EmailTemplateClient emailTemplateClient;
    @Mock
    private ReviewActionTokenServiceImpl reviewActionTokenService;
    @Mock
    private RegisterReviewRequestMapper registerReviewRequestMapper;
    @Mock
    private ITenantWorkflowConfigService tenantWorkflowConfigService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Spy
    @InjectMocks
    private AdmissionServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), JoinRequest.class);
        ReflectionTestUtils.setField(service, "publicWebBaseUrl", "http://localhost:5173");
        ReflectionTestUtils.setField(service, "baseMapper", joinRequestMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void submitApplication_shouldNotifyTargetTenantAdminsWithSourceTenantIdentityAndReferralOwner() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setSourceTenantId(1L);
        joinRequest.setSourceAuthUserId(101L);
        joinRequest.setSourceRoleName("鏅€氭垚鍛?);
        joinRequest.setName("璺ㄧ鎴风敵璇蜂汉");
        joinRequest.setStudentId("S20260414002");
        joinRequest.setUsername("member_a");
        joinRequest.setEmail("cross@example.com");
        joinRequest.setEmailCode("123456");
        joinRequest.setReferralCode("REF2001");

        RKTenant sourceTenant = new RKTenant();
        sourceTenant.setId(1L);
        sourceTenant.setTenantName("绀轰緥瀛︽牎");
        RKTenant targetTenant = new RKTenant();
        targetTenant.setId(2L);
        targetTenant.setTenantName("绀轰緥浼佷笟B");

        User sourceUser = new User();
        sourceUser.setAuthUserId(101L);
        sourceUser.setTenantId(1L);
        sourceUser.setUsername("member_a");
        sourceUser.setRealName("寮犱笁");
        sourceUser.setStudentId("S20260414002");

        ClubMember sourceMember = new ClubMember();
        sourceMember.setTenantId(1L);
        sourceMember.setStudentId("S20260414002");
        sourceMember.setPosition("鍓嶇缁勯暱");

        ReferralCode referralCode = new ReferralCode();
        referralCode.setCode("REF2001");
        referralCode.setTenantId(2L);
        referralCode.setGeneratorId(201L);

        User referralOwner = new User();
        referralOwner.setAuthUserId(201L);
        referralOwner.setTenantId(2L);
        referralOwner.setUsername("manager_b");
        referralOwner.setRealName("鏉庤€佸笀");

        RoleAccountDTO targetAdmin = new RoleAccountDTO();
        targetAdmin.setAccountId(301L);
        targetAdmin.setTenantId(2L);
        targetAdmin.setUsername("admin_b");

        User targetAdminUser = new User();
        targetAdminUser.setAuthUserId(301L);
        targetAdminUser.setTenantId(2L);
        targetAdminUser.setUsername("admin_b");
        targetAdminUser.setRealName("绉熸埛绠＄悊鍛?);
        targetAdminUser.setEmail("admin_b@example.com");

        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");
        RoleDTO tenantClubManagerRole = new RoleDTO();
        tenantClubManagerRole.setId(18L);
        tenantClubManagerRole.setCode("CLUB_MANAGER");
        RoleDTO platformAdminRole = new RoleDTO();
        platformAdminRole.setId(1L);
        platformAdminRole.setCode("ADMIN");

        when(emailVerificationService.verifyCode(any())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doReturn(null).when(service).getApplicationByStudentId("S20260414002");
        doReturn(true).when(service).save(any(JoinRequest.class));
        when(tenantMapper.selectById(1L)).thenReturn(sourceTenant);
        when(tenantMapper.selectById(2L)).thenReturn(targetTenant);
        when(userMapper.selectOne(any())).thenReturn(sourceUser, referralOwner);
        when(clubMemberMapper.selectOne(any())).thenReturn(sourceMember);
        when(referralCodeMapper.selectOne(any())).thenReturn(referralCode);
        when(authClient.listAllRoles(2L)).thenReturn(List.of(tenantAdminRole, tenantClubManagerRole));
        when(authClient.listAllRoles(1L)).thenReturn(List.of(platformAdminRole));
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(targetAdmin), List.of());
        when(userMapper.selectList(any())).thenReturn(List.of(targetAdminUser));
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), any(), eq("APPROVE"))).thenReturn("join-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), any(), eq("REJECT"))).thenReturn("join-reject-token");

        TenantContext.setTenantId(2L);

        boolean result = service.submitApplication(joinRequest);

        assertTrue(result);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertTrue(dto.getManualEmails().contains("admin_b@example.com"));
        assertTrue(joinRequest.getEmailSent());
        assertTrue(dto.getContent().contains("绀轰緥瀛︽牎"));
        assertTrue(dto.getContent().contains("鏅€氭垚鍛?));
        assertTrue(dto.getContent().contains("鍓嶇缁勯暱"));
        assertTrue(dto.getContent().contains("鏉庤€佸笀"));
    }

    @Test
    void notifyRegisterSuccess_shouldSendNotificationToResolvedRecipients() {
        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");
        RoleDTO platformAdminRole = new RoleDTO();
        platformAdminRole.setId(1L);
        platformAdminRole.setCode("ADMIN");

        RoleAccountDTO tenantAdmin = new RoleAccountDTO();
        tenantAdmin.setAccountId(301L);
        tenantAdmin.setTenantId(2L);
        tenantAdmin.setUsername("admin_b");

        User tenantAdminUser = new User();
        tenantAdminUser.setAuthUserId(301L);
        tenantAdminUser.setTenantId(2L);
        tenantAdminUser.setUsername("admin_b");
        tenantAdminUser.setEmail("admin_b@example.com");
        tenantAdminUser.setRealName("绉熸埛绠＄悊鍛楤");

        ReferralCode referralCode = new ReferralCode();
        referralCode.setCode("REF-REGISTER");
        referralCode.setTenantId(2L);
        referralCode.setGeneratorId(401L);

        User referralOwner = new User();
        referralOwner.setAuthUserId(401L);
        referralOwner.setTenantId(2L);
        referralOwner.setUsername("member_b");
        referralOwner.setRealName("鎺ㄨ崘浜築");

        RKTenant targetTenant = new RKTenant();
        targetTenant.setId(2L);
        targetTenant.setTenantName("绀轰緥浼佷笟B");

        when(authClient.listAllRoles(2L)).thenReturn(List.of(tenantAdminRole));
        when(authClient.listAllRoles(1L)).thenReturn(List.of(platformAdminRole));
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(tenantAdmin), List.of());
        when(userMapper.selectList(any())).thenReturn(List.of(tenantAdminUser));
        when(referralCodeMapper.selectOne(any())).thenReturn(referralCode);
        when(userMapper.selectOne(any())).thenReturn(referralOwner);
        when(tenantMapper.selectById(2L)).thenReturn(targetTenant);
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("register-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("register-reject-token");

        Map<String, Object> formPayload = new LinkedHashMap<>();
        formPayload.put("major", "杞欢宸ョ▼");
        formPayload.put("grade", "澶т竴");

        boolean notified = service.notifyRegisterSuccess(2L, "new_user", "鏂板悓瀛?, "new_user@example.com", "REF-REGISTER", formPayload);

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertTrue(dto.getManualEmails().contains("admin_b@example.com"));
        assertTrue(dto.getSubject().contains("绀轰緥浼佷笟B"));
        assertTrue(dto.getContent().contains("鏂板悓瀛?));
        assertTrue(dto.getContent().contains("new_user@example.com"));
        assertTrue(dto.getContent().contains("鎺ㄨ崘浜築"));
        assertTrue(dto.getContent().contains("register-approve-token"));
        assertTrue(dto.getContent().contains("杞欢宸ョ▼"));
    }
    @Test
    void notifyRegisterSuccess_shouldFallbackToTenantContactEmailWhenNoRoleRecipientExists() {
        WorkflowPolicyDTO registration = new WorkflowPolicyDTO();
        registration.setOpenRegistration(false);
        registration.setRequireApproval(true);
        registration.setNotifyAdmins(true);
        registration.setNotifyApplicantOnFailure(true);
        TenantWorkflowConfigDTO workflowConfig = new TenantWorkflowConfigDTO();
        workflowConfig.setRegistration(registration);

        RKTenant tenant = new RKTenant();
        tenant.setId(30L);
        tenant.setTenantName("杞欢椤圭洰寮€鍙戠ぞ鍥?);
        tenant.setContactEmail("club-admin@example.com");

        when(tenantWorkflowConfigService.loadCurrentConfig(30L)).thenReturn(workflowConfig);
        when(authClient.listAllRoles(30L)).thenReturn(List.of());
        when(authClient.listAllRoles(1L)).thenReturn(List.of());
        when(tenantMapper.selectById(30L)).thenReturn(tenant);
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("approve-token");
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("reject-token");

        boolean notified = service.notifyRegisterSuccess(30L, 5820L, "new_user", "鏂板悓瀛?, "student@example.com", null, Map.of("major", "杞欢宸ョ▼"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        assertTrue(captor.getValue().getManualEmails().contains("club-admin@example.com"));
    }

    @Test
    void notifyRegisterSuccess_shouldFallbackToLocalPrivilegedUserEmailsWhenRoleRecipientMissing() {
        RKTenant tenant = new RKTenant();
        tenant.setId(30L);
        tenant.setTenantName("杞欢椤圭洰寮€鍙戠ぞ鍥?);

        User localAdmin = new User();
        localAdmin.setTenantId(30L);
        localAdmin.setRealName("鏈湴绠＄悊鍛?);
        localAdmin.setEmail("local-admin@example.com");
        localAdmin.setType(com.tianji.common.enums.UserType.STAFF);

        when(authClient.listAllRoles(30L)).thenReturn(List.of());
        when(authClient.listAllRoles(1L)).thenReturn(List.of());
        when(userMapper.selectList(any())).thenReturn(List.of(localAdmin), List.of());
        when(tenantMapper.selectById(30L)).thenReturn(tenant);
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("approve-token");
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("reject-token");

        boolean notified = service.notifyRegisterSuccess(30L, 5820L, "new_user", "鏂板悓瀛?, "student@example.com", null, Map.of("major", "杞欢宸ョ▼"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        assertTrue(captor.getValue().getManualEmails().contains("local-admin@example.com"));
    }

    @Test
    void notifyRegisterSuccess_shouldFallbackToLocalPrivilegedUserEmailsWhenRoleLookupFails() {
        RKTenant tenant = new RKTenant();
        tenant.setId(30L);
        tenant.setTenantName("杞欢椤圭洰寮€鍙戠ぞ鍥?);

        User localAdmin = new User();
        localAdmin.setTenantId(30L);
        localAdmin.setRealName("Local Admin");
        localAdmin.setEmail("local-admin@example.com");
        localAdmin.setType(com.tianji.common.enums.UserType.STAFF);

        when(authClient.listAllRoles(30L)).thenThrow(new RuntimeException("auth unavailable"));
        when(userMapper.selectList(any())).thenReturn(List.of(localAdmin), List.of());
        when(tenantMapper.selectById(30L)).thenReturn(tenant);
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("approve-token");
        when(reviewActionTokenService.createToken(eq(30L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("reject-token");

        boolean notified = service.notifyRegisterSuccess(30L, 5820L, "new_user", "New User", "student@example.com", null, Map.of("major", "software"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        assertTrue(captor.getValue().getManualEmails().contains("local-admin@example.com"));
    }

    @Test
    void submitApplication_shouldUpdatePendingApplicationForSameSourceAccountInTargetTenant() {
        JoinRequest existing = new JoinRequest();
        existing.setId(81L);
        existing.setTenantId(2L);
        existing.setSourceAuthUserId(110L);
        existing.setStudentId("OLD-STUDENT");
        existing.setReviewStatus("寰呭鏍?);
        existing.setEmail("old@example.com");

        JoinRequest request = new JoinRequest();
        request.setTenantId(2L);
        request.setSourceAuthUserId(110L);
        request.setSourceTenantId(1L);
        request.setSourceRoleName("member");
        request.setName("Same Account");
        request.setStudentId("NEW-STUDENT");
        request.setUsername("member_a");
        request.setEmail("new@example.com");
        request.setEmailCode("123456");
        request.setPhone("13800138081");
        request.setMajor("Software");
        request.setGrade("2026");

        doReturn(null).when(service).getApplicationByStudentId("NEW-STUDENT");
        when(joinRequestMapper.selectOne(any())).thenReturn(existing);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doReturn(true).when(service).updateById(existing);

        boolean submitted = service.submitApplication(request);

        assertTrue(submitted);
        assertEquals("NEW-STUDENT", existing.getStudentId());
        assertEquals("new@example.com", existing.getEmail());
        verify(service).updateById(existing);
        verify(service, never()).save(any(JoinRequest.class));
    }

    @Test
    void submitApplication_shouldRejectApprovedApplicationForSameSourceAccountInTargetTenant() {
        JoinRequest existing = new JoinRequest();
        existing.setId(82L);
        existing.setTenantId(2L);
        existing.setSourceAuthUserId(110L);
        existing.setStudentId("APPROVED-STUDENT");
        existing.setReviewStatus("閫氳繃");

        JoinRequest request = new JoinRequest();
        request.setTenantId(2L);
        request.setSourceAuthUserId(110L);
        request.setName("Same Account");
        request.setStudentId("NEW-STUDENT");
        request.setUsername("member_a");
        request.setEmail("new-approved@example.com");
        request.setEmailCode("123456");

        doReturn(null).when(service).getApplicationByStudentId("NEW-STUDENT");
        when(joinRequestMapper.selectOne(any())).thenReturn(existing);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        org.junit.jupiter.api.Assertions.assertThrows(
                BadRequestException.class,
                () -> service.submitApplication(request)
        );
        verify(service, never()).save(any(JoinRequest.class));
    }

    @Test
    void reviewRegisterRequest_shouldQueueApprovalEmailToApplicant() {
        RegisterReviewRequest request = new RegisterReviewRequest();
        request.setId(88L);
        request.setTenantId(1L);
        request.setAuthUserId(501L);
        request.setUsername("tenant_user");
        request.setName("applicant");
        request.setEmail("applicant@example.com");
        request.setReviewStatus("PENDING");
        request.setCreateTime(LocalDateTime.now().minusDays(1));
        request.setFormPayloadJson("{\"studentId\":\"S20260428001\",\"major\":\"software\",\"grade\":\"2022\",\"phone\":\"13800000000\"}");

        RKTenant tenant = new RKTenant();
        tenant.setId(1L);
        tenant.setTenantName("Tenant One");

        User localUser = new User();
        localUser.setTenantId(1L);
        localUser.setAuthUserId(501L);
        localUser.setUsername("tenant_user");
        localUser.setRealName("applicant");
        localUser.setEmail("applicant@example.com");
        localUser.setStudentId("S20260428001");

        when(registerReviewRequestMapper.selectById(88L)).thenReturn(request);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(userMapper.selectOne(any())).thenReturn(localUser);
        boolean reviewed = service.reviewRegisterRequest(88L, "APPROVED", "approved", 1L);

        assertTrue(reviewed);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertTrue(dto.getManualEmails().contains("applicant@example.com"));
        assertTrue(dto.getSubject().contains("Tenant One"));
        assertTrue(dto.getContent().contains("approved"));
    }

    @Test
    void reviewRegisterRequest_shouldSyncExistingLocalUserAndReuseLegacyMemberWhenStudentIdChanges() {
        RegisterReviewRequest request = new RegisterReviewRequest();
        request.setId(89L);
        request.setTenantId(1L);
        request.setAuthUserId(5455L);
        request.setUsername("tenant_user");
        request.setName("Updated Name");
        request.setEmail("updated@example.com");
        request.setReviewStatus("PENDING");
        request.setCreateTime(LocalDateTime.now().minusDays(1));
        request.setFormPayloadJson("{\"studentId\":\"S-NEW\",\"major\":\"Software\",\"grade\":\"2026\",\"phone\":\"13800138001\",\"name\":\"Updated Name\",\"email\":\"updated@example.com\",\"username\":\"tenant_user\"}");

        RKTenant tenant = new RKTenant();
        tenant.setId(1L);
        tenant.setTenantName("Tenant One");

        User localUser = new User();
        localUser.setId(5577L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(5455L);
        localUser.setUsername("tenant_user");
        localUser.setRealName("Old Name");
        localUser.setEmail("old@example.com");
        localUser.setStudentId("S-OLD");

        when(registerReviewRequestMapper.selectById(89L)).thenReturn(request);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(userMapper.selectOne(any())).thenReturn(localUser);

        boolean reviewed = service.reviewRegisterRequest(89L, "APPROVED", "approved", 1L);

        assertTrue(reviewed);
        verify(userService).saveUser(argThat(dto ->
                dto != null
                        && Long.valueOf(5455L).equals(dto.getId())
                        && "tenant_user".equals(dto.getUsername())
                        && "Updated Name".equals(dto.getName())
                        && "updated@example.com".equals(dto.getEmail())
                        && "13800138001".equals(dto.getCellPhone())
                        && "S-NEW".equals(dto.getStudentId())
                        && "Software".equals(dto.getMajor())
                        && "2026".equals(dto.getGrade())
        ));
    }

    @Test
    void reviewApplication_shouldRetireLegacyMemberWhenApprovedApplicantStudentIdChanges() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setId(66L);
        joinRequest.setTenantId(1L);
        joinRequest.setAuthUserId(501L);
        joinRequest.setUsername("tenant_user");
        joinRequest.setName("Updated Join User");
        joinRequest.setEmail("updated-join@example.com");
        joinRequest.setPhone("13800138066");
        joinRequest.setStudentId("J-NEW");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");

        User localUser = new User();
        localUser.setId(7788L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(501L);
        localUser.setUsername("tenant_user");
        localUser.setRealName("Old Join User");
        localUser.setEmail("old-join@example.com");
        localUser.setStudentId("J-OLD");

        doReturn(joinRequest).when(service).getById(66L);
        doReturn(true).when(service).updateById(any(JoinRequest.class));
        when(userMapper.selectOne(any())).thenReturn(localUser);

        boolean reviewed = service.reviewApplication(66L, "閫氳繃", "approved", 1L);

        assertTrue(reviewed);
        verify(userService).saveUser(argThat(dto ->
                dto != null
                        && Long.valueOf(501L).equals(dto.getId())
                        && Long.valueOf(1L).equals(dto.getTenantId())
                        && "tenant_user".equals(dto.getUsername())
                        && "Updated Join User".equals(dto.getName())
                        && "updated-join@example.com".equals(dto.getEmail())
                        && "13800138066".equals(dto.getCellPhone())
                        && "J-NEW".equals(dto.getStudentId())
                        && "Software".equals(dto.getMajor())
                        && "2026".equals(dto.getGrade())
        ));
    }

    @Test
    void reviewApplication_shouldNotifyApplicantWithJoinReviewResultEmail() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setId(67L);
        joinRequest.setTenantId(1L);
        joinRequest.setAuthUserId(601L);
        joinRequest.setUsername("join_user_result");
        joinRequest.setName("Join Applicant");
        joinRequest.setEmail("join_applicant@example.com");
        joinRequest.setPhone("13800138067");
        joinRequest.setStudentId("J-RESULT");
        joinRequest.setMajor("Software");
        joinRequest.setGrade("2026");

        User localUser = new User();
        localUser.setId(7789L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(601L);
        localUser.setUsername("join_user_result");
        localUser.setRealName("Join Applicant");
        localUser.setEmail("join_applicant@example.com");
        localUser.setStudentId("J-RESULT");

        RKTenant tenant = new RKTenant();
        tenant.setId(1L);
        tenant.setTenantName("Tenant One");

        doReturn(joinRequest).when(service).getById(67L);
        doReturn(true).when(service).updateById(any(JoinRequest.class));
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        boolean reviewed = service.reviewApplication(67L, "闁俺绻?, "approved", 1L);

        assertTrue(reviewed);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertTrue(dto.getManualEmails().contains("join_applicant@example.com"));
        assertTrue(dto.getSubject().contains("Tenant One"));
        assertTrue(dto.getContent().contains("approved"));
    }

    @Test
    void notifyRegisterSuccess_shouldRouteApprovalNoticeToDesignatedAdvisorRolesWhenConfigured() {
        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");
        RoleDTO teacherRole = new RoleDTO();
        teacherRole.setId(19L);
        teacherRole.setCode("TEACHER");

        RoleAccountDTO advisorAccount = new RoleAccountDTO();
        advisorAccount.setAccountId(901L);
        advisorAccount.setTenantId(2L);
        advisorAccount.setUsername("teacher_b");

        User advisorUser = new User();
        advisorUser.setAuthUserId(901L);
        advisorUser.setTenantId(2L);
        advisorUser.setUsername("teacher_b");
        advisorUser.setRealName("鎸囧鑰佸笀B");
        advisorUser.setEmail("teacher_b@example.com");

        TenantWorkflowConfigDTO workflowConfig = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO registration = new WorkflowPolicyDTO();
        registration.setRequireApproval(true);
        registration.setNotifyAdmins(true);
        registration.setAdvisorMode("DESIGNATED");
        registration.setDesignatedAdvisorRoleIds(List.of(19L));
        workflowConfig.setRegistration(registration);

        RKTenant targetTenant = new RKTenant();
        targetTenant.setId(2L);
        targetTenant.setTenantName("绀轰緥浼佷笟B");

        when(tenantWorkflowConfigService.loadCurrentConfig(2L)).thenReturn(workflowConfig);
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(advisorAccount));
        when(userMapper.selectList(any())).thenReturn(List.of(advisorUser));
        when(tenantMapper.selectById(2L)).thenReturn(targetTenant);
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("register-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("register-reject-token");

        boolean notified = service.notifyRegisterSuccess(2L, "new_user", "鏂板悓瀛?, "new_user@example.com", null, Map.of("major", "杞欢宸ョ▼"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertEquals(List.of("teacher_b@example.com"), dto.getManualEmails());
    }

    @Test
    void notifyRegisterSuccess_shouldNotifyApplicantWhenNoApprovalRecipientCanBeResolved() {
        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");

        TenantWorkflowConfigDTO workflowConfig = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO registration = new WorkflowPolicyDTO();
        registration.setRequireApproval(true);
        registration.setNotifyAdmins(true);
        registration.setNotifyApplicantOnFailure(true);
        registration.setAdvisorMode("DESIGNATED");
        registration.setDesignatedAdvisorRoleIds(List.of(19L));
        workflowConfig.setRegistration(registration);

        when(tenantWorkflowConfigService.loadCurrentConfig(2L)).thenReturn(workflowConfig);
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(), List.of());

        boolean notified = service.notifyRegisterSuccess(2L, "new_user", "鏂板悓瀛?, "new_user@example.com", null, Map.of("major", "杞欢宸ョ▼"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertEquals(List.of("new_user@example.com"), dto.getManualEmails());
    }

    @Test
    void submitApplication_shouldRouteJoinApprovalNoticeToDesignatedAdvisorRolesWhenConfigured() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setName("join applicant");
        joinRequest.setStudentId("S20260429001");
        joinRequest.setUsername("join_user");
        joinRequest.setEmail("join_user@example.com");
        joinRequest.setEmailCode("123456");

        RoleDTO teacherRole = new RoleDTO();
        teacherRole.setId(19L);
        teacherRole.setCode("TEACHER");

        RoleAccountDTO advisorAccount = new RoleAccountDTO();
        advisorAccount.setAccountId(901L);
        advisorAccount.setTenantId(2L);
        advisorAccount.setUsername("teacher_b");

        User advisorUser = new User();
        advisorUser.setAuthUserId(901L);
        advisorUser.setTenantId(2L);
        advisorUser.setUsername("teacher_b");
        advisorUser.setRealName("advisor");
        advisorUser.setEmail("teacher_b@example.com");

        TenantWorkflowConfigDTO workflowConfig = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO joinReview = new WorkflowPolicyDTO();
        joinReview.setNotifyAdmins(true);
        joinReview.setAdvisorMode("DESIGNATED");
        joinReview.setDesignatedAdvisorRoleIds(List.of(19L));
        workflowConfig.setJoinReview(joinReview);

        when(emailVerificationService.verifyCode(any())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doReturn(null).when(service).getApplicationByStudentId("S20260429001");
        doReturn(true).when(service).save(any(JoinRequest.class));
        when(tenantWorkflowConfigService.loadCurrentConfig(2L)).thenReturn(workflowConfig);
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(advisorAccount));
        when(userMapper.selectList(any())).thenReturn(List.of(advisorUser));
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), any(), eq("APPROVE"))).thenReturn("join-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("JOIN"), any(), eq("REJECT"))).thenReturn("join-reject-token");

        TenantContext.setTenantId(2L);

        boolean submitted = service.submitApplication(joinRequest);

        assertTrue(submitted);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertEquals(List.of("teacher_b@example.com"), dto.getManualEmails());
    }

    @Test
    void submitApplication_shouldNotifyApplicantWhenJoinApprovalRecipientCannotBeResolved() {
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setTenantId(2L);
        joinRequest.setName("join applicant");
        joinRequest.setStudentId("S20260429002");
        joinRequest.setUsername("join_user2");
        joinRequest.setEmail("join_user2@example.com");
        joinRequest.setEmailCode("123456");

        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");

        TenantWorkflowConfigDTO workflowConfig = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO joinReview = new WorkflowPolicyDTO();
        joinReview.setNotifyAdmins(true);
        joinReview.setNotifyApplicantOnFailure(true);
        joinReview.setAdvisorMode("DESIGNATED");
        joinReview.setDesignatedAdvisorRoleIds(List.of(19L));
        workflowConfig.setJoinReview(joinReview);

        when(emailVerificationService.verifyCode(any())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        doReturn(null).when(service).getApplicationByStudentId("S20260429002");
        doReturn(true).when(service).save(any(JoinRequest.class));
        when(tenantWorkflowConfigService.loadCurrentConfig(2L)).thenReturn(workflowConfig);
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(), List.of());

        TenantContext.setTenantId(2L);

        boolean submitted = service.submitApplication(joinRequest);

        assertTrue(submitted);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        EmailCenterSendDTO dto = captor.getValue();
        assertEquals(List.of("join_user2@example.com"), dto.getManualEmails());
    }

    @Test
    void notifyRegisterSuccess_shouldUsePublicFrontendReviewUrlWhenRequestHostIsPrivate() {
        RoleDTO tenantAdminRole = new RoleDTO();
        tenantAdminRole.setId(3L);
        tenantAdminRole.setCode("ADMIN");
        RoleDTO platformAdminRole = new RoleDTO();
        platformAdminRole.setId(1L);
        platformAdminRole.setCode("ADMIN");

        RoleAccountDTO tenantAdmin = new RoleAccountDTO();
        tenantAdmin.setAccountId(301L);
        tenantAdmin.setTenantId(2L);
        tenantAdmin.setUsername("admin_b");

        User tenantAdminUser = new User();
        tenantAdminUser.setAuthUserId(301L);
        tenantAdminUser.setTenantId(2L);
        tenantAdminUser.setUsername("admin_b");
        tenantAdminUser.setEmail("admin_b@example.com");

        RKTenant targetTenant = new RKTenant();
        targetTenant.setId(2L);
        targetTenant.setTenantName("Tenant B");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.addHeader("Host", "10.0.0.18:8082");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(authClient.listAllRoles(2L)).thenReturn(List.of(tenantAdminRole));
        when(authClient.listAllRoles(1L)).thenReturn(List.of(platformAdminRole));
        when(authClient.queryAccountsByRoles(any())).thenReturn(List.of(tenantAdmin), List.of());
        when(userMapper.selectList(any())).thenReturn(List.of(tenantAdminUser));
        when(tenantMapper.selectById(2L)).thenReturn(targetTenant);
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("APPROVE"))).thenReturn("register-approve-token");
        when(reviewActionTokenService.createToken(eq(2L), eq("REGISTER"), any(), eq("REJECT"))).thenReturn("register-reject-token");

        boolean notified = service.notifyRegisterSuccess(2L, "new_user", "new user", "new_user@example.com", null, Map.of("major", "software"));

        assertTrue(notified);
        ArgumentCaptor<EmailCenterSendDTO> captor = ArgumentCaptor.forClass(EmailCenterSendDTO.class);
        verify(emailCenterService).send(captor.capture());
        String content = captor.getValue().getContent();
        assertTrue(content.contains("https://example.com/review/admission?token=register-approve-token"));
        assertTrue(content.contains("https://example.com/review/admission?token=register-reject-token"));
        assertTrue(!content.contains("10.0.0.18:8082"));
    }

}
