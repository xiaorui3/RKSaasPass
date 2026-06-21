package com.tianji.user.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tianji.common.enums.UserType;
import com.tianji.common.utils.TenantContext;
import com.tianji.user.domain.po.ClubAlumni;
import com.tianji.user.domain.po.ClubMember;
import com.tianji.user.domain.po.RKTenant;
import com.tianji.user.domain.po.User;
import com.tianji.user.domain.po.UserDetail;
import com.tianji.user.domain.vo.ScopedUserStatisticsVO;
import com.tianji.user.enums.UserStatus;
import com.tianji.user.mapper.ClubAlumniMapper;
import com.tianji.user.mapper.ClubMemberMapper;
import com.tianji.user.mapper.RKTenantMapper;
import com.tianji.user.mapper.UserMapper;
import com.tianji.user.service.IUserDetailService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplScopedStatisticsTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private ClubMemberMapper clubMemberMapper;
    @Mock
    private ClubAlumniMapper clubAlumniMapper;
    @Mock
    private RKTenantMapper tenantMapper;
    @Mock
    private IUserDetailService detailService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ClubMember.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), RKTenant.class);
        service = new UserServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        ReflectionTestUtils.setField(service, "clubMemberMapper", clubMemberMapper);
        ReflectionTestUtils.setField(service, "clubAlumniMapper", clubAlumniMapper);
        ReflectionTestUtils.setField(service, "tenantMapper", tenantMapper);
        ReflectionTestUtils.setField(service, "detailService", detailService);
        lenient().when(clubMemberMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        lenient().when(clubAlumniMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        lenient().when(detailService.getOne(any(Wrapper.class))).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void queryScopedUserStatistics_shouldExposeStandaloneStudentAccountSamples() {
        TenantContext.setTenantId(1L);

        User admin = buildUser(1L, "admin_a", null, UserType.STAFF);
        User sampleOne = buildUser(2L, "member_a", null, UserType.STUDENT);
        User sampleTwo = buildUser(3L, "t01_user", null, UserType.STUDENT);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(admin, sampleOne, sampleTwo));

        ScopedUserStatisticsVO statistics = service.queryScopedUserStatistics(1L);

        assertEquals(3, statistics.getAccountCount());
        assertEquals(2, statistics.getStudentAccountCount());
        assertEquals(2, statistics.getStandaloneStudentAccountCount());
        assertEquals(2, statistics.getStudentAccountWithoutStudentIdCount());
        assertEquals(0, statistics.getStudentAccountMissingMemberLedgerCount());
        assertIterableEquals(List.of("member_a", "t01_user"), statistics.getStandaloneStudentAccountSamples());
    }

    @Test
    void queryScopedUserStatistics_shouldTreatStudentAccountsWithoutMemberLedgerAsStandalone() {
        TenantContext.setTenantId(1L);

        User admin = buildUser(1L, "admin_a", null, UserType.STAFF);
        User missingMemberLedger = buildUser(2L, "student_with_id", "20260001", UserType.STUDENT);
        User missingStudentId = buildUser(3L, "student_without_id", null, UserType.STUDENT);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(admin, missingMemberLedger, missingStudentId));

        ScopedUserStatisticsVO statistics = service.queryScopedUserStatistics(1L);

        assertEquals(2, statistics.getStudentAccountCount());
        assertEquals(1, statistics.getStudentAccountWithStudentIdCount());
        assertEquals(1, statistics.getStudentAccountWithoutStudentIdCount());
        assertEquals(1, statistics.getStudentAccountMissingMemberLedgerCount());
        assertEquals(2, statistics.getStandaloneStudentAccountCount());
        assertEquals(0, statistics.getMemberLinkedAccountCount());
        assertIterableEquals(List.of("student_with_id", "student_without_id"), statistics.getStandaloneStudentAccountSamples());
    }

    @Test
    void queryScopedUserStatistics_shouldUseAllActiveTenantsForSuperAdminView() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        User tenantOneAdmin = buildUser(1L, "admin_a", null, UserType.STAFF);
        User tenantOneStudent = buildUser(2L, "tenant1_student", "20260001", UserType.STUDENT);
        User tenantTwoStudent = buildUser(3L, "tenant2_student", "20260002", UserType.STUDENT);
        tenantTwoStudent.setTenantId(2L);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(tenantOneAdmin, tenantOneStudent, tenantTwoStudent));
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));

        ScopedUserStatisticsVO statistics = service.queryScopedUserStatistics(null);

        verify(tenantMapper).selectList(any(Wrapper.class));
        assertEquals(3, statistics.getAccountCount());
        assertEquals(2, statistics.getStudentAccountCount());
    }

    @Test
    void reconcilePeopleDomainForCurrentScope_shouldUsePlatformMemberScopeForSuperAdminEvenWithTenantHeader() {
        TenantContext.setTenantId(1L);
        TenantContext.setSuperAdmin(true);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(tenantMapper.selectList(any(Wrapper.class))).thenReturn(List.of(activeTenant(1L), activeTenant(2L)));

        service.reconcilePeopleDomainForCurrentScope();

        org.mockito.ArgumentCaptor<Wrapper<ClubMember>> memberQueryCaptor = org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(clubMemberMapper).selectList(memberQueryCaptor.capture());
        String memberSql = memberQueryCaptor.getValue().getSqlSegment();

        assertTrue(memberSql.contains("tenant_id IN"));
        assertFalse(memberSql.contains("tenant_id ="));
    }

    @Test
    void reconcilePeopleDomainForCurrentScope_shouldCreateAlumniRecordForGraduatedMember() {
        TenantContext.setTenantId(1L);

        ClubMember graduatedMember = new ClubMember();
        graduatedMember.setId(10L);
        graduatedMember.setTenantId(1L);
        graduatedMember.setStudentId("20220001");
        graduatedMember.setName("graduated-member");
        graduatedMember.setEmail("graduated@example.com");
        graduatedMember.setMajor("Software");
        graduatedMember.setDepartment("宣传部");
        graduatedMember.setPosition("成员");
        graduatedMember.setGrade((LocalDate.now().getYear() - 4) + "级");
        graduatedMember.setIsDeleted(0);

        User existingUser = buildUser(20L, "graduated-member", "20220001", UserType.STUDENT);
        existingUser.setTenantId(1L);
        existingUser.setAuthUserId(100L);

        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(existingUser);
        when(clubMemberMapper.selectList(any(Wrapper.class))).thenReturn(List.of(graduatedMember));

        service.reconcilePeopleDomainForCurrentScope();

        org.mockito.ArgumentCaptor<ClubAlumni> alumniCaptor = org.mockito.ArgumentCaptor.forClass(ClubAlumni.class);
        verify(clubAlumniMapper, times(1)).insert(alumniCaptor.capture());
        verify(clubAlumniMapper, never()).updateById(any(ClubAlumni.class));
        ClubAlumni alumni = alumniCaptor.getValue();
        assertEquals(1L, alumni.getTenantId());
        assertEquals("20220001", alumni.getStudentId());
        assertEquals("graduated-member", alumni.getName());
        assertEquals("graduated@example.com", alumni.getEmail());
        assertEquals(LocalDate.now().getYear() - 4, alumni.getGenerationYear());
        assertEquals(LocalDate.now().getYear() - 4, alumni.getEnrollmentYear());
        assertEquals(LocalDate.now().getYear(), alumni.getExpectedGraduationYear());
        assertEquals("已毕业", alumni.getMemberStatus());
        assertEquals("已毕业", alumni.getGraduationStatus());
        assertTrue(Boolean.TRUE.equals(alumni.getShowTable()));
        assertTrue(Boolean.TRUE.equals(alumni.getIsActive()));
    }

    private User buildUser(Long id, String username, String studentId, UserType type) {
        User user = new User();
        user.setId(id);
        user.setTenantId(1L);
        user.setUsername(username);
        user.setStudentId(studentId);
        user.setType(type);
        user.setStatus(UserStatus.NORMAL);
        return user;
    }

    private RKTenant activeTenant(Long id) {
        RKTenant tenant = new RKTenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setIsDeleted(0);
        return tenant;
    }
}
