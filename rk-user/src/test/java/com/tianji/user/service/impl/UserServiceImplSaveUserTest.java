package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.api.client.auth.AuthClient;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.ApprovedApplicantProvisionDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.domain.vo.PeopleDomainReconcileResultVO;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ISysOperLogService;
import com.tianji.user.service.IUserDetailService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSaveUserTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IUserDetailService detailService;
    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private AuthClient authClient;
    @Mock
    private ISysOperLogService sysOperLogService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserDetail.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(service, "detailService", detailService);
        ReflectionTestUtils.setField(service, "clubMemberMapper", clubMemberMapper);
        ReflectionTestUtils.setField(service, "authClient", authClient);
        ReflectionTestUtils.setField(service, "sysOperLogService", sysOperLogService);
        lenient().when(passwordEncoder.encode(any())).thenReturn("encoded");
        lenient().when(detailService.getOne(any(Wrapper.class))).thenReturn(null);
        lenient().when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        lenient().when(clubMemberMapper.selectAnyByTenantAndStudentId(any(), any())).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void saveUser_shouldPersistProfileFieldsIntoExistingLocalUser() {
        User existing = new User();
        existing.setId(15L);
        existing.setAuthUserId(99L);
        existing.setTenantId(1L);
        existing.setUsername("member_a");

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        UserDTO dto = new UserDTO();
        dto.setId(99L);
        dto.setUsername("member_a");
        dto.setName("成员A");
        dto.setEmail("member_a@example.com");
        dto.setCellPhone("13800138000");
        dto.setStudentId("20260008");
        dto.setCollege("CS");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("20260008", saved.getStudentId());
        assertEquals("CS", saved.getCollege());
        assertEquals("Software", saved.getMajor());
        assertEquals("2026", saved.getGrade());
    }

    @Test
    void saveUser_shouldPersistProfileFieldsWhenCreatingLocalUser() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        UserDTO dto = new UserDTO();
        dto.setId(108L);
        dto.setUsername("member_b");
        dto.setName("成员B");
        dto.setEmail("member_b@example.com");
        dto.setCellPhone("13800138009");
        dto.setStudentId("20260009");
        dto.setCollege("Math");
        dto.setMajor("Data Science");
        dto.setGrade("2027");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("20260009", saved.getStudentId());
        assertEquals("Math", saved.getCollege());
        assertEquals("Data Science", saved.getMajor());
        assertEquals("2027", saved.getGrade());
    }

    @Test
    void saveUser_shouldExecuteLookupAndInsertWithinProvidedTenantScope() {
        TenantContext.setTenantId(1L);
        List<Long> observedTenantIds = new ArrayList<>();

        when(userMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            observedTenantIds.add(TenantContext.getTenantId());
            return null;
        });
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            observedTenantIds.add(TenantContext.getTenantId());
            User user = invocation.getArgument(0);
            user.setId(3401L);
            return 1;
        });
        when(clubMemberMapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            observedTenantIds.add(TenantContext.getTenantId());
            return null;
        });
        when(clubMemberMapper.insert(any(ClubMember.class))).thenAnswer(invocation -> {
            observedTenantIds.add(TenantContext.getTenantId());
            return 1;
        });

        UserDTO dto = new UserDTO();
        dto.setId(3400L);
        dto.setTenantId(34L);
        dto.setUsername("tenant34_user");
        dto.setName("Tenant 34 User");
        dto.setEmail("tenant34@example.com");
        dto.setCellPhone("13800138340");
        dto.setStudentId("20263400");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        assertFalse(observedTenantIds.isEmpty());
        assertTrue(observedTenantIds.stream().allMatch(tenantId -> Long.valueOf(34L).equals(tenantId)));
    }

    @Test
    void resolveManagedType_shouldNormalizeOrdinaryRosterStaffToStudentWhenStudentIdExists() {
        UserType result = ReflectionTestUtils.invokeMethod(
                service,
                "resolveManagedType",
                UserType.STAFF.getValue(),
                UserType.STAFF,
                "20260010",
                "成员",
                null
        );

        assertEquals(UserType.STUDENT, result);
    }

    @Test
    void resolveManagedType_shouldKeepStaffForManagerPositionWhenStudentIdExists() {
        UserType result = ReflectionTestUtils.invokeMethod(
                service,
                "resolveManagedType",
                UserType.STAFF.getValue(),
                UserType.STAFF,
                "20260011",
                "管理员",
                null
        );

        assertEquals(UserType.STAFF, result);
    }

    @Test
    void resolveManagedMemberPositionOverride_shouldNotForceStaffIntoOrdinaryMember() {
        User localUser = new User();
        localUser.setType(UserType.STAFF);

        String result = ReflectionTestUtils.invokeMethod(
                service,
                "resolveManagedMemberPositionOverride",
                localUser,
                "20260012",
                null
        );

        assertEquals(null, result);
    }

    @Test
    void shouldSyncManagedMember_shouldAcceptTeacherLedgerRowsWhenTheyHaveRealStudentId() {
        User localUser = new User();
        localUser.setType(UserType.TEACHER);

        Boolean result = ReflectionTestUtils.invokeMethod(service, "shouldSyncManagedMember", localUser, "20260013");

        assertTrue(Boolean.TRUE.equals(result));
    }

    @Test
    void syncManagedMemberFromUser_shouldSkipTeacherWithoutSyntheticLedger() {
        User localUser = new User();
        localUser.setId(701L);
        localUser.setTenantId(2L);
        localUser.setUsername("legacy_teacher");
        localUser.setRealName("历史指导老师");
        localUser.setEmail("legacy-teacher@example.com");
        localUser.setType(UserType.TEACHER);

        Object result = ReflectionTestUtils.invokeMethod(service, "syncManagedMemberFromUser", localUser);

        assertEquals("跳过可选同步账号：legacy_teacher", ReflectionTestUtils.getField(result, "detail"));
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
        verify(clubMemberMapper, never()).deleteById(anyLong());
    }

    @Test
    void syncManagedMemberFromUser_shouldSkipSuperAdminWithoutSyntheticLedger() {
        User localUser = new User();
        localUser.setId(703L);
        localUser.setAuthUserId(1L);
        localUser.setTenantId(1L);
        localUser.setUsername("admin_a");
        localUser.setRealName("系统管理员");
        localUser.setEmail("admin@example.com");
        localUser.setType(UserType.STAFF);

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(1L);
        when(authClient.queryAdminUserById(1L)).thenReturn(authUser);

        Object result = ReflectionTestUtils.invokeMethod(service, "syncManagedMemberFromUser", localUser);

        assertEquals("跳过可选同步账号：admin_a", ReflectionTestUtils.getField(result, "detail"));
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
        verify(clubMemberMapper, never()).deleteById(anyLong());
    }

    @Test
    void syncManagedMemberFromUser_shouldUsePlaceholderEmailForClubManagerWithoutEmail() {
        User localUser = new User();
        localUser.setId(702L);
        localUser.setTenantId(30L);
        localUser.setUsername("legacy_admin_without_email");
        localUser.setRealName("Legacy Admin");
        localUser.setType(UserType.STAFF);
        localUser.setStatus(UserStatus.NORMAL);

        ReflectionTestUtils.invokeMethod(service, "syncManagedMemberFromUser", localUser);

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals("LOCAL-702", memberCaptor.getValue().getStudentId());
        assertEquals("no-email-30-local-702@invalid.local", memberCaptor.getValue().getEmail());
    }

    @Test
    void buildScopedUserReconcileQuery_shouldOnlyIncludeNormalUsers() {
        TenantContext.setTenantId(30L);

        Wrapper<User> query = ReflectionTestUtils.invokeMethod(service, "buildScopedUserReconcileQuery");

        assertTrue(query.getSqlSegment().contains("status"));
    }

    @Test
    void reconcileManagedUserFromMember_shouldNotMatchSyntheticMemberBySharedEmail() {
        ClubMember member = new ClubMember();
        member.setTenantId(30L);
        member.setStudentId("AUTH-5817");
        member.setEmail("shared-admin@example.com");
        member.setName("Club Manager");
        member.setPosition("管理员");

        User wrongUserWithSameEmail = new User();
        wrongUserWithSameEmail.setId(800L);
        wrongUserWithSameEmail.setAuthUserId(5809L);
        wrongUserWithSameEmail.setTenantId(30L);
        wrongUserWithSameEmail.setUsername("admin_c");
        wrongUserWithSameEmail.setEmail("shared-admin@example.com");
        wrongUserWithSameEmail.setType(UserType.STAFF);
        wrongUserWithSameEmail.setStatus(UserStatus.NORMAL);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, wrongUserWithSameEmail, wrongUserWithSameEmail);

        ReflectionTestUtils.invokeMethod(service, "reconcileManagedUserFromMember", member);

        verify(userMapper, never()).updateById(any(User.class));
        verify(authClient, never()).provisionApprovedApplicant(any());
    }

    @Test
    void reconcileManagedUserFromMember_shouldExplainSyntheticMemberSkipAsNonError() {
        ClubMember member = new ClubMember();
        member.setTenantId(30L);
        member.setStudentId("AUTH-5817");
        member.setEmail("manager@example.com");
        member.setName("Club Manager");
        member.setPosition("管理员");

        Object result = ReflectionTestUtils.invokeMethod(service, "reconcileManagedUserFromMember", member);

        assertTrue((Boolean) ReflectionTestUtils.getField(result, "skippedSyntheticMember"));
        assertEquals("合成管理人员台账已跳过（非异常）：tenantId=30，studentId=AUTH-5817",
                ReflectionTestUtils.getField(result, "detail"));
        verify(userMapper, never()).insert(any(User.class));
        verify(authClient, never()).provisionApprovedApplicant(any());
    }

    @Test
    void peopleDomainReconcileSummary_shouldExplainSyntheticMemberSkipAsAuditInfo() {
        PeopleDomainReconcileResultVO result = new PeopleDomainReconcileResultVO();
        result.setSkippedSyntheticMemberCount(2);
        result.setScannedAccountCount(10);
        result.setScannedMemberCount(20);

        result.finishSummary();

        assertTrue(result.getSummary().contains("合成管理人员台账 2（非异常）"));
        assertFalse(result.getSummary().contains("跳过合成成员 2"));
    }

    @Test
    void shouldSyncManagedMember_shouldAcceptLegacyUnknownTypeWhenStudentIdExists() {
        User localUser = new User();
        localUser.setType(null);

        Boolean result = ReflectionTestUtils.invokeMethod(service, "shouldSyncManagedMember", localUser, "20260014");

        assertTrue(Boolean.TRUE.equals(result));
    }

    @Test
    void saveUser_shouldCreateManagedMemberLedgerForStudentAccount() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        UserDTO dto = new UserDTO();
        dto.setId(109L);
        dto.setUsername("member_c");
        dto.setName("成员C");
        dto.setEmail("member_c@example.com");
        dto.setCellPhone("13800138010");
        dto.setStudentId("20260010");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        ClubMember savedMember = memberCaptor.getValue();
        assertEquals("20260010", savedMember.getStudentId());
        assertEquals("成员C", savedMember.getName());
        assertEquals("member_c@example.com", savedMember.getEmail());
        assertEquals("Software", savedMember.getMajor());
    }

    @Test
    void saveUser_shouldRestoreSoftDeletedManagedMemberLedger() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        ClubMember deletedMember = new ClubMember();
        deletedMember.setId(88L);
        deletedMember.setTenantId(1L);
        deletedMember.setStudentId("20260088");
        deletedMember.setEmail("restore_legacy@example.com");
        deletedMember.setPhone("13800138088");
        deletedMember.setIsDeleted(1);

        when(clubMemberMapper.selectAnyByTenantAndStudentId(1L, "20260088")).thenReturn(deletedMember);

        UserDTO dto = new UserDTO();
        dto.setId(188L);
        dto.setTenantId(1L);
        dto.setUsername("member_restore");
        dto.setName("Restore Member");
        dto.setStudentId("20260088");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).updateIncludingDeleted(memberCaptor.capture());
        assertEquals(0, memberCaptor.getValue().getIsDeleted());
        assertEquals("20260088", memberCaptor.getValue().getStudentId());
        assertEquals("restore_legacy@example.com", memberCaptor.getValue().getEmail());
        assertEquals("13800138088", memberCaptor.getValue().getPhone());
    }

    @Test
    void saveUser_shouldApplyProvidedJoinDateToLocalUserAndManagedMember() {
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        LocalDateTime joinDate = LocalDateTime.of(2026, 4, 30, 16, 45, 12);

        UserDTO dto = new UserDTO();
        dto.setId(110L);
        dto.setUsername("member_join");
        dto.setName("Join Date Member");
        dto.setEmail("member_join@example.com");
        dto.setCellPhone("13800138011");
        dto.setStudentId("20260011");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setJoinDate(joinDate);
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(joinDate.toLocalDate().atStartOfDay(), userCaptor.getValue().getJoinTime());

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals(joinDate.toLocalDate().atStartOfDay(), memberCaptor.getValue().getJoinDate());
    }

    @Test
    void saveUser_shouldRetirePreviousManagedMemberLedgerWhenStudentIdChanges() {
        User existing = new User();
        existing.setId(15L);
        existing.setAuthUserId(99L);
        existing.setTenantId(1L);
        existing.setUsername("member_a");
        existing.setStudentId("20260008");
        existing.setType(UserType.STUDENT);

        ClubMember previousMember = new ClubMember();
        previousMember.setId(41L);
        previousMember.setTenantId(1L);
        previousMember.setStudentId("20260008");
        previousMember.setIsDeleted(0);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(clubMemberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null, null, previousMember);

        UserDTO dto = new UserDTO();
        dto.setId(99L);
        dto.setUsername("member_a");
        dto.setName("鎴愬憳A");
        dto.setEmail("member_a@example.com");
        dto.setCellPhone("13800138000");
        dto.setStudentId("20269999");
        dto.setCollege("CS");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        verify(clubMemberMapper).deleteById(41L);
        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals("20269999", memberCaptor.getValue().getStudentId());
        verify(clubMemberMapper, never()).deleteById(eq(0L));
    }

    @Test
    void upsertManagedLocalUser_shouldRetirePreviousManagedMemberLedgerWhenStudentIdChanges() {
        User existing = new User();
        existing.setId(16L);
        existing.setAuthUserId(108L);
        existing.setTenantId(1L);
        existing.setUsername("member_d");
        existing.setStudentId("20260016");
        existing.setType(UserType.STUDENT);

        ClubMember previousMember = new ClubMember();
        previousMember.setId(42L);
        previousMember.setTenantId(1L);
        previousMember.setStudentId("20260016");
        previousMember.setIsDeleted(0);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(clubMemberMapper.selectOne(any(Wrapper.class)))
                .thenReturn(null, null, null, previousMember);

        AdminUserProvisionDTO dto = new AdminUserProvisionDTO();
        dto.setTenantId(1L);
        dto.setUsername("member_d");
        dto.setName("鎴愬憳D");
        dto.setEmail("member_d@example.com");
        dto.setCellPhone("13800138016");
        dto.setStudentId("20269998");
        dto.setCollege("CS");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());
        dto.setStatus(1);

        ReflectionTestUtils.invokeMethod(service, "upsertManagedLocalUser", 108L, dto, null);

        verify(clubMemberMapper).deleteById(42L);
        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals("20269998", memberCaptor.getValue().getStudentId());
    }

    @Test
    void saveUser_shouldReuseExistingTenantUserMatchedByStudentIdInsteadOfCreatingDuplicate() {
        TenantContext.setTenantId(1L);

        User existing = new User();
        existing.setId(31L);
        existing.setTenantId(1L);
        existing.setUsername("legacy_member");
        existing.setEmail("legacy_member@example.com");
        existing.setStudentId("20261101");
        existing.setType(UserType.STUDENT);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);

        UserDTO dto = new UserDTO();
        dto.setId(208L);
        dto.setTenantId(1L);
        dto.setUsername("fresh_member");
        dto.setName("Student Merge");
        dto.setEmail("legacy_member@example.com");
        dto.setCellPhone("13800138111");
        dto.setStudentId("20261101");
        dto.setCollege("Computer Science");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        verify(userMapper, never()).insert(any(User.class));
        assertEquals(31L, userCaptor.getValue().getId());
        assertEquals(208L, userCaptor.getValue().getAuthUserId());
        assertEquals("20261101", userCaptor.getValue().getStudentId());
        assertEquals("legacy_member@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void saveUser_shouldReuseExistingTenantUserMatchedByEmailWhenStudentIdMissing() {
        TenantContext.setTenantId(1L);

        User existing = new User();
        existing.setId(32L);
        existing.setTenantId(1L);
        existing.setUsername("email_member");
        existing.setEmail("email_member@example.com");
        existing.setType(UserType.STUDENT);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);

        UserDTO dto = new UserDTO();
        dto.setId(209L);
        dto.setTenantId(1L);
        dto.setUsername("email_member_new");
        dto.setName("Email Merge");
        dto.setEmail("email_member@example.com");
        dto.setCellPhone("13800138112");
        dto.setCollege("Math");
        dto.setMajor("Data");
        dto.setGrade("2027");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        verify(userMapper, never()).insert(any(User.class));
        assertEquals(32L, userCaptor.getValue().getId());
        assertEquals(209L, userCaptor.getValue().getAuthUserId());
        assertEquals("email_member@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    void saveUser_shouldKeepExistingUsernameWhenMergingExistingLocalUser() {
        TenantContext.setTenantId(1L);

        User existing = new User();
        existing.setId(33L);
        existing.setTenantId(1L);
        existing.setUsername("xiaorui");
        existing.setEmail("legacy@example.com");
        existing.setStudentId("LEGACY-T1-U5533");
        existing.setType(UserType.STUDENT);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);

        UserDTO dto = new UserDTO();
        dto.setId(210L);
        dto.setTenantId(1L);
        dto.setUsername("z13039811650@163.com");
        dto.setName("Xiaorui Merge");
        dto.setEmail("z13039811650@163.com");
        dto.setStudentId("LEGACY-T1-U5533");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals("xiaorui", userCaptor.getValue().getUsername());
        assertEquals("z13039811650@163.com", userCaptor.getValue().getEmail());
    }

    @Test
    void saveUser_shouldUpdateExistingUserDetailByDetailPrimaryKeyInsteadOfUserId() {
        TenantContext.setTenantId(1L);

        User existing = new User();
        existing.setId(45L);
        existing.setTenantId(1L);
        existing.setUsername("legacy_detail");
        existing.setEmail("legacy_detail@example.com");
        existing.setStudentId("20261145");
        existing.setType(UserType.STUDENT);

        UserDetail existingDetail = new UserDetail();
        existingDetail.setId(145L);
        existingDetail.setUserId(45L);
        existingDetail.setTenantId(1L);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);
        when(detailService.getOne(any(Wrapper.class))).thenReturn(existingDetail);

        UserDTO dto = new UserDTO();
        dto.setId(245L);
        dto.setTenantId(1L);
        dto.setUsername("legacy_detail_new");
        dto.setName("Detail Merge");
        dto.setEmail("legacy_detail@example.com");
        dto.setCellPhone("13800138145");
        dto.setStudentId("20261145");
        dto.setCollege("Computer Science");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<UserDetail> detailCaptor = ArgumentCaptor.forClass(UserDetail.class);
        verify(detailService).updateById(detailCaptor.capture());
        assertEquals(145L, detailCaptor.getValue().getId());
        assertEquals(45L, detailCaptor.getValue().getUserId());
    }

    @Test
    void reconcilePeopleDomainForCurrentScope_shouldCreateMemberForStudentUserWithoutMember() {
        TenantContext.setTenantId(1L);

        User localUser = new User();
        localUser.setId(41L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(601L);
        localUser.setUsername("scoped_member");
        localUser.setRealName("Scoped Member");
        localUser.setEmail("scoped_member@example.com");
        localUser.setCellPhone("13800138113");
        localUser.setStudentId("20261103");
        localUser.setMajor("Software");
        localUser.setGrade("2026");
        localUser.setType(UserType.STUDENT);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(localUser));
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());

        PeopleDomainReconcileResultVO result = service.reconcilePeopleDomainForCurrentScope();

        ArgumentCaptor<ClubMember> memberCaptor = ArgumentCaptor.forClass(ClubMember.class);
        verify(clubMemberMapper).insert(memberCaptor.capture());
        assertEquals("20261103", memberCaptor.getValue().getStudentId());
        assertEquals("scoped_member@example.com", memberCaptor.getValue().getEmail());
        assertEquals(1L, result.getScannedAccountCount());
        assertEquals(1L, result.getCreatedMemberCount());
        assertEquals(0L, result.getSkippedAccountWithoutStudentIdCount());
        assertTrue(result.getSummary().contains("新增成员台账 1"));
        verify(sysOperLogService).recordOperation(
                eq("修复账号/成员差额"),
                eq("UserServiceImpl.reconcilePeopleDomainForCurrentScope"),
                eq("POST"),
                eq(2),
                any(),
                eq("/users/reconcile/people-domain"),
                eq("server"),
                any(),
                eq(0),
                eq(null),
                anyLong()
        );
    }

    @Test
    void reconcilePeopleDomainForCurrentScope_shouldCreateLocalUserForMemberWithoutUser() {
        TenantContext.setTenantId(1L);

        ClubMember member = new ClubMember();
        member.setId(51L);
        member.setTenantId(1L);
        member.setName("Member Only");
        member.setStudentId("20261104");
        member.setEmail("member_only@example.com");
        member.setPhone("13800138114");
        member.setMajor("Software");
        member.setGrade("2026");
        member.setPosition("成员");

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(member));
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, null, null);
        when(authClient.provisionApprovedApplicant(any(ApprovedApplicantProvisionDTO.class))).thenReturn(701L);

        PeopleDomainReconcileResultVO result = service.reconcilePeopleDomainForCurrentScope();

        ArgumentCaptor<ApprovedApplicantProvisionDTO> authCaptor = ArgumentCaptor.forClass(ApprovedApplicantProvisionDTO.class);
        verify(authClient).provisionApprovedApplicant(authCaptor.capture());
        assertEquals(1L, authCaptor.getValue().getTenantId());
        assertEquals("member_only@example.com", authCaptor.getValue().getUsername());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertEquals(701L, userCaptor.getValue().getAuthUserId());
        assertEquals("20261104", userCaptor.getValue().getStudentId());
        assertEquals("member_only@example.com", userCaptor.getValue().getEmail());
        assertEquals("Software", userCaptor.getValue().getMajor());
        verify(clubMemberMapper, never()).deleteById(anyLong());
        assertEquals(1L, result.getScannedMemberCount());
        assertEquals(1L, result.getCreatedUserCount());
        assertEquals(1L, result.getCreatedAuthAccountCount());
        assertTrue(result.getSummary().contains("新增用户账号 1"));
    }

    @Test
    void reconcilePeopleDomainForCurrentScope_shouldReportSkippedStudentAccountsWithoutStudentId() {
        TenantContext.setTenantId(1L);

        User localUser = new User();
        localUser.setId(71L);
        localUser.setTenantId(1L);
        localUser.setAuthUserId(801L);
        localUser.setUsername("missing_student_id");
        localUser.setRealName("Missing Student Id");
        localUser.setEmail("missing_student_id@example.com");
        localUser.setType(UserType.STUDENT);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of(localUser));
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(java.util.List.of());

        PeopleDomainReconcileResultVO result = service.reconcilePeopleDomainForCurrentScope();

        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
        assertEquals(1L, result.getScannedAccountCount());
        assertEquals(1L, result.getSkippedAccountWithoutStudentIdCount());
        assertTrue(result.getDetails().stream().anyMatch(item -> item.contains("missing_student_id")));
    }

    @Test
    void saveUser_shouldSkipDuplicateCellPhoneWhenCreatingLocalUserFromMember() {
        TenantContext.setTenantId(1L);

        User phoneOwner = new User();
        phoneOwner.setId(88L);
        phoneOwner.setTenantId(1L);
        phoneOwner.setCellPhone("13800000000");

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, null, null);
        when(userMapper.selectAnyByTenantAndCellPhone(1L, "13800000000")).thenReturn(phoneOwner);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(420L);
            return 1;
        });

        UserDTO dto = new UserDTO();
        dto.setId(808L);
        dto.setTenantId(1L);
        dto.setUsername("member_phone_conflict");
        dto.setName("Member Phone Conflict");
        dto.setEmail("member-phone-conflict@example.com");
        dto.setCellPhone("13800000000");
        dto.setStudentId("20260808");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertNull(userCaptor.getValue().getCellPhone());
    }

    @Test
    void saveUser_shouldSkipCellPhoneOccupiedBySoftDeletedHistoryRowWhenUpdatingLocalUser() {
        TenantContext.setTenantId(1L);

        User existing = new User();
        existing.setId(5533L);
        existing.setTenantId(1L);
        existing.setUsername("xiaorui");
        existing.setEmail("z13039811650@163.com");
        existing.setStudentId("2022593031");
        existing.setType(UserType.STUDENT);

        User softDeletedPhoneOwner = new User();
        softDeletedPhoneOwner.setId(99L);
        softDeletedPhoneOwner.setTenantId(1L);
        softDeletedPhoneOwner.setCellPhone("13800000000");
        softDeletedPhoneOwner.setIsDeleted(1);

        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(null, existing);
        when(userMapper.selectAnyByTenantAndCellPhone(1L, "13800000000")).thenReturn(softDeletedPhoneOwner);

        UserDTO dto = new UserDTO();
        dto.setId(5455L);
        dto.setTenantId(1L);
        dto.setUsername("xiaorui");
        dto.setName("Xiaorui");
        dto.setEmail("z13039811650@163.com");
        dto.setCellPhone("13800000000");
        dto.setStudentId("2022593031");
        dto.setMajor("Software");
        dto.setGrade("2026");
        dto.setType(UserType.STUDENT.getValue());

        service.saveUser(dto);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertNull(userCaptor.getValue().getCellPhone());
    }
}
