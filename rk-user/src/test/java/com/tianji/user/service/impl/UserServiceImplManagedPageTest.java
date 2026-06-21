package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.dto.auth.AdminUserProvisionDTO;
import com.tianji.api.dto.auth.RoleDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.ICodeService;
import com.tianji.user.service.IUserDetailService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplManagedPageTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ICodeService codeService;
    @Mock
    private com.tianji.api.client.auth.AuthClient authClient;
    @Mock
    private IUserDetailService detailService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        org.springframework.test.util.ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void queryUserByPage_shouldApplyExplicitTenantFilterForAllTenantManagers() {
        TenantContext.setTenantId(1L);
        Long scopedTenantId = 222L;

        User user = new User();
        user.setId(10L);
        user.setAuthUserId(1000L);
        user.setTenantId(2L);
        user.setUsername("tenant-user");
        user.setRealName("Tenant User");
        user.setCellPhone("13800138000");
        user.setEmail("tenant-user@example.com");
        user.setType(UserType.STUDENT);
        user.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(7L);
        when(authClient.queryAdminUserById(1000L)).thenReturn(authUser);
        when(authClient.queryRoleById(7L)).thenReturn(new RoleDTO().setId(7L).setName("manager"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null, scopedTenantId);

        ArgumentCaptor<Wrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapperCaptor.capture());

        assertTrue(wrapperCaptor.getValue().getExpression().getNormal().size() > 0);
        assertEquals(1, result.getRecords().size());
        assertEquals(2L, result.getRecords().get(0).getTenantId());
        assertEquals("manager", result.getRecords().get(0).getRoleName());
    }

    @Test
    void queryUserByPage_shouldNotRunGlobalReconcileSweepBeforePaging() {
        TenantContext.setTenantId(1L);

        User user = new User();
        user.setId(99L);
        user.setAuthUserId(1999L);
        user.setTenantId(1L);
        user.setUsername("page-only-user");
        user.setRealName("Page Only User");
        user.setCellPhone("13800138999");
        user.setEmail("page-only@example.com");
        user.setType(UserType.STUDENT);
        user.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(2L);
        when(authClient.queryAdminUserById(1999L)).thenReturn(authUser);
        when(authClient.queryRoleById(2L)).thenReturn(new RoleDTO().setId(2L).setName("member"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null);

        assertEquals(1, result.getRecords().size());
        verify(userMapper).selectPage(any(Page.class), any(Wrapper.class));
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void queryUserByPage_shouldUseAllActiveTenantsForSuperAdminView() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User user = new User();
        user.setId(10L);
        user.setAuthUserId(1000L);
        user.setTenantId(2L);
        user.setUsername("tenant-user");
        user.setRealName("Tenant User");
        user.setCellPhone("13800138000");
        user.setEmail("tenant-user@example.com");
        user.setType(UserType.STUDENT);
        user.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(user));
            page.setTotal(1);
            return page;
        });
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(7L);
        when(authClient.queryAdminUserById(1000L)).thenReturn(authUser);
        when(authClient.queryRoleById(7L)).thenReturn(new RoleDTO().setId(7L).setName("manager"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null);

        ArgumentCaptor<Wrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapperCaptor.capture());

        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment().toUpperCase();
        verify(tenantMapper).selectList(any(Wrapper.class));
        assertTrue(sqlSegment.contains("IN"), "super-admin user page should expand to all active tenants");
        assertEquals(1, result.getRecords().size());
    }

    @Test
    void queryUserByPage_shouldNotBackfillClubMemberForScopedTeacherUserWithStudentId() {
        TenantContext.setTenantId(2L);

        User teacher = new User();
        teacher.setId(16L);
        teacher.setAuthUserId(1006L);
        teacher.setTenantId(2L);
        teacher.setUsername("teacher-sync");
        teacher.setRealName("Teacher Sync");
        teacher.setCellPhone("13800138006");
        teacher.setEmail("teacher-sync@example.com");
        teacher.setStudentId("2026002006");
        teacher.setMajor("Computer Science");
        teacher.setGrade("2026");
        teacher.setType(UserType.TEACHER);
        teacher.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(teacher));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(8L);
        when(authClient.queryAdminUserById(1006L)).thenReturn(authUser);
        when(authClient.queryRoleById(8L)).thenReturn(new RoleDTO().setId(8L).setName("teacher"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null);

        assertEquals(1, result.getRecords().size());
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
        verify(clubMemberMapper, never()).updateById(any(ClubMember.class));
        verify(clubMemberMapper, never()).deleteById(any());
        assertEquals("teacher", result.getRecords().get(0).getRoleName());
    }

    @Test
    void queryUserByPage_shouldNotMutateLegacyClubMemberForScopedTeacherUserDuringReadOnlyPageQuery() {
        TenantContext.setTenantId(2L);

        User teacher = new User();
        teacher.setId(17L);
        teacher.setAuthUserId(1007L);
        teacher.setTenantId(2L);
        teacher.setUsername("teacher-legacy");
        teacher.setRealName("Teacher Legacy");
        teacher.setCellPhone("13800138007");
        teacher.setEmail("teacher-legacy@example.com");
        teacher.setStudentId("2026002007");
        teacher.setMajor("Computer Science");
        teacher.setGrade("2026");
        teacher.setType(UserType.TEACHER);
        teacher.setStatus(UserStatus.NORMAL);

        ClubMember legacyMember = new ClubMember();
        legacyMember.setId(27L);
        legacyMember.setTenantId(2L);
        legacyMember.setStudentId("2026002007");
        legacyMember.setPosition("成员");

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(teacher));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(8L);
        when(authClient.queryAdminUserById(1007L)).thenReturn(authUser);
        when(authClient.queryRoleById(8L)).thenReturn(new RoleDTO().setId(8L).setName("teacher"));

        userService.queryUserByPage(1, 10, null, null, null);

        verify(clubMemberMapper, never()).deleteById(27L);
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
    }

    @Test
    void queryUserByPage_shouldAvoidGlobalReconcileSweepWhenSuperAdminViewsAggregateData() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User tenantOneUser = new User();
        tenantOneUser.setId(21L);
        tenantOneUser.setAuthUserId(1021L);
        tenantOneUser.setTenantId(1L);
        tenantOneUser.setUsername("super-admin-scope");
        tenantOneUser.setRealName("Scope User");
        tenantOneUser.setCellPhone("13800138021");
        tenantOneUser.setEmail("scope-user@example.com");
        tenantOneUser.setStudentId("2026002021");
        tenantOneUser.setMajor("Computer Science");
        tenantOneUser.setGrade("2026");
        tenantOneUser.setType(UserType.STUDENT);
        tenantOneUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(tenantOneUser));
            page.setTotal(1);
            return page;
        });
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(1L);
        when(authClient.queryAdminUserById(1021L)).thenReturn(authUser);
        when(authClient.queryRoleById(1L)).thenReturn(new RoleDTO().setId(1L).setName("super-admin"));

        userService.queryUserByPage(1, 10, null, null, null);

        ArgumentCaptor<Wrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment().toUpperCase();

        assertTrue(sqlSegment.contains("IN"), "super-admin page query should aggregate active tenants by default");
        verify(userMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void queryUserByPage_shouldFilterScopedResultsByRoleIdAfterRoleEnrichment() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User manager = new User();
        manager.setId(51L);
        manager.setAuthUserId(2051L);
        manager.setTenantId(1L);
        manager.setUsername("manager-reviewer");
        manager.setRealName("Manager Reviewer");
        manager.setType(UserType.STUDENT);
        manager.setStatus(UserStatus.NORMAL);

        User teacher = new User();
        teacher.setId(52L);
        teacher.setAuthUserId(2052L);
        teacher.setTenantId(1L);
        teacher.setUsername("teacher-reviewer");
        teacher.setRealName("Teacher Reviewer");
        teacher.setType(UserType.TEACHER);
        teacher.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(manager, teacher));
            page.setTotal(2);
            return page;
        });

        AdminUserProvisionDTO managerAuth = new AdminUserProvisionDTO();
        managerAuth.setRoleId(7L);
        AdminUserProvisionDTO teacherAuth = new AdminUserProvisionDTO();
        teacherAuth.setRoleId(8L);
        when(authClient.queryAdminUserById(2051L)).thenReturn(managerAuth);
        when(authClient.queryAdminUserById(2052L)).thenReturn(teacherAuth);
        when(authClient.queryRoleById(7L)).thenReturn(new RoleDTO().setId(7L).setName("manager"));
        when(authClient.queryRoleById(8L)).thenReturn(new RoleDTO().setId(8L).setName("teacher"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null, 1L, 7L);

        assertEquals(1, result.getRecords().size());
        assertEquals(51L, result.getRecords().get(0).getId());
        assertEquals(1, result.getTotal());
    }

    @Test
    void queryUsersByAuthIds_shouldOnlyReturnCurrentTenantProfiles() {
        TenantContext.setTenantId(2L);

        User tenantTwoUser = new User();
        tenantTwoUser.setId(82L);
        tenantTwoUser.setAuthUserId(2082L);
        tenantTwoUser.setTenantId(2L);
        tenantTwoUser.setUsername("tenant-two-commenter");
        tenantTwoUser.setRealName("Tenant Two Commenter");
        tenantTwoUser.setType(UserType.STUDENT);
        tenantTwoUser.setStatus(UserStatus.NORMAL);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tenantTwoUser));

        List<UserDTO> result = userService.queryUsersByAuthIds(List.of(2082L, 2082L));

        ArgumentCaptor<Wrapper<User>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectList(wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getCustomSqlSegment().toUpperCase();
        assertTrue(sqlSegment.contains("TENANT_ID"), "auth-id profile lookup must stay in current tenant");
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getTenantId());
    }

    @Test
    void queryUserByPage_shouldExposeLegacyStaffAsManagerReviewerWhenAuthRoleIsMissing() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User manager = new User();
        manager.setId(71L);
        manager.setTenantId(1L);
        manager.setUsername("legacy-manager");
        manager.setRealName("Legacy Manager");
        manager.setType(UserType.STAFF);
        manager.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(manager));
            page.setTotal(1);
            return page;
        });

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null, 1L, 7L);

        assertEquals(1, result.getRecords().size());
        assertEquals(71L, result.getRecords().get(0).getId());
        assertEquals(7L, result.getRecords().get(0).getRoleId());
        assertEquals(1, result.getTotal());
        verifyNoInteractions(authClient);
    }

    @Test
    void queryUserByPage_shouldFindRoleReviewerWhenReviewerIsNotOnFirstUnfilteredPage() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        List<User> firstPageStudents = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> {
                    User student = new User();
                    student.setId((long) index);
                    student.setTenantId(1L);
                    student.setUsername("student-" + index);
                    student.setType(UserType.STUDENT);
                    student.setStatus(UserStatus.NORMAL);
                    return student;
                })
                .collect(java.util.stream.Collectors.toList());

        User manager = new User();
        manager.setId(71L);
        manager.setTenantId(1L);
        manager.setUsername("late-page-manager");
        manager.setRealName("Late Page Manager");
        manager.setType(UserType.STAFF);
        manager.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setTotal(11);
            if (page.getSize() >= 11) {
                List<User> allUsers = new java.util.ArrayList<>(firstPageStudents);
                allUsers.add(manager);
                page.setRecords(allUsers);
            } else {
                page.setRecords(page.getCurrent() == 1 ? firstPageStudents : List.of(manager));
            }
            return page;
        });

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null, 1L, 7L);

        assertEquals(1, result.getRecords().size());
        assertEquals(71L, result.getRecords().get(0).getId());
        assertEquals(7L, result.getRecords().get(0).getRoleId());
        assertEquals(1, result.getTotal());
        verifyNoInteractions(authClient);
    }

    @Test
    void queryUserByPage_shouldExposeLegacyTeacherAsTeacherReviewerWhenAuthRoleIsMissing() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User teacher = new User();
        teacher.setId(72L);
        teacher.setTenantId(1L);
        teacher.setUsername("legacy-teacher");
        teacher.setRealName("Legacy Teacher");
        teacher.setType(UserType.TEACHER);
        teacher.setStatus(UserStatus.NORMAL);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(teacher));
            page.setTotal(1);
            return page;
        });

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null, 1L, 8L);

        assertEquals(1, result.getRecords().size());
        assertEquals(72L, result.getRecords().get(0).getId());
        assertEquals(8L, result.getRecords().get(0).getRoleId());
        assertEquals(1, result.getTotal());
        verifyNoInteractions(authClient);
    }

    private RKTenant activeTenant(Long id) {
        RKTenant tenant = new RKTenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        return tenant;
    }

    @Test
    void queryUserByPage_shouldSkipMemberUpdateWhenScopedMemberAlreadyMatchesUser() {
        TenantContext.setTenantId(1L);

        User memberUser = new User();
        memberUser.setId(41L);
        memberUser.setAuthUserId(1041L);
        memberUser.setTenantId(1L);
        memberUser.setUsername("stable-member");
        memberUser.setRealName("Stable Member");
        memberUser.setCellPhone("13800138041");
        memberUser.setEmail("stable-member@example.com");
        memberUser.setStudentId("2026002041");
        memberUser.setMajor("Computer Science");
        memberUser.setGrade("2026");
        memberUser.setType(UserType.STUDENT);
        memberUser.setStatus(UserStatus.NORMAL);
        memberUser.setJoinTime(java.time.LocalDateTime.of(2026, 4, 1, 10, 0));

        ClubMember existingMember = new ClubMember();
        existingMember.setId(61L);
        existingMember.setTenantId(1L);
        existingMember.setName("Stable Member");
        existingMember.setStudentId("2026002041");
        existingMember.setEmail("stable-member@example.com");
        existingMember.setPhone("13800138041");
        existingMember.setMajor("Computer Science");
        existingMember.setGrade("2026");
        existingMember.setPosition("成员");
        existingMember.setStatus("正常");
        existingMember.setJoinDate(memberUser.getJoinTime());
        existingMember.setIsDeleted(0);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(memberUser));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(2L);
        when(authClient.queryAdminUserById(1041L)).thenReturn(authUser);
        when(authClient.queryRoleById(2L)).thenReturn(new RoleDTO().setId(2L).setName("member"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null);

        assertEquals(1, result.getRecords().size());
        verify(clubMemberMapper, never()).updateById(any(ClubMember.class));
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
    }

    @Test
    void queryUserByPage_shouldSkipMemberUpdateWhenJoinDateDiffersOnlyByTimeOfDay() {
        TenantContext.setTenantId(1L);

        User memberUser = new User();
        memberUser.setId(42L);
        memberUser.setAuthUserId(1042L);
        memberUser.setTenantId(1L);
        memberUser.setUsername("same-day-member");
        memberUser.setRealName("Same Day Member");
        memberUser.setCellPhone("13800138042");
        memberUser.setEmail("same-day-member@example.com");
        memberUser.setStudentId("2026002042");
        memberUser.setMajor("Computer Science");
        memberUser.setGrade("2026");
        memberUser.setType(UserType.STUDENT);
        memberUser.setStatus(UserStatus.NORMAL);
        memberUser.setJoinTime(java.time.LocalDateTime.of(2026, 4, 1, 10, 30, 45));

        ClubMember existingMember = new ClubMember();
        existingMember.setId(62L);
        existingMember.setTenantId(1L);
        existingMember.setName("Same Day Member");
        existingMember.setStudentId("2026002042");
        existingMember.setEmail("same-day-member@example.com");
        existingMember.setPhone("13800138042");
        existingMember.setMajor("Computer Science");
        existingMember.setGrade("2026");
        existingMember.setPosition("成员");
        existingMember.setStatus("正常");
        existingMember.setJoinDate(java.time.LocalDateTime.of(2026, 4, 1, 0, 0, 0));
        existingMember.setIsDeleted(0);

        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenAnswer(invocation -> {
            Page<User> page = invocation.getArgument(0);
            page.setRecords(List.of(memberUser));
            page.setTotal(1);
            return page;
        });

        AdminUserProvisionDTO authUser = new AdminUserProvisionDTO();
        authUser.setRoleId(2L);
        when(authClient.queryAdminUserById(1042L)).thenReturn(authUser);
        when(authClient.queryRoleById(2L)).thenReturn(new RoleDTO().setId(2L).setName("member"));

        Page<UserDTO> result = userService.queryUserByPage(1, 10, null, null, null);

        assertEquals(1, result.getRecords().size());
        verify(clubMemberMapper, never()).updateById(any(ClubMember.class));
        verify(clubMemberMapper, never()).insert(any(ClubMember.class));
    }
}
