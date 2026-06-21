package com.tianji.auth.service.impl;

import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.CurrentUserPasswordUpdateDTO;
import com.tianji.api.dto.user.EmailLoginCandidateDTO;
import com.tianji.api.dto.user.TenantSelfServiceAdmissionPolicyDTO;
import com.tianji.api.dto.user.TenantWorkflowConfigDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.api.dto.user.WorkflowPolicyDTO;
import com.tianji.auth.domain.dto.EmailLoginConfirmDTO;
import com.tianji.auth.domain.dto.EmailLoginPrepareDTO;
import com.tianji.api.dto.user.RegisterSuccessNotifyDTO;
import com.tianji.common.domain.dto.LoginUserDTO;
import com.tianji.auth.domain.dto.RegisterDTO;
import com.tianji.auth.domain.po.AccountRole;
import com.tianji.auth.domain.po.LoginRecord;
import com.tianji.auth.domain.po.Role;
import com.tianji.auth.domain.po.User;
import com.tianji.auth.domain.vo.EmailLoginPrepareVO;
import com.tianji.auth.mapper.AccountRoleMapper;
import com.tianji.auth.mapper.LoginRecordMapper;
import com.tianji.auth.mapper.RoleMapper;
import com.tianji.auth.mapper.UserMapper;
import com.tianji.auth.service.IRoleService;
import com.tianji.auth.util.JwtTool;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.TenantContext;
import com.tianji.auth.domain.vo.LoginVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private UserClient userClient;
    @Mock
    private RoleMapper roleMapper;
    @Mock
    private AccountRoleMapper accountRoleMapper;
    @Mock
    private LoginRecordMapper loginRecordMapper;
    @Mock
    private IRoleService roleService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTool jwtTool;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void register_shouldRejectWhenEmailCodeVerificationFails() {
        RegisterDTO dto = buildRegisterDTO();
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(dto));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_shouldRejectWhenTenantSelfServiceClosesPublicRegistration() {
        RegisterDTO dto = buildRegisterDTO();
        when(userClient.queryTenantSelfServiceAdmissionPolicy(1L))
                .thenReturn(new TenantSelfServiceAdmissionPolicyDTO()
                        .setTenantId(1L)
                        .setAllowPublicRegister(false)
                        .setAllowJoinApplication(true));

        assertThrows(BadRequestException.class, () -> authService.register(dto));

        verify(userClient, never()).verifyEmailVerificationCode(any());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_shouldDeferLocalUserSyncUntilApprovalWhenWorkflowRequiresApproval() {
        RegisterDTO dto = buildRegisterDTO();
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.queryCurrentTenantWorkflowConfig(1L)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(roleMapper.selectOne(any())).thenReturn((Role) null);
        authService.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(0, userCaptor.getValue().getStatus());
        verify(userClient, never()).syncUserOnRegister(any(UserDTO.class));
        ArgumentCaptor<RegisterSuccessNotifyDTO> notifyCaptor = ArgumentCaptor.forClass(RegisterSuccessNotifyDTO.class);
        verify(userClient).notifyRegisterSuccess(notifyCaptor.capture());
        assertEquals(1L, notifyCaptor.getValue().getTenantId());
        assertEquals(99L, notifyCaptor.getValue().getAuthUserId());
        assertEquals("email_user", notifyCaptor.getValue().getUsername());
        assertEquals("Email User", notifyCaptor.getValue().getName());
        assertEquals("test@example.com", notifyCaptor.getValue().getEmail());
        assertEquals(null, notifyCaptor.getValue().getReferralCode());
        assertEquals("软件工程", notifyCaptor.getValue().getFormPayload().get("major"));
    }

    @Test
    void register_shouldNotifyInvitationRegisterSuccessWhenInviteTokenPresent() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setInviteToken("invite-token");
        dto.setReferralCode("REF2600001");
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.queryCurrentTenantWorkflowConfig(1L)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(100L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(roleMapper.selectOne(any())).thenReturn((Role) null);
        authService.register(dto);

        verify(userClient).markInvitationRegisterSuccess("invite-token", "REF2600001", "test@example.com", 100L);
    }

    @Test
    void login_shouldRejectPendingRegisteredUser() {
        User pendingUser = new User();
        pendingUser.setId(8L);
        pendingUser.setUsername("member_a");
        pendingUser.setTenantId(1L);
        pendingUser.setPassword("encoded-password");
        pendingUser.setStatus(0);
        when(userMapper.selectList(any())).thenReturn(List.of(pendingUser));
        when(roleService.listAssignableRoles(anyLong())).thenReturn(Collections.emptyList());
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

        com.tianji.auth.domain.dto.LoginDTO dto = new com.tianji.auth.domain.dto.LoginDTO();
        dto.setUsername("member_a");
        dto.setPassword("123456");

        assertThrows(BadRequestException.class, () -> authService.login(dto));
        verify(accountRoleMapper, never()).selectList(any());
    }

    @Test
    void register_shouldRejectWhenGenericReferralCodeValidationFails() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setReferralCode("REF2600999");
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.validateReferralCodeInternal("REF2600999", 1L)).thenReturn(false);

        assertThrows(BadRequestException.class, () -> authService.register(dto));
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_shouldRecordGenericReferralRegisterSuccessWhenReferralCodePresent() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setReferralCode("REF2600002");
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.validateReferralCodeInternal("REF2600002", 1L)).thenReturn(true);
        when(userClient.queryCurrentTenantWorkflowConfig(1L)).thenReturn(null);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(101L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(roleMapper.selectOne(any())).thenReturn((Role) null);
        authService.register(dto);

        verify(userClient).markGenericReferralRegisterSuccess(101L, 1L, "REF2600002", "test@example.com", 101L);
    }

    @Test
    void register_shouldUseTargetTenantWorkflowConfigAndSyncLocalUserIntoSameTenant() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setOrganizationId("33");
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.queryCurrentTenantWorkflowConfig(33L)).thenReturn(openRegistrationConfig());
        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(133L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(roleMapper.selectOne(any())).thenReturn((Role) null);
        when(userClient.syncUserOnRegister(any())).thenReturn(233L);

        authService.register(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(33L, userCaptor.getValue().getTenantId());
        assertEquals(1, userCaptor.getValue().getStatus());
        verify(userClient).queryCurrentTenantWorkflowConfig(33L);

        ArgumentCaptor<UserDTO> syncUserCaptor = ArgumentCaptor.forClass(UserDTO.class);
        verify(userClient).syncUserOnRegister(syncUserCaptor.capture());
        assertEquals(33L, syncUserCaptor.getValue().getTenantId());
    }

    @Test
    void register_shouldUseTargetTenantScopeWhenCheckingAndCreatingAuthUser() {
        RegisterDTO dto = buildRegisterDTO();
        dto.setOrganizationId("30");
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);
        when(userClient.queryCurrentTenantWorkflowConfig(30L)).thenReturn(openRegistrationConfig());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(roleMapper.selectOne(any())).thenReturn((Role) null);
        when(userClient.syncUserOnRegister(any())).thenReturn(230L);

        AtomicReference<Long> tenantSeenBySelect = new AtomicReference<>();
        AtomicReference<Long> tenantSeenByInsert = new AtomicReference<>();
        when(userMapper.selectOne(any())).thenAnswer(invocation -> {
            tenantSeenBySelect.set(TenantContext.getTenantId());
            return null;
        });
        doAnswer(invocation -> {
            tenantSeenByInsert.set(TenantContext.getTenantId());
            User user = invocation.getArgument(0);
            user.setId(130L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        authService.register(dto);

        assertEquals(30L, tenantSeenBySelect.get());
        assertEquals(30L, tenantSeenByInsert.get());
        assertEquals(1L, TenantContext.getTenantId());
        assertFalse(TenantContext.isSuperAdmin());
    }

    @Test
    void login_shouldRejectDuplicateUsernameWithoutOrganizationId() {
        User tenantOneUser = new User();
        tenantOneUser.setId(8L);
        tenantOneUser.setUsername("member_a");
        tenantOneUser.setTenantId(1L);
        User tenantTwoUser = new User();
        tenantTwoUser.setId(108L);
        tenantTwoUser.setUsername("member_a");
        tenantTwoUser.setTenantId(2L);
        when(userMapper.selectList(any())).thenReturn(List.of(tenantOneUser, tenantTwoUser));

        com.tianji.auth.domain.dto.LoginDTO dto = new com.tianji.auth.domain.dto.LoginDTO();
        dto.setUsername("member_a");
        dto.setPassword("123456");

        assertThrows(BadRequestException.class, () -> authService.login(dto));
        verify(accountRoleMapper, never()).selectList(any());
    }

    @Test
    void login_shouldRequireEmailVerificationWhenLastLoginIsOlderThanThreeDays() {
        User dormantUser = new User();
        dormantUser.setId(28L);
        dormantUser.setUsername("member_dormant");
        dormantUser.setTenantId(1L);
        dormantUser.setPassword("encoded-password");
        dormantUser.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(dormantUser));
        when(roleService.listAssignableRoles(anyLong())).thenReturn(Collections.emptyList());
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);

        LoginRecord loginRecord = new LoginRecord();
        loginRecord.setUserId(28L);
        loginRecord.setLoginTime(LocalDateTime.now().minusDays(4));
        when(loginRecordMapper.selectOne(any())).thenReturn(loginRecord);

        com.tianji.auth.domain.dto.LoginDTO dto = new com.tianji.auth.domain.dto.LoginDTO();
        dto.setUsername("member_dormant");
        dto.setPassword("123456");

        assertThrows(BadRequestException.class, () -> authService.login(dto));
        verify(jwtTool, never()).createToken(any());
    }

    @Test
    void login_shouldPersistSchemaCompatibleLoginRecordOnSuccess() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        User activeUser = new User();
        activeUser.setId(88L);
        activeUser.setUsername("member_active");
        activeUser.setTenantId(1L);
        activeUser.setPassword("encoded-password");
        activeUser.setStatus(1);
        when(userMapper.selectList(any())).thenReturn(List.of(activeUser));
        when(roleService.listAssignableRoles(anyLong())).thenReturn(Collections.emptyList());
        when(passwordEncoder.matches("123456", "encoded-password")).thenReturn(true);
        when(loginRecordMapper.selectOne(any())).thenReturn(null);
        when(accountRoleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(jwtTool.createToken(any())).thenReturn("token-value");
        when(jwtTool.createRefreshToken(any())).thenReturn("refresh-token-value");

        com.tianji.auth.domain.dto.LoginDTO dto = new com.tianji.auth.domain.dto.LoginDTO();
        dto.setUsername("member_active");
        dto.setPassword("123456");

        LoginVO loginVO = authService.login(dto);

        ArgumentCaptor<LoginRecord> loginRecordCaptor = ArgumentCaptor.forClass(LoginRecord.class);
        verify(loginRecordMapper).insert(loginRecordCaptor.capture());
        assertEquals("member_active", loginRecordCaptor.getValue().getUsername());
        assertEquals("password", loginRecordCaptor.getValue().getLoginType());
        assertEquals(1, loginRecordCaptor.getValue().getLoginStatus());
        assertNotNull(loginRecordCaptor.getValue().getLoginTime());
        assertEquals("token-value", loginVO.getToken());
        assertEquals("refresh-token-value", loginVO.getRefreshToken());
    }

    @Test
    void prepareEmailLogin_shouldReturnTicketAndCandidatesAfterVerification() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);

        EmailLoginCandidateDTO tenantOneCandidate = new EmailLoginCandidateDTO();
        tenantOneCandidate.setAuthUserId(201L);
        tenantOneCandidate.setTenantId(1L);
        tenantOneCandidate.setUsername("admin_a");
        tenantOneCandidate.setDisplayName("绀句富 A");
        tenantOneCandidate.setAvatar("avatars/admin-a.png");

        EmailLoginCandidateDTO tenantTwoCandidate = new EmailLoginCandidateDTO();
        tenantTwoCandidate.setAuthUserId(202L);
        tenantTwoCandidate.setTenantId(2L);
        tenantTwoCandidate.setUsername("admin_a_2");
        tenantTwoCandidate.setDisplayName("绀句富 B");
        tenantTwoCandidate.setAvatar("avatars/admin-b.png");

        when(userClient.queryEmailLoginCandidates(any())).thenReturn(List.of(tenantOneCandidate, tenantTwoCandidate));

        User tenantOneAuthUser = new User();
        tenantOneAuthUser.setId(201L);
        tenantOneAuthUser.setTenantId(1L);
        tenantOneAuthUser.setUsername("admin_a");
        tenantOneAuthUser.setStatus(1);
        tenantOneAuthUser.setIsDeleted(0);

        User tenantTwoAuthUser = new User();
        tenantTwoAuthUser.setId(202L);
        tenantTwoAuthUser.setTenantId(2L);
        tenantTwoAuthUser.setUsername("admin_a_2");
        tenantTwoAuthUser.setStatus(1);
        tenantTwoAuthUser.setIsDeleted(0);

        when(userMapper.selectById(201L)).thenReturn(tenantOneAuthUser);
        when(userMapper.selectById(202L)).thenReturn(tenantTwoAuthUser);

        EmailLoginPrepareDTO dto = new EmailLoginPrepareDTO();
        dto.setEmail("admin@example.com");
        dto.setEmailCode("246810");

        EmailLoginPrepareVO result = authService.prepareEmailLogin(dto);

        assertNotNull(result);
        assertNotNull(result.getLoginTicket());
        assertFalse(result.getLoginTicket().isBlank());
        assertEquals(2, result.getCandidates().size());
        assertEquals(201L, result.getCandidates().get(0).getAuthUserId());
        assertEquals(2L, result.getCandidates().get(1).getTenantId());
        verify(valueOperations).set(anyString(), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    void prepareEmailLogin_shouldReturnOneCandidatePerRoleForSameTenantAccount() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);

        EmailLoginCandidateDTO candidate = new EmailLoginCandidateDTO();
        candidate.setAuthUserId(201L);
        candidate.setTenantId(1L);
        candidate.setUsername("multi_role");
        candidate.setDisplayName("Multi Role");
        when(userClient.queryEmailLoginCandidates(any())).thenReturn(List.of(candidate));

        User authUser = new User();
        authUser.setId(201L);
        authUser.setTenantId(1L);
        authUser.setUsername("multi_role");
        authUser.setStatus(1);
        authUser.setIsDeleted(0);
        when(userMapper.selectById(201L)).thenReturn(authUser);

        AccountRole managerRole = new AccountRole();
        managerRole.setAccountId(201L);
        managerRole.setRoleId(7L);
        AccountRole teacherRole = new AccountRole();
        teacherRole.setAccountId(201L);
        teacherRole.setRoleId(8L);
        when(accountRoleMapper.selectList(any())).thenReturn(List.of(managerRole, teacherRole));

        EmailLoginPrepareDTO dto = new EmailLoginPrepareDTO();
        dto.setEmail("multi@example.com");
        dto.setEmailCode("246810");

        EmailLoginPrepareVO result = authService.prepareEmailLogin(dto);

        assertEquals(2, result.getCandidates().size());
        assertEquals(7L, result.getCandidates().get(0).getRoleId());
        assertEquals(8L, result.getCandidates().get(1).getRoleId());
    }

    @Test
    void confirmEmailLogin_shouldIssueTokenForCandidateStoredInTicket() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rk:auth:email-login:ticket:ticket-123")).thenReturn(
                "{\"email\":\"admin@example.com\",\"candidates\":[{\"authUserId\":201,\"tenantId\":1,\"username\":\"admin_a\",\"displayName\":\"绀句富 A\",\"avatar\":\"avatars/admin-a.png\"}]}"
        );

        User authUser = new User();
        authUser.setId(201L);
        authUser.setTenantId(1L);
        authUser.setUsername("admin_a");
        authUser.setStatus(1);
        authUser.setIsDeleted(0);
        when(userMapper.selectById(201L)).thenReturn(authUser);

        AccountRole accountRole = new AccountRole();
        accountRole.setAccountId(201L);
        accountRole.setRoleId(2L);
        when(accountRoleMapper.selectList(any())).thenReturn(List.of(accountRole));
        when(jwtTool.createToken(any())).thenReturn("email-token");
        when(jwtTool.createRefreshToken(any())).thenReturn("email-refresh-token");
        when(jwtTool.resolveTokenTtl(any())).thenReturn(Duration.ofDays(1));

        EmailLoginConfirmDTO dto = new EmailLoginConfirmDTO();
        dto.setLoginTicket("ticket-123");
        dto.setAuthUserId(201L);
        dto.setTenantId(1L);

        LoginVO result = authService.confirmEmailLogin(dto);

        assertEquals("email-token", result.getToken());
        assertEquals("email-refresh-token", result.getRefreshToken());
        assertEquals(201L, result.getUserId());
        assertEquals("1", result.getOrganizationId());
        verify(valueOperations).set(eq("rk:auth:token:201"), eq("email-token"), eq(Duration.ofDays(1).getSeconds()), eq(TimeUnit.SECONDS));
        verify(stringRedisTemplate).delete("rk:auth:email-login:ticket:ticket-123");

        ArgumentCaptor<LoginRecord> loginRecordCaptor = ArgumentCaptor.forClass(LoginRecord.class);
        verify(loginRecordMapper).insert(loginRecordCaptor.capture());
        assertEquals("email_code", loginRecordCaptor.getValue().getLoginType());
        assertEquals("admin_a", loginRecordCaptor.getValue().getUsername());
    }

    @Test
    void switchTenant_shouldIssueSessionForRequestedRoleWhenSameTenantHasMultipleRoles() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(8L);
        loginUserDTO.setClientType("web");
        when(jwtTool.parseToken("token-value")).thenReturn(loginUserDTO);

        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setUsername("member_a");
        currentUser.setTenantId(1L);
        currentUser.setIdentityKey("member_a");
        when(userMapper.selectById(8L)).thenReturn(currentUser);

        when(userMapper.selectOne(any())).thenReturn(currentUser);

        AccountRole managerRole = new AccountRole();
        managerRole.setAccountId(8L);
        managerRole.setRoleId(7L);
        AccountRole teacherRole = new AccountRole();
        teacherRole.setAccountId(8L);
        teacherRole.setRoleId(8L);
        when(accountRoleMapper.selectList(any())).thenReturn(List.of(managerRole, teacherRole));
        when(jwtTool.createToken(any())).thenReturn("role-token");
        when(jwtTool.createRefreshToken(any())).thenReturn("role-refresh");

        LoginVO result = authService.switchTenant("Bearer token-value", 1L, 8L);

        ArgumentCaptor<LoginUserDTO> loginCaptor = ArgumentCaptor.forClass(LoginUserDTO.class);
        verify(jwtTool).createToken(loginCaptor.capture());
        assertEquals(8L, loginCaptor.getValue().getRoleId());
        assertEquals("role-token", result.getToken());
        assertEquals(8L, result.getRoles().get(0).getRoleId());
    }

    @Test
    void prepareEmailLogin_shouldTemporarilyIgnoreAmbientTenantScopeWhenLoadingAuthUsers() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(userClient.verifyEmailVerificationCode(any())).thenReturn(true);

        EmailLoginCandidateDTO candidate = new EmailLoginCandidateDTO();
        candidate.setAuthUserId(5464L);
        candidate.setTenantId(30L);
        candidate.setUsername("xiaorui");
        candidate.setDisplayName("赵锐");
        when(userClient.queryEmailLoginCandidates(any())).thenReturn(List.of(candidate));

        AtomicReference<Long> tenantSeenByMapper = new AtomicReference<>();
        AtomicReference<Boolean> superAdminSeenByMapper = new AtomicReference<>();
        User authUser = new User();
        authUser.setId(5464L);
        authUser.setTenantId(30L);
        authUser.setUsername("xiaorui");
        authUser.setStatus(1);
        authUser.setIsDeleted(0);
        when(userMapper.selectById(5464L)).thenAnswer(invocation -> {
            tenantSeenByMapper.set(TenantContext.getTenantId());
            superAdminSeenByMapper.set(TenantContext.isSuperAdmin());
            return authUser;
        });

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        EmailLoginPrepareDTO dto = new EmailLoginPrepareDTO();
        dto.setEmail("3505469466@qq.com");
        dto.setEmailCode("123456");

        EmailLoginPrepareVO result = authService.prepareEmailLogin(dto);

        assertEquals(1, result.getCandidates().size());
        assertEquals("xiaorui", result.getCandidates().get(0).getUsername());
        assertEquals(null, tenantSeenByMapper.get());
        assertEquals(true, superAdminSeenByMapper.get());
        assertEquals(1L, TenantContext.getTenantId());
        assertEquals(false, TenantContext.isSuperAdmin());
    }

    @Test
    void confirmEmailLogin_shouldTemporarilyIgnoreAmbientTenantScopeWhenLoadingAuthUser() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("rk:auth:email-login:ticket:ticket-tenant-30")).thenReturn(
                "{\"email\":\"3505469466@qq.com\",\"candidates\":[{\"authUserId\":5464,\"tenantId\":30,\"username\":\"xiaorui\",\"displayName\":\"赵锐\",\"avatar\":null}]}"
        );
        when(accountRoleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(jwtTool.createToken(any())).thenReturn("token-tenant-30");
        when(jwtTool.createRefreshToken(any())).thenReturn("refresh-tenant-30");

        AtomicReference<Long> tenantSeenByMapper = new AtomicReference<>();
        AtomicReference<Boolean> superAdminSeenByMapper = new AtomicReference<>();
        User authUser = new User();
        authUser.setId(5464L);
        authUser.setTenantId(30L);
        authUser.setUsername("xiaorui");
        authUser.setStatus(1);
        authUser.setIsDeleted(0);
        when(userMapper.selectById(5464L)).thenAnswer(invocation -> {
            tenantSeenByMapper.set(TenantContext.getTenantId());
            superAdminSeenByMapper.set(TenantContext.isSuperAdmin());
            return authUser;
        });

        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(false);

        EmailLoginConfirmDTO dto = new EmailLoginConfirmDTO();
        dto.setLoginTicket("ticket-tenant-30");
        dto.setAuthUserId(5464L);
        dto.setTenantId(30L);

        LoginVO result = authService.confirmEmailLogin(dto);

        assertEquals("token-tenant-30", result.getToken());
        assertEquals(null, tenantSeenByMapper.get());
        assertEquals(true, superAdminSeenByMapper.get());
        assertEquals(1L, TenantContext.getTenantId());
        assertEquals(false, TenantContext.isSuperAdmin());
    }

    @Test
    void provisionAdminUser_shouldCreateUserAndAssignRole() {
        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(1L);
        dto.setUsername("admin_created_user");
        dto.setPassword("Passw0rd!");
        dto.setRoleId(9L);
        dto.setStatus(1);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("encoded-pass");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(321L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        Role role = new Role();
        role.setId(9L);
        when(roleMapper.selectById(9L)).thenReturn(role);

        Long authUserId = authService.provisionAdminUser(dto);

        assertEquals(321L, authUserId);
        verify(userMapper).insert(any(User.class));
        ArgumentCaptor<AccountRole> accountRoleCaptor = ArgumentCaptor.forClass(AccountRole.class);
        verify(accountRoleMapper).insert(accountRoleCaptor.capture());
        assertEquals(321L, accountRoleCaptor.getValue().getAccountId());
        assertEquals(9L, accountRoleCaptor.getValue().getRoleId());
    }

    @Test
    void provisionApprovedApplicant_shouldCreateDefaultUserRoleWhenTenantMissingOne() {
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(501L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        when(roleMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            Role role = invocation.getArgument(0);
            role.setId(901L);
            return 1;
        }).when(roleMapper).insert(any(Role.class));

        ApprovedApplicantProvisionDTO dto = new ApprovedApplicantProvisionDTO();
        dto.setTenantId(2L);
        dto.setUsername("tenant2_join_user");
        dto.setEncodedPassword("encoded-password");

        Long authUserId = authService.provisionApprovedApplicant(dto);

        assertEquals(501L, authUserId);
        verify(roleMapper).insert(any(Role.class));
        ArgumentCaptor<AccountRole> accountRoleCaptor = ArgumentCaptor.forClass(AccountRole.class);
        verify(accountRoleMapper).insert(accountRoleCaptor.capture());
        assertEquals(901L, accountRoleCaptor.getValue().getRoleId());
    }

    @Test
    void provisionApprovedApplicant_shouldReuseSourceAccountIdentityAndUsernameForLinkedTenant() {
        User sourceUser = new User();
        sourceUser.setId(8L);
        sourceUser.setUsername("member_a");
        sourceUser.setPassword("encoded-existing");
        sourceUser.setIdentityKey("member_a");
        when(userMapper.selectById(8L)).thenReturn(sourceUser);
        when(userMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(777L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        Role defaultRole = new Role();
        defaultRole.setId(2L);
        when(roleMapper.selectOne(any())).thenReturn(defaultRole);

        ApprovedApplicantProvisionDTO dto = new ApprovedApplicantProvisionDTO();
        dto.setTenantId(2L);
        dto.setUsername("temporary_join_username");
        dto.setSourceAuthUserId(8L);

        Long authUserId = authService.provisionApprovedApplicant(dto);

        assertEquals(777L, authUserId);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals("member_a", userCaptor.getValue().getUsername());
        assertEquals("encoded-existing", userCaptor.getValue().getPassword());
        assertEquals("member_a", userCaptor.getValue().getIdentityKey());
    }

    @Test
    void querySwitchableTenants_shouldReturnLinkedTenantsForSameIdentity() {
        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(8L);
        when(jwtTool.parseToken("token-value")).thenReturn(loginUserDTO);

        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setUsername("member_a");
        currentUser.setTenantId(1L);
        currentUser.setIdentityKey("member_a");
        when(userMapper.selectById(8L)).thenReturn(currentUser);

        User tenantOneUser = new User();
        tenantOneUser.setId(8L);
        tenantOneUser.setUsername("member_a");
        tenantOneUser.setTenantId(1L);
        tenantOneUser.setIdentityKey("member_a");
        User tenantTwoUser = new User();
        tenantTwoUser.setId(108L);
        tenantTwoUser.setUsername("member_a");
        tenantTwoUser.setTenantId(2L);
        tenantTwoUser.setIdentityKey("member_a");
        when(userMapper.selectList(any())).thenReturn(List.of(tenantOneUser, tenantTwoUser));

        AccountRole tenantOneRole = new AccountRole();
        tenantOneRole.setRoleId(2L);
        AccountRole tenantTwoRole = new AccountRole();
        tenantTwoRole.setRoleId(7L);
        when(accountRoleMapper.selectList(any())).thenReturn(
                Collections.singletonList(tenantOneRole),
                Collections.singletonList(tenantTwoRole)
        );

        var result = authService.querySwitchableTenants("Bearer token-value");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getTenantId());
        assertEquals(2L, result.get(0).getRoleId());
        assertEquals(2L, result.get(1).getTenantId());
        assertEquals(7L, result.get(1).getRoleId());
    }

    @Test
    void switchTenant_shouldIssueFreshSessionForTargetTenant() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginUserDTO loginUserDTO = new LoginUserDTO();
        loginUserDTO.setUserId(8L);
        when(jwtTool.parseToken("token-value")).thenReturn(loginUserDTO);

        User currentUser = new User();
        currentUser.setId(8L);
        currentUser.setUsername("member_a");
        currentUser.setTenantId(1L);
        currentUser.setIdentityKey("member_a");
        when(userMapper.selectById(8L)).thenReturn(currentUser);

        User targetUser = new User();
        targetUser.setId(108L);
        targetUser.setUsername("member_a");
        targetUser.setTenantId(2L);
        targetUser.setIdentityKey("member_a");
        targetUser.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(targetUser);

        AccountRole targetRole = new AccountRole();
        targetRole.setAccountId(108L);
        targetRole.setRoleId(7L);
        when(accountRoleMapper.selectList(any())).thenReturn(Collections.singletonList(targetRole));
        when(jwtTool.createToken(any())).thenReturn("new-token");
        when(jwtTool.createRefreshToken(any())).thenReturn("new-refresh-token");
        when(jwtTool.resolveTokenTtl(any())).thenReturn(Duration.ofDays(1));

        var result = authService.switchTenant("Bearer token-value", 2L);

        assertEquals("new-token", result.getToken());
        assertEquals("new-refresh-token", result.getRefreshToken());
        assertEquals("2", result.getOrganizationId());
        assertEquals(108L, result.getUserId());
        assertEquals(1, result.getRoles().size());
        assertEquals(7L, result.getRoles().get(0).getRoleId());
        verify(valueOperations).set(eq("rk:auth:token:108"), eq("new-token"), eq(Duration.ofDays(1).getSeconds()), eq(java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void queryAdminUserById_shouldReturnRoleFromAccountRoleBinding() {
        User user = new User();
        user.setId(66L);
        user.setTenantId(1L);
        user.setUsername("admin_lookup");
        user.setStatus(1);
        when(userMapper.selectById(66L)).thenReturn(user);
        AccountRole accountRole = new AccountRole();
        accountRole.setAccountId(66L);
        accountRole.setRoleId(7L);
        when(accountRoleMapper.selectList(any())).thenReturn(Collections.singletonList(accountRole));

        AdminUserProvisionDTO result = authService.queryAdminUserById(66L);

        assertNotNull(result);
        assertEquals(66L, result.getAuthUserId());
        assertEquals(7L, result.getRoleId());
    }

    @Test
    void updateAdminUserStatus_shouldUpdateStatus() {
        User user = new User();
        user.setId(88L);
        user.setTenantId(1L);
        user.setUsername("admin_status");
        user.setStatus(1);
        when(userMapper.selectById(88L)).thenReturn(user);

        authService.updateAdminUserStatus(88L, 0);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(88L, userCaptor.getValue().getId());
        assertEquals(0, userCaptor.getValue().getStatus());
    }

    @Test
    void resetAdminUserPassword_shouldEncodeAndPersistPassword() {
        User user = new User();
        user.setId(77L);
        user.setTenantId(1L);
        user.setUsername("admin_pwd");
        when(userMapper.selectById(77L)).thenReturn(user);
        when(passwordEncoder.encode("new-default")).thenReturn("encoded-default");

        authService.resetAdminUserPassword(77L, "new-default");

        verify(passwordEncoder).encode("new-default");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(77L, userCaptor.getValue().getId());
        assertEquals("encoded-default", userCaptor.getValue().getPassword());
    }

    @Test
    void updateCurrentUserPassword_shouldValidateOldPasswordAndPersistNewPassword() {
        User user = new User();
        user.setId(78L);
        user.setTenantId(1L);
        user.setUsername("member_pwd");
        user.setPassword("encoded-old");
        when(userMapper.selectById(78L)).thenReturn(user);
        when(passwordEncoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new");

        CurrentUserPasswordUpdateDTO dto = new CurrentUserPasswordUpdateDTO();
        dto.setOldPassword("old-pass");
        dto.setNewPassword("new-pass");

        authService.updateCurrentUserPassword(78L, dto);

        verify(passwordEncoder).matches("old-pass", "encoded-old");
        verify(passwordEncoder).encode("new-pass");
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(78L, userCaptor.getValue().getId());
        assertEquals("encoded-new", userCaptor.getValue().getPassword());
    }

    @Test
    void deleteAdminUser_shouldMarkDeletedAndClearRoleBindings() {
        User user = new User();
        user.setId(55L);
        user.setTenantId(1L);
        user.setUsername("admin_delete");
        user.setIsDeleted(0);
        when(userMapper.selectById(55L)).thenReturn(user);

        authService.deleteAdminUser(55L);

        verify(userMapper).deleteById(55L);
        verify(accountRoleMapper).delete(any());
    }

    private RegisterDTO buildRegisterDTO() {
        RegisterDTO dto = new RegisterDTO();
        dto.setUsername("email_user");
        dto.setPassword("Test@123456");
        dto.setEmail("test@example.com");
        dto.setPhone("13800138000");
        dto.setOrganizationId("1");
        dto.setName("Email User");
        dto.setEmailCode("123456");
        dto.setFormPayload(java.util.Map.of("major", "软件工程", "grade", "大一"));
        dto.setFormPayload(java.util.Map.of(
                "studentId", "20260001",
                "college", "计算机学院",
                "major", "软件工程",
                "grade", "大一"
        ));
        return dto;
    }

    private TenantWorkflowConfigDTO openRegistrationConfig() {
        TenantWorkflowConfigDTO dto = new TenantWorkflowConfigDTO();
        WorkflowPolicyDTO registration = new WorkflowPolicyDTO();
        registration.setOpenRegistration(true);
        registration.setRequireApproval(false);
        registration.setNotifyAdmins(false);
        registration.setNotifyApplicantOnFailure(false);
        registration.setNotifyOnSuccess(true);
        registration.setAdvisorMode("ANY_ONE");
        registration.setDesignatedAdvisorRoleIds(Collections.emptyList());
        dto.setRegistration(registration);
        return dto;
    }
}
